package org.rostislav.curiokeep.user.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rostislav.curiokeep.security.SetupModeFilter;
import org.rostislav.curiokeep.user.AppUserRepository;
import org.rostislav.curiokeep.user.LoginThrottle;
import org.rostislav.curiokeep.user.entities.AppUserEntity;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    AuthenticationManager authenticationManager;

    @Mock
    SecurityContextRepository securityContextRepository;

    @Mock
    AppUserRepository appUserRepository;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(appUserRepository.existsByIsAdminTrue()).thenReturn(true);
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authenticationManager, appUserRepository, securityContextRepository, new LoginThrottle()))
                .addFilters(new SetupModeFilter(appUserRepository))
                .build();
    }

    @Test
    void loginCreatesSession() throws Exception {
        AppUserEntity user = new AppUserEntity();
        user.setId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        user.setEmail("user@curiokeep.local");
        user.setDisplayName("User");

        Authentication auth = new UsernamePasswordAuthenticationToken("user@curiokeep.local", "pw");
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(auth);
        when(appUserRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(user));
        doNothing().when(securityContextRepository).saveContext(any(), any(), any());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@curiokeep.local\",\"password\":\"pw\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));
    }

    @Test
    void loginRejectsBlankCredentialsWithoutAskingTheAuthenticationManager() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());

        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void wrongPasswordAnswersUnauthorizedWithoutSayingWhichPartWasWrong() throws Exception {
        when(authenticationManager.authenticate(any(Authentication.class))).thenThrow(new BadCredentialsException("bad"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@curiokeep.local\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void anOverlongPasswordIsAnOrdinaryFailedLoginNotAServerError() throws Exception {
        when(authenticationManager.authenticate(any(Authentication.class))).thenThrow(new IllegalArgumentException("too long"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@curiokeep.local\",\"password\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void repeatedFailuresAreThrottledAndTheAuthenticationManagerIsNoLongerAsked() throws Exception {
        when(authenticationManager.authenticate(any(Authentication.class))).thenThrow(new BadCredentialsException("bad"));
        String body = "{\"email\":\"user@curiokeep.local\",\"password\":\"wrong\"}";

        for (int i = 0; i < LoginThrottle.MAX_FAILURES_PER_ACCOUNT_FROM_ADDRESS; i++) {
            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.error").value("TOO_MANY_ATTEMPTS"));
        verify(authenticationManager, org.mockito.Mockito.times(LoginThrottle.MAX_FAILURES_PER_ACCOUNT_FROM_ADDRESS))
                .authenticate(any(Authentication.class));
    }

    @Test
    void loginReplacesAnExistingSessionId() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken("user@curiokeep.local", "pw");
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(auth);
        when(appUserRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        MockHttpSession session = new MockHttpSession();
        String before = session.getId();

        mockMvc.perform(post("/api/auth/login")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@curiokeep.local\",\"password\":\"pw\"}"))
                .andExpect(status().isOk());

        assertThat(session.getId()).isNotEqualTo(before);
    }

    @Test
    void logoutReturnsOk() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken("user@curiokeep.local", "pw");

        mockMvc.perform(post("/api/auth/logout").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));
    }

    @Test
    void meReturnsCurrentUser() throws Exception {
        AppUserEntity user = new AppUserEntity();
        user.setId(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));
        user.setEmail("user@curiokeep.local");
        user.setDisplayName("RoastSlav");
        user.setAdmin(true);
        when(appUserRepository.findByEmailIgnoreCase("user@curiokeep.local")).thenReturn(Optional.of(user));

        Authentication auth = new UsernamePasswordAuthenticationToken("user@curiokeep.local", "pw");
        mockMvc.perform(get("/api/auth/me").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.email").value("user@curiokeep.local"))
                .andExpect(jsonPath("$.displayName").value("RoastSlav"))
                .andExpect(jsonPath("$.isAdmin").value(true));
    }
}
