package org.rostislav.curiokeep.items.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.rostislav.curiokeep.items.ExportFormat;
import org.rostislav.curiokeep.items.ItemExportService;
import org.rostislav.curiokeep.items.ItemImportService;
import org.rostislav.curiokeep.items.api.dto.ImportResult;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.util.UUID;

@Tag(name = "Items", description = "Export and import a collection's items")
@SecurityRequirement(name = "sessionAuth")
@RestController
@RequestMapping("/api/collections/{collectionId}")
public class ItemTransferController {

    private final ItemExportService exporter;
    private final ItemImportService importer;

    public ItemTransferController(ItemExportService exporter, ItemImportService importer) {
        this.exporter = exporter;
        this.importer = importer;
    }

    @Operation(summary = "Export items",
            description = "Downloads the collection's items. `format=json` (default) writes every enabled module in a file that can be "
                    + "imported again; `format=csv` writes one module as a table for spreadsheets and needs `moduleId` unless the "
                    + "collection has a single module. Cover image files are not included. Requires viewer access.")
    @GetMapping("/export")
    public ResponseEntity<StreamingResponseBody> export(
            @PathVariable UUID collectionId,
            @RequestParam(defaultValue = "json") String format,
            @RequestParam(required = false) UUID moduleId
    ) {
        ItemExportService.ExportFile file = exporter.open(collectionId, ExportFormat.parse(format), moduleId);
        return ResponseEntity.ok()
                .contentType(file.mediaType())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.fileName()).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(file.body());
    }

    @Operation(summary = "Import items",
            description = "Adds the items of a JSON export to the collection. Invalid items are skipped and listed in the result; nothing "
                    + "is updated or removed and duplicates are not detected. Requires editor access.")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResult importItems(@PathVariable UUID collectionId, @RequestPart("file") MultipartFile file) throws IOException {
        return importer.importJson(collectionId, file.getBytes());
    }
}
