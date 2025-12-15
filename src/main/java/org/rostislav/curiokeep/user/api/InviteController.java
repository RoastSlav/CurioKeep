package org.rostislav.curiokeep.user.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.rostislav.curiokeep.api.dto.ApiError;
import org.rostislav.curiokeep.user.InviteService;
import org.rostislav.curiokeep.user.api.dto.InviteValidateResponse;
import org.rostislav.curiokeep.user.api.dto.OkResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Invites", description = "Public endpoints for validating and accepting invites")
@RestController
@RequestMapping("/api/invites")
public class InviteController {

    private final InviteService inviteService;

    public InviteController(InviteService inviteService) {
        this.inviteService = inviteService;
    }

    @Operation(
            summary = "Validate invite token",
            description = "Checks whether the invite token is valid and not expired. Returns { valid: true/false }.",
            security = {}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Validation result returned",
                    content = @Content(schema = @Schema(implementation = InviteValidateResponse.class))
            )
    })
    @GetMapping("/{token}/validate")
    public InviteValidateResponse validate(@PathVariable String token) {
        return new InviteValidateResponse(inviteService.isInviteValid(token));
    }

    @Operation(
            summary = "Accept invite",
            description = """
                    Accepts an invite token and creates a new LOCAL user account.
                    This endpoint is public because the invited user is not authenticated yet.
                    """
            ,
            security = {}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Invite accepted, user created",
                    content = @Content(schema = @Schema(implementation = OkResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid/expired invite, user already exists, or invalid input",
                    content = @Content(schema = @Schema(implementation = ApiError.class))
            )
    })
    @PostMapping("/accept")
    public ResponseEntity<OkResponse> accept(@RequestBody AcceptInviteRequest req) {
        inviteService.acceptInvite(req.token(), req.password(), req.displayName());
        return ResponseEntity.ok(new OkResponse(true));
    }

    public record AcceptInviteRequest(String token, String password, String displayName) {
    }
}
