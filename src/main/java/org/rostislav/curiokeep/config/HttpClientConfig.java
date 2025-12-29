package org.rostislav.curiokeep.config;

import org.apache.hc.core5.http.HttpHeaders;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Optional;

@Configuration
public class HttpClientConfig {

    private static final String BASE_USER_AGENT = "CurioKeep/1.0 (+https://github.com/RoastSlav/CurioKeep)";

    @Bean
    RestClient restClient(RestClient.Builder builder) {
        return builder.build();
    }

    @Bean
    RestClient.Builder restClientBuilder() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectionRequestTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));

        Optional<String> email = resolveCurrentUserEmail();

        return RestClient.builder().defaultHeader(HttpHeaders.USER_AGENT, BASE_USER_AGENT + (email.isPresent() ? " (user=" + email.get() + ")" : "")).requestFactory(factory);
    }

    private Optional<String> resolveCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return Optional.empty();
        }

        String name = auth.getName();
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(name);
    }
}
