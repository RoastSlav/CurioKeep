package org.rostislav.curiokeep.collections.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.rostislav.curiokeep.api.dto.ApiError;
import org.rostislav.curiokeep.collections.CollectionMemberManagementService;
import org.rostislav.curiokeep.collections.api.dto.AcceptCollectionInviteRequest;
import org.rostislav.curiokeep.collections.api.dto.CollectionMemberResponse;
import org.rostislav.curiokeep.collections.api.dto.CreateCollectionInviteRequest;
import org.rostislav.curiokeep.collections.api.dto.CreateCollectionInviteResponse;
import org.rostislav.curiokeep.collections.api.dto.InviteValidateResponse;
import org.rostislav.curiokeep.collections.api.dto.UpdateMemberRoleRequest;
import org.rostislav.curiokeep.collections.invites.CollectionInviteService;
import org.rostislav.curiokeep.user.api.dto.OkResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Collection Members", description = "Manage collection members and invites")
@RestController
@RequestMapping("/api/collections")
public class CollectionMemberController {

    private final CollectionMemberManagementService memberService;
    private final CollectionInviteService inviteService;

    public CollectionMemberController(CollectionMemberManagementService memberService,
                                      CollectionInviteService inviteService) {
        this.memberService = memberService;
        this.inviteService = inviteService;
    }

    @Operation(summary = "List members", description = "Requires ADMIN or OWNER")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Members returned", content = @Content(array = @ArraySchema(schema = @Schema(implementation = CollectionMemberResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @SecurityRequirement(name = "sessionAuth")
    @GetMapping("/{collectionId}/members")
    public List<CollectionMemberResponse> list(@PathVariable UUID collectionId) {
        return memberService.list(collectionId);
    }

    @Operation(summary = "Update member role", description = "Requires ADMIN or OWNER. Ownership transfer is disabled.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role updated", content = @Content(schema = @Schema(implementation = CollectionMemberResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @SecurityRequirement(name = "sessionAuth")
    @PutMapping("/{collectionId}/members/{userId}")
    public CollectionMemberResponse updateRole(@PathVariable UUID collectionId, @PathVariable UUID userId, @RequestBody UpdateMemberRoleRequest req) {
        return memberService.updateRole(collectionId, userId, req);
    }

    @Operation(summary = "Remove member", description = "Requires ADMIN or OWNER. Cannot remove owner.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Member removed", content = @Content(schema = @Schema(implementation = OkResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @SecurityRequirement(name = "sessionAuth")
    @DeleteMapping("/{collectionId}/members/{userId}")
    public OkResponse remove(@PathVariable UUID collectionId, @PathVariable UUID userId) {
        memberService.remove(collectionId, userId);
        return new OkResponse(true);
    }

    @Operation(summary = "Create collection invite", description = "Requires ADMIN or OWNER")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invite created", content = @Content(schema = @Schema(implementation = CreateCollectionInviteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @SecurityRequirement(name = "sessionAuth")
    @PostMapping("/{collectionId}/invites")
    public CreateCollectionInviteResponse createInvite(@PathVariable UUID collectionId, @RequestBody CreateCollectionInviteRequest req) {
        return inviteService.createInvite(collectionId, req);
    }

    @Operation(summary = "List pending invites", description = "Requires ADMIN or OWNER")
    @SecurityRequirement(name = "sessionAuth")
    @GetMapping("/{collectionId}/invites")
    public List<CreateCollectionInviteResponse> listInvites(@PathVariable UUID collectionId) {
        return inviteService.listPending(collectionId);
    }

    @Operation(summary = "Revoke invite", description = "Requires ADMIN or OWNER")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invite revoked", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @SecurityRequirement(name = "sessionAuth")
    @DeleteMapping("/{collectionId}/invites/{token}")
    public OkResponse revokeInvite(@PathVariable UUID collectionId, @PathVariable String token) {
        inviteService.revokeInvite(collectionId, token);
        return new OkResponse(true);
    }

    @Operation(summary = "Validate invite token", description = "Public endpoint to check if an invite can be accepted")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Validation result", content = @Content(schema = @Schema(implementation = InviteValidateResponse.class)))
    })
    @GetMapping("/invites/{token}/validate")
    public InviteValidateResponse validate(@PathVariable String token) {
        return inviteService.validate(token);
    }

    @Operation(summary = "Accept invite", description = "Authenticated users accept a collection invite")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invite accepted", content = @Content(schema = @Schema(implementation = CollectionMemberResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid or expired invite", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Invite not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @SecurityRequirement(name = "sessionAuth")
    @PostMapping("/invites/accept")
    public CollectionMemberResponse accept(@RequestBody AcceptCollectionInviteRequest req) {
        return inviteService.accept(req);
    }
}
