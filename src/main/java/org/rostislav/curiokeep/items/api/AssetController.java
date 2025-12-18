package org.rostislav.curiokeep.items.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.rostislav.curiokeep.items.ItemImageService;

import java.io.IOException;
import java.nio.file.Files;

@Tag(name = "Assets", description = "Serves cached provider assets (images)")
@SecurityRequirement(name = "sessionAuth")
@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final ItemImageService images;

    public AssetController(ItemImageService images) {
        this.images = images;
    }

    @Operation(summary = "Get cached asset")
    @ApiResponse(responseCode = "200", description = "Asset returned")
    @GetMapping("/{fileName}")
    public ResponseEntity<Resource> get(@PathVariable String fileName) throws IOException {
        Resource res = images.load(fileName);
        if (res == null || !res.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        String contentType = Files.probeContentType(res.getFile().toPath());
        MediaType mediaType = contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "max-age=86400, public")
                .contentType(mediaType)
                .body(res);
    }
}
