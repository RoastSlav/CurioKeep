package org.rostislav.curiokeep.modules.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.rostislav.curiokeep.api.dto.ApiError;
import org.rostislav.curiokeep.modules.ModuleImportService;
import org.rostislav.curiokeep.modules.api.dto.ModuleSummaryResponse;
import org.rostislav.curiokeep.modules.api.dto.ScanModulesResponse;
import org.rostislav.curiokeep.user.api.dto.OkResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@Tag(name = "Admin - Modules", description = "Admin-only APIs for imported modules")
@SecurityRequirement(name = "sessionAuth")
@RestController
@RequestMapping("/api/admin/modules")
@PreAuthorize("hasAuthority('APP_ADMIN')")
public class AdminModuleController {

    private final ModuleImportService moduleImportService;

    public AdminModuleController(ModuleImportService moduleImportService) {
        this.moduleImportService = moduleImportService;
    }

    @Operation(summary = "Import module XML",
            description = "Uploads a module XML, validates it, stores it in the import folder, and registers it in the database.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Module imported",
                    content = @Content(schema = @Schema(implementation = ModuleSummaryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid XML or missing file",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Not an admin",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Module key already exists",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ModuleSummaryResponse importModule(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Module file is required");
        }
        try {
            return moduleImportService.importFromBytes(file.getBytes(), file.getOriginalFilename(), true);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to read uploaded module", e);
        }
    }

    @Operation(summary = "Scan import folder",
            description = "Processes every XML already present in the import folder and imports any missing modules.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Scan completed",
                    content = @Content(schema = @Schema(implementation = ScanModulesResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Not an admin",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/scan")
    public ScanModulesResponse scanImportFolder() {
        return moduleImportService.scanImportDir();
    }

    @Operation(summary = "Delete imported module",
            description = "Removes an imported module if it is not used by any collection or items.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Module deleted",
                    content = @Content(schema = @Schema(implementation = OkResponse.class))),
            @ApiResponse(responseCode = "400", description = "Module is not imported",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Not an admin",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Module not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Module in use",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{moduleKey}")
    public OkResponse deleteModule(@PathVariable String moduleKey) {
        moduleImportService.deleteImportedModule(moduleKey);
        return new OkResponse(true);
    }
}
