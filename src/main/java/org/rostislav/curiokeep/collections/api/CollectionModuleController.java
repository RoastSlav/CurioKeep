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
import org.rostislav.curiokeep.collections.CollectionModuleService;
import org.rostislav.curiokeep.collections.api.dto.CollectionModuleResponse;
import org.rostislav.curiokeep.collections.api.dto.EnableModuleResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Collection Modules", description = "Enable/disable modules per collection")
@SecurityRequirement(name = "sessionAuth")
@RestController
@RequestMapping("/api/collections/{collectionId}/modules")
public class CollectionModuleController {

    private final CollectionModuleService service;

    public CollectionModuleController(CollectionModuleService service) {
        this.service = service;
    }

    @Operation(summary = "List enabled modules", description = "Lists modules enabled for the collection (VIEWER+).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Enabled modules returned",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CollectionModuleResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Not permitted / not a member",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public List<CollectionModuleResponse> list(@PathVariable UUID collectionId) {
        return service.listEnabled(collectionId);
    }

    @Operation(summary = "Enable module", description = "Enables a module for the collection (ADMIN+). Idempotent.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Module enabled",
                    content = @Content(schema = @Schema(implementation = EnableModuleResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid module key",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Not permitted (role too low)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Module not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/{moduleKey}")
    public EnableModuleResponse enable(@PathVariable UUID collectionId, @PathVariable String moduleKey) {
        return service.enable(collectionId, moduleKey);
    }

    @Operation(summary = "Disable module", description = "Disables a module for the collection (ADMIN+). Idempotent.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Module disabled",
                    content = @Content(schema = @Schema(implementation = EnableModuleResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid module key",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Not permitted (role too low)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Module not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{moduleKey}")
    public EnableModuleResponse disable(@PathVariable UUID collectionId, @PathVariable String moduleKey) {
        return service.disable(collectionId, moduleKey);
    }
}