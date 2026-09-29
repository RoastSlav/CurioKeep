package org.rostislav.curiokeep.items.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rostislav.curiokeep.api.GlobalExceptionHandler;
import org.rostislav.curiokeep.items.ExportFormat;
import org.rostislav.curiokeep.items.ItemExportService;
import org.rostislav.curiokeep.items.ItemImportService;
import org.rostislav.curiokeep.items.api.dto.ImportResult;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ItemTransferControllerTest {

    private static final UUID COLLECTION = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MODULE = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    ItemExportService exporter;
    @Mock
    ItemImportService importer;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ItemTransferController(exporter, importer))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private static ItemExportService.ExportFile file(ExportFormat format, String body) {
        StreamingResponseBody stream = out -> out.write(body.getBytes(StandardCharsets.UTF_8));
        return new ItemExportService.ExportFile("curiokeep-books-2026-03-05." + format.extension(), format.mediaType(), stream);
    }

    @Test
    void exportStreamsTheFileAsADownload() throws Exception {
        when(exporter.open(COLLECTION, ExportFormat.JSON, null)).thenReturn(file(ExportFormat.JSON, "{\"items\":[]}"));

        MvcResult started = mvc.perform(get("/api/collections/" + COLLECTION + "/export"))
                .andExpect(request().asyncStarted()).andReturn();

        mvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"curiokeep-books-2026-03-05.json\""))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(content().string("{\"items\":[]}"));
    }

    @Test
    void exportCanBeAskedForCsvOfOneModule() throws Exception {
        when(exporter.open(COLLECTION, ExportFormat.CSV, MODULE)).thenReturn(file(ExportFormat.CSV, "state,title"));

        MvcResult started = mvc.perform(get("/api/collections/" + COLLECTION + "/export").param("format", "csv").param("moduleId", MODULE.toString()))
                .andExpect(request().asyncStarted()).andReturn();

        mvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string("state,title"));
    }

    @Test
    void anUnknownFormatIsABadRequestAndExportsNothing() throws Exception {
        mvc.perform(get("/api/collections/" + COLLECTION + "/export").param("format", "xml"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_EXPORT_FORMAT"));
    }

    @Test
    void importPassesTheUploadedFileToTheService() throws Exception {
        when(importer.importJson(eq(COLLECTION), org.mockito.ArgumentMatchers.any(byte[].class)))
                .thenReturn(new ImportResult(2, 1, List.of(new ImportResult.ItemError(3, "INVALID_FIELD_pages"))));

        mvc.perform(multipart("/api/collections/" + COLLECTION + "/import")
                        .file(new MockMultipartFile("file", "export.json", "application/json", "{}".getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(2))
                .andExpect(jsonPath("$.failed").value(1))
                .andExpect(jsonPath("$.errors[0].index").value(3))
                .andExpect(jsonPath("$.errors[0].reason").value("INVALID_FIELD_pages"));
        verify(importer).importJson(eq(COLLECTION), org.mockito.ArgumentMatchers.argThat(bytes -> new String(bytes, StandardCharsets.UTF_8).equals("{}")));
    }

    @Test
    void importWithoutAFileIsABadRequest() throws Exception {
        mvc.perform(multipart("/api/collections/" + COLLECTION + "/import"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exportPassesNoModuleWhenNoneIsGiven() throws Exception {
        when(exporter.open(eq(COLLECTION), eq(ExportFormat.JSON), isNull())).thenReturn(file(ExportFormat.JSON, "{}"));

        mvc.perform(get("/api/collections/" + COLLECTION + "/export")).andExpect(request().asyncStarted());

        verify(exporter).open(COLLECTION, ExportFormat.JSON, null);
    }
}
