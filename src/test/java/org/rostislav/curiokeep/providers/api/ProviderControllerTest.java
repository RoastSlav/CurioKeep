package org.rostislav.curiokeep.providers.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rostislav.curiokeep.modules.ModuleService;
import org.rostislav.curiokeep.modules.contract.ModuleSource;
import org.rostislav.curiokeep.modules.entities.ModuleDefinitionEntity;
import org.rostislav.curiokeep.providers.ProviderConfidence;
import org.rostislav.curiokeep.providers.ProviderLookupService;
import org.rostislav.curiokeep.providers.ProviderRegistry;
import org.rostislav.curiokeep.providers.api.dto.LookupResponse;
import org.rostislav.curiokeep.providers.ProviderResult;
import org.rostislav.curiokeep.security.SetupModeFilter;
import org.rostislav.curiokeep.user.AppUserRepository;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProviderControllerTest {

    @Mock
    ModuleService moduleService;

        @Mock
        ProviderLookupService providerLookupService;

        @Mock
        ProviderRegistry providerRegistry;

    @Mock
    AppUserRepository appUserRepository;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(appUserRepository.existsByIsAdminTrue()).thenReturn(true);
        mockMvc = MockMvcBuilders.standaloneSetup(new ProviderController(moduleService, providerRegistry, providerLookupService))
                .addFilters(new SetupModeFilter(appUserRepository))
                .build();
    }

    @Test
    void lookupReturnsMergedPayload() throws Exception {
        UUID moduleId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        ModuleDefinitionEntity module = new ModuleDefinitionEntity();
        module.setId(moduleId);
        module.setModuleKey("books");
        module.setName("Books");
        module.setVersion("1.0.0");
        module.setSource(ModuleSource.BUILTIN);
        module.setChecksum("chk");
        module.setXmlRaw("<module></module>");
        module.setDefinitionJson("{}");
        when(moduleService.getById(moduleId)).thenReturn(module);

        ProviderResult result = new ProviderResult(
                "google",
                Map.of("raw", 1),
                Map.of("title", "Dune"),
                List.of(),
                new ProviderConfidence(80, "good")
        );
        LookupResponse response = new LookupResponse(
                List.of(result),
                result,
                Map.of("title", "Dune"),
                List.of()
        );
        when(providerLookupService.lookup(eq(module), any())).thenReturn(response);

        mockMvc.perform(post("/api/providers/lookup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"moduleId\":\"" + moduleId + "\",\"identifiers\":[{\"idType\":\"ISBN13\",\"idValue\":\"978\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.best.providerKey").value("google"))
                .andExpect(jsonPath("$.mergedAttributes.title").value("Dune"))
                .andExpect(jsonPath("$.results[0].normalizedFields.title").value("Dune"));
    }
}
