package org.rostislav.curiokeep.modules.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.rostislav.curiokeep.api.dto.ApiError;
import org.rostislav.curiokeep.modules.ModuleQueryService;
import org.rostislav.curiokeep.modules.api.dto.ModuleDetailsResponse;
import org.rostislav.curiokeep.modules.api.dto.ModuleRawXmlResponse;
import org.rostislav.curiokeep.modules.api.dto.ModuleSummaryResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Modules", description = "Module definition discovery and compiled contract retrieval.")
@SecurityRequirement(name = "sessionAuth")
@RestController
@RequestMapping("/api/modules")
public class ModuleController {

    private final ModuleQueryService service;

    public ModuleController(ModuleQueryService service) {
        this.service = service;
    }

    @Operation(summary = "List all modules", description = "Returns all module definitions available on the server.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Modules returned",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ModuleSummaryResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public List<ModuleSummaryResponse> list() {
        return service.listAll();
    }

    @Operation(summary = "Get module by key", description = "Returns the compiled contract JSON and metadata for a module key.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Module returned",
                    content = @Content(schema = @Schema(implementation = ModuleDetailsResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid module key",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Module not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{moduleKey}")
    public ModuleDetailsResponse get(@PathVariable String moduleKey) {
        return service.getByKey(moduleKey);
    }

    // Optional: raw XML endpoint (useful for debugging; remove if you don't want this exposed)
    @Operation(summary = "Get module raw XML", description = "Returns the raw XML that was loaded for the module.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Raw XML returned",
                    content = @Content(schema = @Schema(implementation = ModuleRawXmlResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid module key",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Module not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{moduleKey}/raw")
    public ModuleRawXmlResponse raw(@PathVariable String moduleKey) {
        return service.getRawXml(moduleKey);
    }
}