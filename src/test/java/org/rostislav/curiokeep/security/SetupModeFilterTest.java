package org.rostislav.curiokeep.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rostislav.curiokeep.user.AppUserRepository;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SetupModeFilterTest {

    @Mock
    AppUserRepository users;

    @Mock
    FilterChain chain;

    SetupModeFilter filter;
    MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new SetupModeFilter(users);
        response = new MockHttpServletResponse();
    }

    @Test
    void requestsPassThroughOnceAnAdminExists() throws Exception {
        when(users.existsByIsAdminTrue()).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/collections");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void apiRequestsAreForbiddenWithSetupRequiredErrorBeforeFirstAdminExists() throws Exception {
        when(users.existsByIsAdminTrue()).thenReturn(false);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/collections");

        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).isEqualTo("{\"error\":\"SETUP_REQUIRED\"}");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/setup/status", "/swagger-ui/index.html", "/v3/api-docs", "/", "/index.html", "/assets/app.js", "/favicon.ico"})
    void setupEndpointsAndStaticAssetsStayReachableBeforeFirstAdminExists(String path) throws Exception {
        when(users.existsByIsAdminTrue()).thenReturn(false);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void otherPagesRedirectToRootBeforeFirstAdminExists() throws Exception {
        when(users.existsByIsAdminTrue()).thenReturn(false);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/collections/123");

        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(request, response);
        assertThat(response.getRedirectedUrl()).isEqualTo("/");
    }

    @Test
    void lookalikeApiSetupPrefixIsNotTreatedAsSetupEndpoint() throws Exception {
        when(users.existsByIsAdminTrue()).thenReturn(false);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/setupfoo");

        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(403);
    }
}
