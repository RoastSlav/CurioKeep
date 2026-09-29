package org.rostislav.curiokeep.items.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.rostislav.curiokeep.api.dto.ApiError;
import org.rostislav.curiokeep.items.ItemMigrationService;
import org.rostislav.curiokeep.items.api.dto.MigrationPreviewResponse;
import org.rostislav.curiokeep.items.api.dto.MigrationResultResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Item migration", description = "Bring items up to the current version of their module.")
@SecurityRequirement(name = "sessionAuth")
@RestController
@RequestMapping("/api/collections/{collectionId}/items/migration")
public class ItemMigrationController {

    private final ItemMigrationService service;

    public ItemMigrationController(ItemMigrationService service) {
        this.service = service;
    }

    @Operation(summary = "Preview a migration", description = "Shows what the module's migration would do to the collection's items of that module. Changes nothing. Needs the ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Preview returned"),
            @ApiResponse(responseCode = "400", description = "Unknown module",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Not an admin of the collection",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public MigrationPreviewResponse preview(@PathVariable UUID collectionId, @RequestParam UUID moduleId) {
        return service.preview(collectionId, moduleId);
    }

    @Operation(summary = "Accept a migration", description = "Runs the module's migration on the collection's items of that module. It cannot be undone. Needs the ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Items migrated"),
            @ApiResponse(responseCode = "400", description = "Unknown module",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Not an admin of the collection",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public MigrationResultResponse apply(@PathVariable UUID collectionId, @RequestParam UUID moduleId) {
        return service.apply(collectionId, moduleId);
    }
}
