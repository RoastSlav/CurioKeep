package org.rostislav.curiokeep.items.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.UUID;

@Schema(name = "MigrationPreviewResponse", description = "What accepting a module's migration would do to a collection's items. Nothing is changed by asking.")
public record MigrationPreviewResponse(
        @Schema(description = "The module version the items would be brought up to.", example = "2.0.0")
        String targetVersion,

        @Schema(description = "Items that are on an earlier module version and would be migrated.", example = "120")
        long behind,

        @Schema(description = "Of those, how many would have their attributes changed. The rest only move to the new version.", example = "87")
        long changed,

        @Schema(description = "The earlier versions items are on, oldest first.")
        List<VersionCount> versions,

        @Schema(description = "A few of the items that would change, with what would change on each.")
        List<Sample> samples
) {

    @Schema(name = "MigrationVersionCount")
    public record VersionCount(String version, long items) {
    }

    @Schema(name = "MigrationSample")
    public record Sample(UUID itemId, String title, List<FieldChange> changes) {
    }

    @Schema(name = "MigrationFieldChange", description = "One attribute of a sample item. A missing before means the attribute was added, a missing after means it was removed.")
    public record FieldChange(String field, JsonNode before, JsonNode after) {
    }
}
