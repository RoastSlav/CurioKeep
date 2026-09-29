package org.rostislav.curiokeep.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TimeConfig {

    /** One shared clock so anything that stamps or compares times can be tested with a fixed one. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
