package org.rostislav.curiokeep.user;

import org.jspecify.annotations.NullMarked;
import org.rostislav.curiokeep.user.entities.AppUserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class AppUserDetailsService implements UserDetailsService {
    private static final Logger log = LoggerFactory.getLogger(AppUserDetailsService.class);
    private final AppUserRepository users;

    public AppUserDetailsService(AppUserRepository users) {
        this.users = users;
    }

    @Override
    @NullMarked
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUserEntity u = users.findByEmailIgnoreCase(username)
                .orElseThrow(() -> {
                    log.warn("Authentication failed: user not found for email={}", username);
                    return new UsernameNotFoundException("User not found");
                });

        if (!"ACTIVE".equals(u.getStatus())) {
            log.warn(
                    "Authentication failed: user not active (status={}) email={}",
                    u.getStatus(),
                    username
            );
            throw new UsernameNotFoundException("User not active");
        }

        var auths = new ArrayList<SimpleGrantedAuthority>();
        if (u.isAdmin()) {
            auths.add(new SimpleGrantedAuthority("APP_ADMIN"));
        }

        return User.withUsername(u.getEmail())
                .password(u.getPasswordHash() == null ? "" : u.getPasswordHash())
                .authorities(auths)
                .build();
    }
}
