package org.rostislav.curiokeep.security;

import org.jspecify.annotations.NullMarked;
import org.rostislav.curiokeep.user.AppUserRepository;
import org.rostislav.curiokeep.user.entities.AppUserEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository users;

    public AppUserDetailsService(AppUserRepository users) {
        this.users = users;
    }

    @Override
    @NullMarked
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUserEntity u = users.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!"ACTIVE".equals(u.getStatus())) {
            throw new UsernameNotFoundException("User not active");
        }

        var auths = new ArrayList<SimpleGrantedAuthority>();
        if (u.isAdmin()) auths.add(new SimpleGrantedAuthority("APP_ADMIN"));

        return User.withUsername(u.getEmail())
                .password(u.getPasswordHash() == null ? "" : u.getPasswordHash())
                .authorities(auths)
                .build();
    }
}
