package org.rostislav.curiokeep.user;

import org.rostislav.curiokeep.user.entities.AppUserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private static final Logger log = LoggerFactory.getLogger(CurrentUserService.class);
    private final AppUserRepository users;

    public CurrentUserService(AppUserRepository users) {
        this.users = users;
    }

    public AppUserEntity requireCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null) {
            log.warn("Current user lookup failed: no Authentication in SecurityContext");
            throw new IllegalStateException("Not authenticated");
        }

        String username = auth.getName();
        if (username == null || username.isBlank()) {
            log.warn("Current user lookup failed: authentication name is null/blank");
            throw new IllegalStateException("Not authenticated");
        }

        return users.findByEmailIgnoreCase(username)
                .orElseThrow(() -> {
                    log.warn("Current user lookup failed: no user found for email={}", username);
                    return new IllegalStateException("User not found");
                });
    }
}
