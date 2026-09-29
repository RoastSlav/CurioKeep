package org.rostislav.curiokeep.items.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.rostislav.curiokeep.items.ItemImageService;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@Tag(name = "Assets", description = "Serves cached provider assets (images)")
@SecurityRequirement(name = "sessionAuth")
@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final ItemImageService images;

    public AssetController(ItemImageService images) {
        this.images = images;
    }

    @Operation(summary = "Get saved asset")
    @ApiResponse(responseCode = "200", description = "Asset returned")
    @GetMapping("/{fileName}")
    public ResponseEntity<Resource> get(@PathVariable String fileName) {
        return images.load(fileName)
                .map(image -> ResponseEntity.ok()
                        .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePrivate())
                        // Belt and braces: even if a file were ever misidentified, the browser may not run it.
                        .header("Content-Security-Policy", "default-src 'none'; sandbox")
                        .contentType(image.mediaType())
                        .body(image.resource()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
