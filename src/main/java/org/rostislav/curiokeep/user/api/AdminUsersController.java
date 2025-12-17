package org.rostislav.curiokeep.user.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.rostislav.curiokeep.api.dto.ApiError;
import org.rostislav.curiokeep.user.AppUserRepository;
import org.rostislav.curiokeep.user.CurrentUserService;
import org.rostislav.curiokeep.user.UserInviteRepository;
import org.rostislav.curiokeep.user.api.dto.AdminUserResponse;
import org.rostislav.curiokeep.user.api.dto.OkResponse;
import org.rostislav.curiokeep.user.api.dto.UpdateUserAdminRequest;
import org.rostislav.curiokeep.user.api.dto.UpdateUserStatusRequest;
import org.rostislav.curiokeep.user.entities.AppUserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Tag(name = "Admin - Users", description = "Admin-only endpoints for managing users")
@SecurityRequirement(name = "sessionAuth")
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasAuthority('APP_ADMIN')")
public class AdminUsersController {

    private static final Logger log = LoggerFactory.getLogger(AdminUsersController.class);
    private static final Set<String> ALLOWED_STATUSES = Set.of("ACTIVE", "DISABLED");

    private final AppUserRepository users;
    private final CurrentUserService currentUser;
    private final UserInviteRepository invites;

    public AdminUsersController(AppUserRepository users, CurrentUserService currentUser, UserInviteRepository invites) {
        this.users = users;
        this.currentUser = currentUser;
        this.invites = invites;
    }

    @Operation(summary = "List users")
    @GetMapping
    public List<AdminUserResponse> listUsers() {
        return users.findAll().stream().map(this::toDto).toList();
    }

    @Operation(summary = "Update user status", description = "Sets status to ACTIVE or DISABLED")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated", content = @Content(schema = @Schema(implementation = OkResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid status or last admin", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/{id}/status")
    public ResponseEntity<OkResponse> updateStatus(@PathVariable UUID id, @RequestBody UpdateUserStatusRequest req) {
        if (req == null || req.status() == null || req.status().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status is required");
        }
        String status = req.status().trim().toUpperCase();
        if (!ALLOWED_STATUSES.contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status must be ACTIVE or DISABLED");
        }

        AppUserEntity target = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (target.isAdmin() && "DISABLED".equals(status)) {
            ensureAnotherAdminExists(target.getId(), "Cannot disable the last admin user");
        }

        target.setStatus(status);
        users.save(target);
        log.info("Admin updated user status: userId={} status={}", target.getId(), status);
        return ResponseEntity.ok(new OkResponse(true));
    }

    @Operation(summary = "Grant or revoke admin")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Admin flag updated", content = @Content(schema = @Schema(implementation = OkResponse.class))),
            @ApiResponse(responseCode = "400", description = "Cannot remove last admin", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/{id}/admin")
    public ResponseEntity<OkResponse> updateAdmin(@PathVariable UUID id, @RequestBody UpdateUserAdminRequest req) {
        if (req == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body is required");

        AppUserEntity target = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (target.isAdmin() && !req.admin()) {
            ensureAnotherAdminExists(target.getId(), "Cannot remove admin from the last admin user");
        }

        target.setAdmin(req.admin());
        users.save(target);
        log.info("Admin updated user admin flag: userId={} admin={}", target.getId(), req.admin());
        return ResponseEntity.ok(new OkResponse(true));
    }

    @Operation(summary = "Delete user", description = "Removes a user account (cannot delete yourself or the last admin)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User deleted", content = @Content(schema = @Schema(implementation = OkResponse.class))),
            @ApiResponse(responseCode = "400", description = "Protected user or integrity violation", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<OkResponse> deleteUser(@PathVariable UUID id) {
        AppUserEntity target = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        AppUserEntity acting = currentUser.requireCurrentUser();

        if (acting.getId().equals(target.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot delete your own account");
        }

        if (target.isAdmin()) {
            ensureAnotherAdminExists(target.getId(), "Cannot delete the last admin user");
        }

        if (invites.countByInvitedBy(target) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User has invites and cannot be deleted");
        }

        try {
            users.delete(target);
        } catch (DataIntegrityViolationException ex) {
            log.warn("User delete failed due to integrity violation userId={}", target.getId(), ex);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User cannot be deleted because related records exist");
        }

        log.info("Admin deleted userId={}", target.getId());
        return ResponseEntity.ok(new OkResponse(true));
    }

    private void ensureAnotherAdminExists(UUID targetId, String message) {
        long adminCount = users.countByIsAdminTrue();
        if (adminCount <= 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private AdminUserResponse toDto(AppUserEntity u) {
        return new AdminUserResponse(
                u.getId(),
                u.getEmail(),
                u.getDisplayName(),
                u.isAdmin(),
                u.getStatus(),
                u.getAuthProvider(),
                u.getProviderSubject(),
                u.getLastLoginAt(),
                u.getCreatedAt(),
                u.getUpdatedAt()
        );
    }
}
