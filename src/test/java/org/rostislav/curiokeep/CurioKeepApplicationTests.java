package org.rostislav.curiokeep;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rostislav.curiokeep.modules.ModuleService;
import org.rostislav.curiokeep.modules.entities.ModuleDefinitionEntity;
import org.rostislav.curiokeep.user.AppUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:curiokeep_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.flyway.enabled=false",
    "spring.sql.init.mode=never"
})
class CurioKeepApplicationTests {

    @TestConfiguration
    static class NoopModuleConfig {
        @Bean
        @Primary
        ModuleService moduleService() {
            return new ModuleService(null, null, null) {
                @Override
                public void loadAllModules() {
                    // no-op for tests
                }

                @Override
                public ModuleDefinitionEntity getById(UUID uuid) {
                    throw new UnsupportedOperationException("Module lookup not needed in smoke test");
                }
            };
        }
    }

    @Autowired
    WebApplicationContext context;

    @MockitoBean
    AppUserRepository users;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        when(users.existsByIsAdminTrue()).thenReturn(true);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void contextLoads() {
    }

    @Test
    void servesTheSinglePageAppWithoutSigningIn() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/collections/123")).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void answersNotFoundForAMissingAsset() throws Exception {
        mvc.perform(get("/assets/missing.js")).andExpect(status().isNotFound());
    }

    @Test
    void protectsTheApiFromAnonymousRequests() throws Exception {
        mvc.perform(get("/api/collections")).andExpect(status().isForbidden());
    }

}
