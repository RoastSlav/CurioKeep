package org.rostislav.curiokeep.user.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rostislav.curiokeep.security.SetupModeFilter;
import org.rostislav.curiokeep.user.AppUserRepository;
import org.rostislav.curiokeep.user.InviteService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InviteControllerTest {

    @Mock
    InviteService inviteService;

    @Mock
    AppUserRepository appUserRepository;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(appUserRepository.existsByIsAdminTrue()).thenReturn(true);
        mockMvc = MockMvcBuilders.standaloneSetup(new InviteController(inviteService))
                .addFilters(new SetupModeFilter(appUserRepository))
                .build();
    }

    @Test
    void validateReturnsResult() throws Exception {
        when(inviteService.isInviteValid("token")).thenReturn(true);

        mockMvc.perform(get("/api/invites/token/validate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void acceptProcessesInvite() throws Exception {
        doNothing().when(inviteService).acceptInvite(anyString(), anyString(), anyString());

        mockMvc.perform(post("/api/invites/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"token\",\"password\":\"pw\",\"displayName\":\"User\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));
    }
}
