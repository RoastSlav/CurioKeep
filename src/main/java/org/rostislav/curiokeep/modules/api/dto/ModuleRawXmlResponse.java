package org.rostislav.curiokeep.modules.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ModuleRawXmlResponse", description = "Raw module XML source.")
public record ModuleRawXmlResponse(
        @Schema(example = "<module>...</module>") String xmlRaw
) {
}
