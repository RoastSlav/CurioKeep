package org.rostislav.curiokeep.collections.invites;

import org.rostislav.curiokeep.collections.CollectionAccessService;
import org.rostislav.curiokeep.collections.CollectionMemberManagementService;
import org.rostislav.curiokeep.collections.CollectionMemberRepository;
import org.rostislav.curiokeep.collections.CollectionRepository;
import org.rostislav.curiokeep.collections.api.dto.AcceptCollectionInviteRequest;
import org.rostislav.curiokeep.collections.api.dto.CollectionMemberResponse;
import org.rostislav.curiokeep.collections.api.dto.CreateCollectionInviteRequest;
import org.rostislav.curiokeep.collections.api.dto.CreateCollectionInviteResponse;
import org.rostislav.curiokeep.collections.api.dto.InviteValidateResponse;
import org.rostislav.curiokeep.collections.api.dto.Role;
import org.rostislav.curiokeep.collections.entities.CollectionEntity;
import org.rostislav.curiokeep.collections.entities.CollectionMemberEntity;
import org.rostislav.curiokeep.user.CurrentUserService;
import org.rostislav.curiokeep.user.entities.AppUserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CollectionInviteService {

    private static final Logger log = LoggerFactory.getLogger(CollectionInviteService.class);
    private static final int DEFAULT_EXPIRES_DAYS = 7;

    private final CollectionInviteRepository invites;
    private final CollectionMemberRepository members;
    private final CollectionRepository collections;
    private final CurrentUserService currentUser;
    private final CollectionAccessService access;
    private final CollectionMemberManagementService memberService;

    public CollectionInviteService(CollectionInviteRepository invites,
                                   CollectionMemberRepository members,
                                   CollectionRepository collections,
                                   CurrentUserService currentUser,
                                   CollectionAccessService access,
                                   CollectionMemberManagementService memberService) {
        this.invites = invites;
        this.members = members;
        this.collections = collections;
        this.currentUser = currentUser;
        this.access = access;
        this.memberService = memberService;
    }

    @Transactional
    public CreateCollectionInviteResponse createInvite(UUID collectionId, CreateCollectionInviteRequest req) {
        if (req == null || req.role() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "role is required");
        }
        if (req.role() == Role.OWNER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ownership transfer is not supported");
        }

        AppUserEntity acting = currentUser.requireCurrentUser();
        access.requireRole(collectionId, acting.getId(), Role.ADMIN);

        CollectionEntity collection = collections.findById(collectionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        OffsetDateTime expiresAt = null;
        Integer expiresInDays = req.expiresInDays();
        if (expiresInDays == null) {
            expiresInDays = DEFAULT_EXPIRES_DAYS;
        }
        if (expiresInDays != null) {
            if (expiresInDays <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "expiresInDays must be positive");
            }
            expiresAt = OffsetDateTime.now().plusDays(expiresInDays);
        }

        String token = UUID.randomUUID().toString().replace("-", "");

        CollectionInviteEntity entity = new CollectionInviteEntity();
        entity.setToken(token);
        entity.setCollectionId(collection.getId());
        entity.setRole(req.role());
        entity.setCreatedByUserId(acting.getId());
        entity.setExpiresAt(expiresAt);

        invites.save(entity);

        log.info("Collection invite created: collectionId={} byUserId={} role={} expiresAt={}", collectionId, acting.getId(), req.role(), expiresAt);
        return new CreateCollectionInviteResponse(token, req.role(), expiresAt, collectionId);
    }

    @Transactional(readOnly = true)
    public List<CreateCollectionInviteResponse> listPending(UUID collectionId) {
        AppUserEntity acting = currentUser.requireCurrentUser();
        access.requireRole(collectionId, acting.getId(), Role.ADMIN);

        return invites.findAllByCollectionIdAndAcceptedAtIsNullAndRevokedAtIsNull(collectionId).stream()
                .map(inv -> new CreateCollectionInviteResponse(inv.getToken(), inv.getRole(), inv.getExpiresAt(), inv.getCollectionId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public InviteValidateResponse validate(String token) {
        CollectionInviteEntity invite = invites.findByToken(token).orElse(null);
        if (invite == null) {
            return new InviteValidateResponse(false, "Invite not found", null, null);
        }
        var invalidReason = validationFailure(invite);
        if (invalidReason != null) {
            return new InviteValidateResponse(false, invalidReason, invite.getCollectionId(), invite.getRole());
        }
        return new InviteValidateResponse(true, null, invite.getCollectionId(), invite.getRole());
    }

    @Transactional
    public CollectionMemberResponse accept(AcceptCollectionInviteRequest req) {
        if (req == null || req.token() == null || req.token().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "token is required");
        }
        AppUserEntity user = currentUser.requireCurrentUser();

        CollectionInviteEntity invite = invites.findByToken(req.token())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invite not found"));

        String invalidReason = validationFailure(invite);
        if (invalidReason != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, invalidReason);
        }

        CollectionMemberEntity member = members.findByIdCollectionIdAndIdUserId(invite.getCollectionId(), user.getId()).orElse(null);
        if (member == null) {
            member = memberService.createMembership(invite.getCollectionId(), user.getId(), invite.getRole());
        } else if (invite.getRole().ordinal() < member.getRole().ordinal()) {
            member.setRole(invite.getRole());
        }
        members.save(member);

        invite.setAcceptedByUserId(user.getId());
        invite.setAcceptedAt(OffsetDateTime.now());
        invites.save(invite);

        log.info("Invite accepted: collectionId={} userId={} role={}", invite.getCollectionId(), user.getId(), member.getRole());
        return memberService.toResponse(member, user);
    }

    @Transactional
    public void revokeInvite(UUID collectionId, String token) {
        AppUserEntity acting = currentUser.requireCurrentUser();
        access.requireRole(collectionId, acting.getId(), Role.ADMIN);

        CollectionInviteEntity invite = invites.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invite not found"));
        if (!invite.getCollectionId().equals(collectionId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invite not found");
        }
        if (invite.getAcceptedAt() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invite already accepted");
        }
        if (invite.getRevokedAt() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invite already revoked");
        }

        invite.setRevokedAt(OffsetDateTime.now());
        invites.save(invite);

        log.info("Invite revoked: collectionId={} token={} byUserId={}", collectionId, token, acting.getId());
    }

    private String validationFailure(CollectionInviteEntity invite) {
        OffsetDateTime now = OffsetDateTime.now();
        if (invite.getRevokedAt() != null) return "Invite was revoked";
        if (invite.getAcceptedAt() != null) return "Invite already accepted";
        if (invite.getExpiresAt() != null && invite.getExpiresAt().isBefore(now)) return "Invite expired";
        return null;
    }
}
