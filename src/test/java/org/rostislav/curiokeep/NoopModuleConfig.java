package org.rostislav.curiokeep;

import org.rostislav.curiokeep.modules.ModuleService;
import org.rostislav.curiokeep.modules.entities.ModuleDefinitionEntity;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.UUID;

/** Keeps the H2-backed context tests from loading the bundled modules, which need the real PostgreSQL schema. */
@TestConfiguration
class NoopModuleConfig {

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
                throw new UnsupportedOperationException("Module lookup not needed in these tests");
            }
        };
    }
}
