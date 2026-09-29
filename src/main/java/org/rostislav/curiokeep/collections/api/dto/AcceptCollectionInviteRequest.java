package org.rostislav.curiokeep.collections.api.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AcceptCollectionInviteRequest")
public record AcceptCollectionInviteRequest(
        @Schema(description = "Invite token") @NotBlank @Size(max = 200) String token
) {
}
