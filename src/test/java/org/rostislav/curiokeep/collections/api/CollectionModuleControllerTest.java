package org.rostislav.curiokeep.collections.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rostislav.curiokeep.collections.CollectionModuleService;
import org.rostislav.curiokeep.collections.api.dto.CollectionModuleResponse;
import org.rostislav.curiokeep.collections.api.dto.EnableModuleResponse;
import org.rostislav.curiokeep.modules.contract.ModuleSource;
import org.rostislav.curiokeep.security.SetupModeFilter;
import org.rostislav.curiokeep.user.AppUserRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CollectionModuleControllerTest {

    @Mock
    CollectionModuleService collectionModuleService;

    @Mock
    AppUserRepository appUserRepository;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(appUserRepository.existsByIsAdminTrue()).thenReturn(true);
        mockMvc = MockMvcBuilders.standaloneSetup(new CollectionModuleController(collectionModuleService))
                .addFilters(new SetupModeFilter(appUserRepository))
                .build();
    }

    @Test
    void listEnabledModules() throws Exception {
        UUID moduleId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        CollectionModuleResponse resp = new CollectionModuleResponse("books", "Books", "1.0.0", ModuleSource.BUILTIN, OffsetDateTime.parse("2025-01-05T00:00:00Z"), moduleId);
        when(collectionModuleService.listEnabled(any())).thenReturn(List.of(resp));

        mockMvc.perform(get("/api/collections/" + UUID.randomUUID() + "/modules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].moduleKey").value("books"))
                .andExpect(jsonPath("$[0].moduleId").value(moduleId.toString()));
    }

    @Test
    void enableModuleReturnsEnabled() throws Exception {
        when(collectionModuleService.enable(any(), any())).thenReturn(new EnableModuleResponse(true));

        UUID collectionId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        mockMvc.perform(post("/api/collections/" + collectionId + "/modules/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void disableModuleReturnsDisabled() throws Exception {
        when(collectionModuleService.disable(any(), any())).thenReturn(new EnableModuleResponse(false));

        UUID collectionId = UUID.fromString("bbbbbbbb-cccc-dddd-eeee-ffffffffffff");
        mockMvc.perform(delete("/api/collections/" + collectionId + "/modules/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }
}
