package org.rostislav.curiokeep.items.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.rostislav.curiokeep.api.dto.ApiError;
import org.rostislav.curiokeep.items.ItemService;
import org.rostislav.curiokeep.items.api.dto.ChangeStateRequest;
import org.rostislav.curiokeep.items.api.dto.CreateItemRequest;
import org.rostislav.curiokeep.items.api.dto.ItemImageUrlRequest;
import org.rostislav.curiokeep.items.api.dto.ItemResponse;
import org.rostislav.curiokeep.items.api.dto.UpdateItemRequest;
import org.rostislav.curiokeep.user.api.dto.OkResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Tag(name = "Items", description = "CRUD for module-driven items inside collections.")
@SecurityRequirement(name = "sessionAuth")
@RestController
@RequestMapping("/api/collections/{collectionId}/items")
public class ItemController {

    private final ItemService service;

    public ItemController(ItemService service) {
        this.service = service;
    }

    @Operation(summary = "List items", description = "Lists items in a collection filtered by module id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Items returned"),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "No access to collection",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public Page<ItemResponse> list(
            @PathVariable UUID collectionId,
            @RequestParam UUID moduleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return service.list(collectionId, moduleId, pageable);
    }

    @Operation(summary = "Create item", description = "Creates a new item for a module in the collection. Attributes are validated against the module contract.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item created",
                    content = @Content(schema = @Schema(implementation = ItemResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request / validation failed",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "No access to collection",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ItemResponse create(@PathVariable UUID collectionId, @RequestBody CreateItemRequest req) {
        return service.create(collectionId, req);
    }

    @Operation(summary = "Get item", description = "Returns a single item by id (must belong to the collection).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item returned",
                    content = @Content(schema = @Schema(implementation = ItemResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "No access to collection",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Item not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{itemId}")
    public ItemResponse get(@PathVariable UUID collectionId, @PathVariable UUID itemId) {
        return service.get(collectionId, itemId);
    }

    @Operation(summary = "Update item", description = "Updates item fields. If attributes are present they are validated against the module contract.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item updated",
                    content = @Content(schema = @Schema(implementation = ItemResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request / validation failed",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "No access to collection",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Item not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{itemId}")
    public ItemResponse update(@PathVariable UUID collectionId, @PathVariable UUID itemId, @RequestBody UpdateItemRequest req) {
        return service.update(collectionId, itemId, req);
    }

    @Operation(summary = "Change item state", description = "Moves an item to a different contract-defined state.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "State changed",
                    content = @Content(schema = @Schema(implementation = ItemResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid state",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "No access to collection",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Item not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/{itemId}/state")
    public ItemResponse changeState(@PathVariable UUID collectionId, @PathVariable UUID itemId, @RequestBody ChangeStateRequest req) {
        return service.changeState(collectionId, itemId, req);
    }

    @Operation(summary = "Delete item", description = "Deletes item and its identifiers.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Deleted",
                    content = @Content(schema = @Schema(implementation = OkResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "No access to collection",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Item not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{itemId}")
    public OkResponse delete(@PathVariable UUID collectionId, @PathVariable UUID itemId) {
        service.delete(collectionId, itemId);
        return new OkResponse(true);
    }

    @Operation(summary = "Set image from URL", description = "Downloads and caches an image for the item, updating providerImageUrl.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Image set",
                    content = @Content(schema = @Schema(implementation = ItemResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid image URL",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "No access to collection",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Item not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
        @PostMapping({"/{itemId}/image/url", "{itemId}/image/from-url", "/{itemId}/image/from-url"})
    public ItemResponse setImageFromUrl(
            @PathVariable UUID collectionId,
            @PathVariable UUID itemId,
            @RequestBody ItemImageUrlRequest req
    ) {
                String url = req == null ? null : req.url();
                return service.setImageFromUrl(collectionId, itemId, url);
    }

    @Operation(summary = "Upload image", description = "Uploads an image file for the item and caches it.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Image uploaded",
                    content = @Content(schema = @Schema(implementation = ItemResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid image",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "No access to collection",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Item not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping(value = "/{itemId}/image/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ItemResponse uploadImage(
            @PathVariable UUID collectionId,
            @PathVariable UUID itemId,
            @RequestPart("file") MultipartFile file
    ) {
        return service.setImageFromUpload(collectionId, itemId, file);
    }

    @Operation(summary = "Delete image", description = "Removes cached image from the item and clears providerImageUrl.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Image removed",
                    content = @Content(schema = @Schema(implementation = ItemResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "No access to collection",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Item not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{itemId}/image")
    public ItemResponse clearImage(@PathVariable UUID collectionId, @PathVariable UUID itemId) {
        return service.clearImage(collectionId, itemId);
    }
}