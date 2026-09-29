package org.rostislav.curiokeep.user.api;

import org.junit.jupiter.api.BeforeEach;
import org.rostislav.curiokeep.user.api.dto.CreateAdminRequest;
import org.springframework.web.server.ResponseStatusException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
        mockMvc = MockMvcBuilders.standaloneSetup(new SetupController(appUserRepository, passwordEncoder, ""))
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
        when(passwordEncoder.encode("a-long-secret-1"))
                .thenReturn("hashed-secret");
        when(appUserRepository.save(any(AppUserEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/setup/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@example.com\",\"displayName\":\"Admin\",\"password\":\"a-long-secret-1\"}"))
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
    void createAdminRejectsAWeakPasswordAndABadEmail() throws Exception {
        mockMvc.perform(post("/api/setup/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@example.com\",\"displayName\":\"Admin\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/setup/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"displayName\":\"Admin\",\"password\":\"a-long-secret-1\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/setup/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@example.com\",\"displayName\":\"Admin\",\"password\":\"" + "x".repeat(73) + "\"}"))
                .andExpect(status().isBadRequest());

        verify(appUserRepository, never()).save(any());
    }

    @Test
    void withASetupTokenConfiguredCreatingTheAdminNeedsIt() throws Exception {
        MockMvc guarded = MockMvcBuilders.standaloneSetup(new SetupController(appUserRepository, passwordEncoder, "the-token"))
                .addFilters(new SetupModeFilter(appUserRepository))
                .build();
        when(passwordEncoder.encode("a-long-secret-1")).thenReturn("hashed");
        when(appUserRepository.save(any(AppUserEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        String base = "\"email\":\"admin@example.com\",\"displayName\":\"Admin\",\"password\":\"a-long-secret-1\"";

        guarded.perform(get("/api/setup/status")).andExpect(jsonPath("$.tokenRequired").value(true));
        guarded.perform(post("/api/setup/admin").contentType(MediaType.APPLICATION_JSON).content("{" + base + "}"))
                .andExpect(status().isForbidden());
        guarded.perform(post("/api/setup/admin").contentType(MediaType.APPLICATION_JSON).content("{" + base + ",\"setupToken\":\"nope\"}"))
                .andExpect(status().isForbidden());
        verify(appUserRepository, never()).save(any());

        guarded.perform(post("/api/setup/admin").contentType(MediaType.APPLICATION_JSON).content("{" + base + ",\"setupToken\":\"the-token\"}"))
                .andExpect(status().isOk());
        verify(appUserRepository).save(any());
    }

    @Test
    void statusSaysNoTokenIsNeededWhenNoneIsConfigured() throws Exception {
        mockMvc.perform(get("/api/setup/status")).andExpect(jsonPath("$.tokenRequired").value(false));
    }

    @Test
    void simultaneousFirstRunRequestsCreateExactlyOneAdmin() throws Exception {
        AtomicBoolean adminExists = new AtomicBoolean(false);
        when(appUserRepository.existsByIsAdminTrue()).thenAnswer(inv -> adminExists.get());
        when(passwordEncoder.encode(any())).thenAnswer(inv -> {
            Thread.sleep(50); // widens the window between the check and the save
            return "hashed";
        });
        when(appUserRepository.save(any(AppUserEntity.class))).thenAnswer(inv -> {
            adminExists.set(true);
            return inv.getArgument(0);
        });
        SetupController controller = new SetupController(appUserRepository, passwordEncoder, "");
        CreateAdminRequest request = new CreateAdminRequest("admin@example.com", "a-long-secret-1", "Admin", null);

        int callers = 8;
        AtomicInteger created = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(callers)) {
            for (int i = 0; i < callers; i++) {
                pool.submit(() -> {
                    start.await();
                    try {
                        controller.createAdmin(request);
                        created.incrementAndGet();
                    } catch (ResponseStatusException conflict) {
                        // expected for every caller except the first
                    }
                    return null;
                });
            }
            start.countDown();
        }

        assertThat(created.get()).isEqualTo(1);
        verify(appUserRepository, org.mockito.Mockito.times(1)).save(any());
    }

    @Test
    void createAdminFailsWhenAlreadyInitialized() throws Exception {
        when(appUserRepository.existsByIsAdminTrue()).thenReturn(true);

        mockMvc.perform(post("/api/setup/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@example.com\",\"displayName\":\"Admin\",\"password\":\"a-long-secret-1\"}"))
                .andExpect(status().isConflict());

        verify(appUserRepository, never()).save(any());
    }
}
