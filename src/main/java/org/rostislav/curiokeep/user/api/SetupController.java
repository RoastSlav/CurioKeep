package org.rostislav.curiokeep.user.api;

import org.rostislav.curiokeep.user.AppUserRepository;
import org.rostislav.curiokeep.user.entities.AppUserEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/setup")
public class SetupController {

    private final AppUserRepository users;
    private final PasswordEncoder encoder;

    public SetupController(AppUserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        boolean setupRequired = !users.existsByIsAdminTrue();
        return Map.of("setupRequired", setupRequired);
    }

    @PostMapping("/admin")
    public ResponseEntity<?> createAdmin(@RequestBody CreateAdminRequest req) {
        if (users.existsByIsAdminTrue()) {
            return ResponseEntity.status(409).body(Map.of("error", "ADMIN_ALREADY_EXISTS"));
        }

        AppUserEntity u = new AppUserEntity();
        u.setEmail(req.email().trim().toLowerCase());
        u.setDisplayName(req.displayName().trim());
        u.setPasswordHash(encoder.encode(req.password()));
        u.setAdmin(true);
        u.setStatus("ACTIVE");
        u.setAuthProvider("LOCAL");

        users.save(u);

        return ResponseEntity.ok(Map.of("created", true));
    }

    public record CreateAdminRequest(String email, String password, String displayName) {}
}
