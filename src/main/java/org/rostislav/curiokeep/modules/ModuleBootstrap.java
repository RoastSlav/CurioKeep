package org.rostislav.curiokeep.modules;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class ModuleBootstrap {

    @Bean
    @Order(1)
    ApplicationRunner loadModulesOnStartup(ModuleService moduleService) {
        return args -> {
            moduleService.loadAllModules();
        };
    }
}
