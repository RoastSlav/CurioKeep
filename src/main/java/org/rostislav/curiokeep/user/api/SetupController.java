package org.rostislav.curiokeep.user.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.rostislav.curiokeep.api.dto.ApiError;
import org.rostislav.curiokeep.user.AppUserRepository;
import org.rostislav.curiokeep.user.api.dto.CreateAdminRequest;
import org.rostislav.curiokeep.user.api.dto.OkResponse;
import org.rostislav.curiokeep.user.api.dto.SetupStatusResponse;
import org.rostislav.curiokeep.user.entities.AppUserEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;


@Tag(name = "Setup", description = "First-run admin setup")
@RestController
@RequestMapping("/api/setup")
public class SetupController {

    private final AppUserRepository users;
    private final PasswordEncoder encoder;

    public SetupController(AppUserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Operation(summary = "Check if setup is required", description = "Returns true if no admin user exists yet.",
            security = {})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Setup status returned",
                    content = @Content(schema = @Schema(implementation = SetupStatusResponse.class)))
    })
    @GetMapping("/status")
    public SetupStatusResponse status() {
        boolean setupRequired = !users.existsByIsAdminTrue();
        return new SetupStatusResponse(setupRequired);
    }

    @Operation(summary = "Create initial admin user",
            description = "Creates the first admin. Only allowed when no admin exists yet.",
            security = {})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Admin created",
                    content = @Content(schema = @Schema(implementation = OkResponse.class))),
            @ApiResponse(responseCode = "409", description = "Admin already exists",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/admin")
    public ResponseEntity<?> createAdmin(@RequestBody CreateAdminRequest req) {
        if (users.existsByIsAdminTrue()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "ADMIN_ALREADY_EXISTS");
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
}
