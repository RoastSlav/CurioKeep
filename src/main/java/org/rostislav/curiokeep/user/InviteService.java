package org.rostislav.curiokeep.user;

import org.rostislav.curiokeep.user.entities.AppUserEntity;
import org.rostislav.curiokeep.user.entities.UserInviteEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class InviteService {
    private static final Logger log = LoggerFactory.getLogger(InviteService.class);
    private final UserInviteRepository invites;
    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final CurrentUserService currentUser;

    public InviteService(UserInviteRepository invites, AppUserRepository users, PasswordEncoder encoder, CurrentUserService currentUser) {
        this.invites = invites;
        this.users = users;
        this.encoder = encoder;
        this.currentUser = currentUser;
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] out = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    @Transactional
    public String createInvite(String emailRaw) {
        String email = emailRaw.trim().toLowerCase();

        if (invites.existsByEmailIgnoreCaseAndStatus(email, "PENDING")) {
            log.warn("Invite creation blocked: pending invite already exists for email={}", email);
            throw new IllegalStateException("Invite already exists for this email");
        }

        String token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");

        String tokenHash = sha256Hex(token);

        AppUserEntity inviter = currentUser.requireCurrentUser();

        UserInviteEntity inv = new UserInviteEntity();
        inv.setEmail(email);
        inv.setTokenHash(tokenHash);
        inv.setInvitedBy(inviter);
        inv.setStatus("PENDING");
        inv.setExpiresAt(OffsetDateTime.now().plusHours(48));

        invites.save(inv);

        log.info(
                "Invite created: inviteId={} email={} invitedByUserId={} expiresAt={}",
                inv.getId(),
                email,
                inviter.getId(),
                inv.getExpiresAt()
        );

        return token;
    }

    @Transactional(readOnly = true)
    public boolean isInviteValid(String token) {
        String hash = sha256Hex(token);

        boolean valid = invites.findByTokenHashAndStatus(hash, "PENDING")
                .filter(i -> i.getExpiresAt().isAfter(OffsetDateTime.now()))
                .isPresent();

        log.debug("Invite validation checked: valid={}", valid);

        return valid;
    }


    @Transactional
    public void acceptInvite(String token, String password, String displayName) {
        String hash = sha256Hex(token);

        UserInviteEntity inv = invites.findByTokenHashAndStatus(hash, "PENDING")
                .orElseThrow(() -> {
                    log.warn("Invite acceptance failed: invalid or already used token");
                    return new IllegalStateException("Invalid invite");
                });

        if (!inv.getExpiresAt().isAfter(OffsetDateTime.now())) {
            inv.setStatus("EXPIRED");
            invites.save(inv);

            log.warn(
                    "Invite expired on accept: inviteId={} email={} expiredAt={}",
                    inv.getId(),
                    inv.getEmail(),
                    inv.getExpiresAt()
            );

            throw new IllegalStateException("Invite expired");
        }

        String email = inv.getEmail();

        if (users.findByEmailIgnoreCase(email).isPresent()) {
            log.warn(
                    "Invite acceptance blocked: user already exists for email={} inviteId={}",
                    email,
                    inv.getId()
            );
            throw new IllegalStateException("User already exists");
        }

        AppUserEntity u = new AppUserEntity();
        u.setEmail(email);
        u.setDisplayName(displayName.trim());
        u.setPasswordHash(encoder.encode(password));
        u.setAdmin(false);
        u.setStatus("ACTIVE");
        u.setAuthProvider("LOCAL");

        users.save(u);

        inv.setStatus("ACCEPTED");
        inv.setAcceptedAt(OffsetDateTime.now());
        invites.save(inv);

        log.info(
                "Invite accepted: inviteId={} email={} newUserId={}",
                inv.getId(),
                email,
                u.getId()
        );
    }
}
