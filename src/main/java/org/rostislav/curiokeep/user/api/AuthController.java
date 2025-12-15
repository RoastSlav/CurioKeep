package org.rostislav.curiokeep.user.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.rostislav.curiokeep.user.AppUserRepository;
import org.rostislav.curiokeep.user.entities.AppUserEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authManager;
    private final AppUserRepository users;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(AuthenticationManager authManager, AppUserRepository users, SecurityContextRepository securityContextRepository) {
        this.securityContextRepository = securityContextRepository;
        this.authManager = authManager;
        this.users = users;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req, HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        req.email().trim().toLowerCase(),
                        req.password()
                )
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        request.getSession(true);

        users.findByEmailIgnoreCase(req.email().trim().toLowerCase()).ifPresent(u -> {
            u.setLastLoginAt(OffsetDateTime.now());
            users.save(u);
        });

        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(Authentication authentication, HttpServletRequest request) {
        new SecurityContextLogoutHandler().logout(request, null, authentication);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        String email = authentication.getName();
        AppUserEntity u = users.findByEmailIgnoreCase(email).orElseThrow();
        return Map.of(
                "id", u.getId(),
                "email", u.getEmail(),
                "displayName", u.getDisplayName(),
                "isAdmin", u.isAdmin()
        );
    }

    public record LoginRequest(String email, String password) {}
}