package org.rostislav.curiokeep.modules.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rostislav.curiokeep.modules.ModuleQueryService;
import org.rostislav.curiokeep.modules.api.dto.ModuleDetailsResponse;
import org.rostislav.curiokeep.modules.api.dto.ModuleRawXmlResponse;
import org.rostislav.curiokeep.modules.api.dto.ModuleSummaryResponse;
import org.rostislav.curiokeep.modules.contract.ModuleContract;
import org.rostislav.curiokeep.modules.contract.ModuleSource;
import org.rostislav.curiokeep.security.SetupModeFilter;
import org.rostislav.curiokeep.user.AppUserRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ModuleControllerTest {

    @Mock
    ModuleQueryService moduleQueryService;

    @Mock
    AppUserRepository appUserRepository;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(appUserRepository.existsByIsAdminTrue()).thenReturn(true);
        mockMvc = MockMvcBuilders.standaloneSetup(new ModuleController(moduleQueryService))
                .addFilters(new SetupModeFilter(appUserRepository))
                .build();
    }

    @Test
    void listReturnsSummaries() throws Exception {
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        OffsetDateTime updatedAt = OffsetDateTime.parse("2025-01-01T12:00:00Z");
        ModuleSummaryResponse summary = new ModuleSummaryResponse(id, "books", "Books", "1.0.0", ModuleSource.BUILTIN, updatedAt);
        when(moduleQueryService.listAll()).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/modules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].moduleKey").value("books"))
                .andExpect(jsonPath("$[0].name").value("Books"))
                .andExpect(jsonPath("$[0].version").value("1.0.0"))
                .andExpect(jsonPath("$[0].source").value("BUILTIN"));
    }

    @Test
    void getReturnsDetails() throws Exception {
        UUID id = UUID.fromString("22222222-2222-2222-2222-222222222222");
        OffsetDateTime created = OffsetDateTime.parse("2025-02-02T00:00:00Z");
        OffsetDateTime updated = OffsetDateTime.parse("2025-02-03T00:00:00Z");
        ModuleContract contract = new ModuleContract("books", "1.0.0", "Books", "desc", null, List.of(), List.of(), List.of(), List.of(), Map.of());
        ModuleDetailsResponse details = new ModuleDetailsResponse(id, "books", "Books", "1.0.0", ModuleSource.BUILTIN, "checksum", contract, created, updated);
        when(moduleQueryService.getByKey("books")).thenReturn(details);

        mockMvc.perform(get("/api/modules/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.moduleKey").value("books"))
                .andExpect(jsonPath("$.checksum").value("checksum"))
                .andExpect(jsonPath("$.contract.key").value("books"));
    }

    @Test
    void rawReturnsXml() throws Exception {
        when(moduleQueryService.getRawXml("books")).thenReturn(new ModuleRawXmlResponse("<module>ok</module>"));

        mockMvc.perform(get("/api/modules/books/raw"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.xmlRaw").value("<module>ok</module>"));
    }
}
