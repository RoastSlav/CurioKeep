package org.rostislav.curiokeep.user.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rostislav.curiokeep.security.SetupModeFilter;
import org.rostislav.curiokeep.user.AppUserRepository;
import org.rostislav.curiokeep.user.entities.AppUserEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SetupControllerTest {

    @Mock
    AppUserRepository appUserRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(appUserRepository.existsByIsAdminTrue()).thenReturn(false);
        mockMvc = MockMvcBuilders.standaloneSetup(new SetupController(appUserRepository, passwordEncoder))
                .addFilters(new SetupModeFilter(appUserRepository))
                .build();
    }

    @Test
    void statusReturnsTrueWhenNoAdmin() throws Exception {
        when(appUserRepository.existsByIsAdminTrue()).thenReturn(false);

        mockMvc.perform(get("/api/setup/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.setupRequired").value(true));
    }

    @Test
    void statusReturnsFalseWhenAdminExists() throws Exception {
        when(appUserRepository.existsByIsAdminTrue()).thenReturn(true);

        mockMvc.perform(get("/api/setup/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.setupRequired").value(false));
    }

    @Test
    void createAdminSucceedsWhenNotInitialized() throws Exception {
        when(passwordEncoder.encode("secret"))
                .thenReturn("hashed-secret");
        when(appUserRepository.save(any(AppUserEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/setup/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@example.com\",\"displayName\":\"Admin\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(true));

        ArgumentCaptor<AppUserEntity> captor = ArgumentCaptor.forClass(AppUserEntity.class);
        verify(appUserRepository).save(captor.capture());
        AppUserEntity saved = captor.getValue();
        assertThat(saved.isAdmin()).isTrue();
        assertThat(saved.getEmail()).isEqualTo("admin@example.com");
        assertThat(saved.getDisplayName()).isEqualTo("Admin");
        assertThat(saved.getPasswordHash()).isEqualTo("hashed-secret");
    }

    @Test
    void createAdminFailsWhenAlreadyInitialized() throws Exception {
        when(appUserRepository.existsByIsAdminTrue()).thenReturn(true);

        mockMvc.perform(post("/api/setup/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@example.com\",\"displayName\":\"Admin\",\"password\":\"secret\"}"))
                .andExpect(status().isConflict());

        verify(appUserRepository, never()).save(any());
    }
}
