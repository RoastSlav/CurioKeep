package org.rostislav.curiokeep.collections.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.rostislav.curiokeep.api.dto.ApiError;
import org.rostislav.curiokeep.collections.CollectionService;
import org.rostislav.curiokeep.collections.api.dto.CollectionResponse;
import org.rostislav.curiokeep.collections.api.dto.CreateCollectionRequest;
import org.rostislav.curiokeep.collections.api.dto.UpdateCollectionRequest;
import org.rostislav.curiokeep.user.api.dto.OkResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Collections", description = "Manage collections owned by you or shared with you.")
@SecurityRequirement(name = "sessionAuth")
@RestController
@RequestMapping("/api/collections")
public class CollectionController {

    private final CollectionService service;

    public CollectionController(CollectionService service) {
        this.service = service;
    }

    @Operation(
            summary = "List collections",
            description = "Returns collections where the current user is a member. Each entry includes your role in that collection."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Collections returned",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CollectionResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public List<CollectionResponse> list() {
        return service.listForCurrentUser();
    }

    @Operation(
            summary = "Create collection",
            description = "Creates a new collection and assigns the current user as OWNER."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Collection created",
                    content = @Content(schema = @Schema(implementation = CollectionResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "id": "b91b4c5f-1a4d-4c4f-8b39-3f8a9e6d2e22",
                                      "name": "Board Games",
                                      "description": "My physical board game library",
                                      "role": "OWNER",
                                      "createdAt": "2025-12-15T22:10:00+02:00"
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = """
                                    { "error": "VALIDATION_ERROR", "message": "name is required" }
                                    """))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public CollectionResponse create(@RequestBody CreateCollectionRequest req) {
        return service.create(req);
    }

    @Operation(
            summary = "Get collection",
            description = "Returns a single collection if the current user is a member of it (VIEWER or higher)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Collection returned",
                    content = @Content(schema = @Schema(implementation = CollectionResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Not a member / no access",
                    content = @Content(schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = """
                                    { "error": "FORBIDDEN", "message": "Not permitted" }
                                    """))),
            @ApiResponse(responseCode = "404", description = "Collection not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = """
                                    { "error": "NOT_FOUND", "message": "Collection not found" }
                                    """)))
    })
    @GetMapping("/{id}")
    public CollectionResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @Operation(
            summary = "Update collection",
            description = "Updates collection name/description. Requires ADMIN or OWNER role."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Collection updated",
                    content = @Content(schema = @Schema(implementation = CollectionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Not permitted (role too low)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Collection not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{id}")
    public CollectionResponse update(@PathVariable UUID id, @RequestBody UpdateCollectionRequest req) {
        return service.update(id, req);
    }

    @Operation(
            summary = "Delete collection",
            description = "Deletes the collection. Requires OWNER role."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Collection deleted",
                    content = @Content(schema = @Schema(implementation = OkResponse.class),
                            examples = @ExampleObject(value = """
                                    { "ok": true }
                                    """))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Not permitted (not OWNER)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Collection not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{id}")
    public OkResponse delete(@PathVariable UUID id) {
        service.delete(id);
        return new OkResponse(true);
    }
}
