package org.rostislav.curiokeep.collections.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rostislav.curiokeep.collections.CollectionService;
import org.rostislav.curiokeep.collections.api.dto.CollectionResponse;
import org.rostislav.curiokeep.collections.api.dto.CreateCollectionRequest;
import org.rostislav.curiokeep.collections.api.dto.UpdateCollectionRequest;
import org.rostislav.curiokeep.security.SetupModeFilter;
import org.rostislav.curiokeep.user.AppUserRepository;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CollectionControllerTest {

    @Mock
    CollectionService collectionService;

    @Mock
    AppUserRepository appUserRepository;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(appUserRepository.existsByIsAdminTrue()).thenReturn(true);
        mockMvc = MockMvcBuilders.standaloneSetup(new CollectionController(collectionService))
                .addFilters(new SetupModeFilter(appUserRepository))
                .build();
    }

    @Test
    void listReturnsCollections() throws Exception {
        UUID id = UUID.fromString("44444444-4444-4444-4444-444444444444");
        CollectionResponse resp = new CollectionResponse(id, "Board Games", "desc", "OWNER", OffsetDateTime.parse("2025-01-01T00:00:00Z"));
        when(collectionService.listForCurrentUser()).thenReturn(List.of(resp));

        mockMvc.perform(get("/api/collections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].name").value("Board Games"))
                .andExpect(jsonPath("$[0].role").value("OWNER"));
    }

    @Test
    void createReturnsNewCollection() throws Exception {
        UUID id = UUID.fromString("55555555-5555-5555-5555-555555555555");
        CollectionResponse resp = new CollectionResponse(id, "Movies", "My films", "OWNER", OffsetDateTime.parse("2025-01-02T00:00:00Z"));
        when(collectionService.create(any(CreateCollectionRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/collections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Movies\",\"description\":\"My films\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Movies"));
    }

    @Test
    void getReturnsCollection() throws Exception {
        UUID id = UUID.fromString("66666666-6666-6666-6666-666666666666");
        CollectionResponse resp = new CollectionResponse(id, "Books", null, "VIEWER", OffsetDateTime.parse("2025-01-03T00:00:00Z"));
        when(collectionService.get(id)).thenReturn(resp);

        mockMvc.perform(get("/api/collections/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("VIEWER"))
                .andExpect(jsonPath("$.name").value("Books"));
    }

    @Test
    void updateReturnsUpdatedCollection() throws Exception {
        UUID id = UUID.fromString("77777777-7777-7777-7777-777777777777");
        CollectionResponse resp = new CollectionResponse(id, "Books Updated", "new", "ADMIN", OffsetDateTime.parse("2025-01-04T00:00:00Z"));
        when(collectionService.update(eq(id), any(UpdateCollectionRequest.class))).thenReturn(resp);

        mockMvc.perform(put("/api/collections/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Books Updated\",\"description\":\"new\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Books Updated"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void deleteRemovesCollection() throws Exception {
        UUID id = UUID.fromString("88888888-8888-8888-8888-888888888888");

        mockMvc.perform(delete("/api/collections/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));
    }
}
