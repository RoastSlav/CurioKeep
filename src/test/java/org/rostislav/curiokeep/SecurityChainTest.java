package org.rostislav.curiokeep;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rostislav.curiokeep.user.AppUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The real security filter chain, exception handling and SPA routes, on the H2-backed context. */
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
@Import(NoopModuleConfig.class)
class SecurityChainTest {

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
        mvc.perform(get("/")).andExpect(result -> assertThat(result.getResponse().getStatus()).isNotIn(302, 403));
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

    @Test
    void stateChangingRequestsNeedTheCsrfTokenTheServerHandsOutInACookie() throws Exception {
        String body = "{}";

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());

        Cookie token = mvc.perform(get("/api/setup/status")).andReturn().getResponse().getCookie("XSRF-TOKEN");
        assertThat(token).isNotNull();
        assertThat(token.isHttpOnly()).as("the SPA has to read it").isFalse();

        // With the token the request gets past CSRF and fails on its own (empty) content instead.
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body)
                        .cookie(token).header("X-XSRF-TOKEN", token.getValue()))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body)
                        .cookie(token).header("X-XSRF-TOKEN", "a-different-value"))
                .andExpect(status().isForbidden());
    }

    @Test
    void aSignedInUserWithoutTheAdminAuthorityGetsForbiddenNotAServerError() throws Exception {
        mvc.perform(get("/api/admin/users").with(user("plain@example.test").authorities(new SimpleGrantedAuthority("SOMETHING_ELSE"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

}
