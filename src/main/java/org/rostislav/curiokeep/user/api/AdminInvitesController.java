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
import org.rostislav.curiokeep.user.api.dto.CreateInviteResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Admin - Invites", description = "Admin-only endpoints for inviting new users")
@SecurityRequirement(name = "sessionAuth")
@RestController
@RequestMapping("/api/admin/invites")
public class AdminInvitesController {

    private final InviteService inviteService;

    public AdminInvitesController(InviteService inviteService) {
        this.inviteService = inviteService;
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

    public record CreateInviteRequest(String email) {}
}
