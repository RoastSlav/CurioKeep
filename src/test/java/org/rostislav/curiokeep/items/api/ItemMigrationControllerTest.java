package org.rostislav.curiokeep.items.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rostislav.curiokeep.items.ItemMigrationService;
import org.rostislav.curiokeep.items.api.dto.MigrationPreviewResponse;
import org.rostislav.curiokeep.items.api.dto.MigrationResultResponse;
import org.rostislav.curiokeep.security.SetupModeFilter;
import org.rostislav.curiokeep.user.AppUserRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.node.StringNode;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ItemMigrationControllerTest {

    private static final UUID COLLECTION_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID MODULE_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    @Mock
    ItemMigrationService service;

    @Mock
    AppUserRepository appUserRepository;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(appUserRepository.existsByIsAdminTrue()).thenReturn(true);
        mockMvc = MockMvcBuilders.standaloneSetup(new ItemMigrationController(service))
                .addFilters(new SetupModeFilter(appUserRepository))
                .build();
    }

    @Test
    void previewReturnsWhatWouldChange() throws Exception {
        when(service.preview(COLLECTION_ID, MODULE_ID)).thenReturn(new MigrationPreviewResponse("2.0.0", 12, 10,
                List.of(new MigrationPreviewResponse.VersionCount("1.0.0", 12)),
                List.of(new MigrationPreviewResponse.Sample(UUID.randomUUID(), "Dune", List.of(
                        new MigrationPreviewResponse.FieldChange("notes", null, StringNode.valueOf("first")))))));

        mockMvc.perform(get("/api/collections/" + COLLECTION_ID + "/items/migration").param("moduleId", MODULE_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetVersion").value("2.0.0"))
                .andExpect(jsonPath("$.behind").value(12))
                .andExpect(jsonPath("$.changed").value(10))
                .andExpect(jsonPath("$.samples[0].changes[0].field").value("notes"))
                .andExpect(jsonPath("$.samples[0].changes[0].after").value("first"));
    }

    @Test
    void acceptingRunsTheMigrationAndReportsTheOutcome() throws Exception {
        when(service.apply(COLLECTION_ID, MODULE_ID)).thenReturn(new MigrationResultResponse(12, 10, 1));

        mockMvc.perform(post("/api/collections/" + COLLECTION_ID + "/items/migration").param("moduleId", MODULE_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.migrated").value(12))
                .andExpect(jsonPath("$.changed").value(10))
                .andExpect(jsonPath("$.skipped").value(1));

        verify(service).apply(COLLECTION_ID, MODULE_ID);
    }
}
