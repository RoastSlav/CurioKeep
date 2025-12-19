package org.rostislav.curiokeep;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.rostislav.curiokeep.modules.ModuleService;
import org.rostislav.curiokeep.modules.entities.ModuleDefinitionEntity;

import java.util.UUID;

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

    @Test
    void contextLoads() {
    }

}
