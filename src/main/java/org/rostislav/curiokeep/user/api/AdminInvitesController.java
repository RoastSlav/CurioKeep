package org.rostislav.curiokeep.user.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.rostislav.curiokeep.api.dto.ApiError;
import org.rostislav.curiokeep.user.InviteService;
import org.rostislav.curiokeep.user.UserInviteRepository;
import org.rostislav.curiokeep.user.entities.UserInviteEntity;
import org.rostislav.curiokeep.user.api.dto.AdminInviteResponse;
import org.rostislav.curiokeep.user.api.dto.CreateInviteRequest;
import org.rostislav.curiokeep.user.api.dto.CreateInviteResponse;
import org.rostislav.curiokeep.user.api.dto.OkResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Admin - Invites", description = "Admin-only endpoints for inviting new users")
@SecurityRequirement(name = "sessionAuth")
@RestController
@RequestMapping("/api/admin/invites")
public class AdminInvitesController {

        private final InviteService inviteService;
        private final UserInviteRepository invites;

        public AdminInvitesController(InviteService inviteService, UserInviteRepository invites) {
                this.inviteService = inviteService;
                this.invites = invites;
        }

    @Operation(
            summary = "Create invite",
            description = """
                    Creates an invite for a new user.
                    Returns a raw token that should be shared with the invited user.
                    The token is only returned once and is not stored in plain text (only a hash is stored).
                    Requires global admin privileges.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Invite created. Token returned once.",
                    content = @Content(schema = @Schema(implementation = CreateInviteResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ApiError.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Not an admin / forbidden",
                    content = @Content(schema = @Schema(implementation = ApiError.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request or invite already exists",
                    content = @Content(schema = @Schema(implementation = ApiError.class))
            )
    })
    @PreAuthorize("hasAuthority('APP_ADMIN')")
    @PostMapping
    public ResponseEntity<CreateInviteResponse> createInvite(@RequestBody CreateInviteRequest req) {
        String token = inviteService.createInvite(req.email());
        return ResponseEntity.ok(new CreateInviteResponse(token));
    }

    @Operation(summary = "List pending invites", description = "List pending admin invites")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List returned"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PreAuthorize("hasAuthority('APP_ADMIN')")
    @GetMapping
    @Transactional(readOnly = true)
    public java.util.List<AdminInviteResponse> listInvites() {
        return invites.findAll().stream()
                .filter(i -> "PENDING".equals(i.getStatus()))
                .map(i -> new AdminInviteResponse(
                        i.getId().toString(),
                        i.getEmail(),
                        new AdminInviteResponse.InvitedBy(
                                i.getInvitedBy() != null ? i.getInvitedBy().getDisplayName() : null,
                                i.getInvitedBy() != null ? i.getInvitedBy().getEmail() : null
                        ),
                        i.getCreatedAt()
                ))
                .toList();
    }

    @Operation(summary = "Revoke invite", description = "Revoke a pending invite by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invite revoked"),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PreAuthorize("hasAuthority('APP_ADMIN')")
    @DeleteMapping("/{token}")
    public ResponseEntity<OkResponse> revokeInvite(@PathVariable java.util.UUID token) {
        UserInviteEntity inv = invites.findById(token).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invite not found"));
        inv.setStatus("REVOKED");
        invites.save(inv);
        return ResponseEntity.ok(new OkResponse(true));
    }
}
