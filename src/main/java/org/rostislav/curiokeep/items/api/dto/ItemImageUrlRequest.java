package org.rostislav.curiokeep.items.api.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ItemImageUrlRequest", description = "Request to set the item's primary image from a URL.")
public record ItemImageUrlRequest(
        @Schema(description = "Image URL to download and cache.", example = "https://example.com/cover.jpg")
        @NotBlank @Size(max = 2048) String url
) {
}
