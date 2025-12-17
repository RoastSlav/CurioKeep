package org.rostislav.curiokeep.items.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rostislav.curiokeep.items.ItemService;
import org.rostislav.curiokeep.items.api.dto.ChangeStateRequest;
import org.rostislav.curiokeep.items.api.dto.CreateItemRequest;
import org.rostislav.curiokeep.items.api.dto.ItemResponse;
import org.rostislav.curiokeep.items.api.dto.UpdateItemRequest;
import org.rostislav.curiokeep.security.SetupModeFilter;
import org.rostislav.curiokeep.user.AppUserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ItemControllerTest {

    @Mock
    ItemService itemService;

    @Mock
    AppUserRepository appUserRepository;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(appUserRepository.existsByIsAdminTrue()).thenReturn(true);
        mockMvc = MockMvcBuilders.standaloneSetup(new ItemController(itemService))
                .addFilters(new SetupModeFilter(appUserRepository))
                .build();
    }

    private ItemResponse sampleItem(UUID collectionId, UUID moduleId, UUID itemId) {
        return new ItemResponse(
                itemId,
                collectionId,
                moduleId,
                "OWNED",
                "Dune",
                Map.of("title", "Dune"),
                UUID.fromString("01010101-0101-0101-0101-010101010101"),
                OffsetDateTime.parse("2025-01-01T00:00:00Z"),
                OffsetDateTime.parse("2025-01-02T00:00:00Z")
        );
    }

    @Test
    void listReturnsPagedItems() throws Exception {
        UUID collectionId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        UUID moduleId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        UUID itemId = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
        Page<ItemResponse> page = new PageImpl<>(List.of(sampleItem(collectionId, moduleId, itemId)), PageRequest.of(0, 25), 1);
        when(itemService.list(collectionId, moduleId, PageRequest.of(0, 25))).thenReturn(page);

        mockMvc.perform(get("/api/collections/" + collectionId + "/items")
                        .param("moduleId", moduleId.toString())
                        .param("page", "0")
                        .param("size", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(itemId.toString()))
                .andExpect(jsonPath("$.content[0].title").value("Dune"));
    }

    @Test
    void createReturnsItem() throws Exception {
        UUID collectionId = UUID.fromString("f0000000-0000-0000-0000-000000000000");
        UUID moduleId = UUID.fromString("f1111111-1111-1111-1111-111111111111");
        UUID itemId = UUID.fromString("f2222222-2222-2222-2222-222222222222");
        when(itemService.create(any(), any(CreateItemRequest.class))).thenReturn(sampleItem(collectionId, moduleId, itemId));

        mockMvc.perform(post("/api/collections/" + collectionId + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"moduleId\":\"" + moduleId + "\",\"attributes\":{\"title\":\"Dune\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(itemId.toString()))
                .andExpect(jsonPath("$.moduleId").value(moduleId.toString()));
    }

    @Test
    void getReturnsItem() throws Exception {
        UUID collectionId = UUID.fromString("f3333333-3333-3333-3333-333333333333");
        UUID moduleId = UUID.fromString("f4444444-4444-4444-4444-444444444444");
        UUID itemId = UUID.fromString("f5555555-5555-5555-5555-555555555555");
        when(itemService.get(collectionId, itemId)).thenReturn(sampleItem(collectionId, moduleId, itemId));

        mockMvc.perform(get("/api/collections/" + collectionId + "/items/" + itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stateKey").value("OWNED"))
                .andExpect(jsonPath("$.title").value("Dune"));
    }

    @Test
    void updateReturnsItem() throws Exception {
        UUID collectionId = UUID.fromString("f6666666-6666-6666-6666-666666666666");
        UUID moduleId = UUID.fromString("f7777777-7777-7777-7777-777777777777");
        UUID itemId = UUID.fromString("f8888888-8888-8888-8888-888888888888");
        ItemResponse updated = new ItemResponse(itemId, collectionId, moduleId, "OWNED", "Updated", Map.of("title", "Updated"), UUID.randomUUID(), OffsetDateTime.parse("2025-01-01T00:00:00Z"), OffsetDateTime.parse("2025-01-03T00:00:00Z"));
        when(itemService.update(any(), any(), any(UpdateItemRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/collections/" + collectionId + "/items/" + itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated"));
    }

    @Test
    void changeStateReturnsItem() throws Exception {
        UUID collectionId = UUID.fromString("f9999999-9999-9999-9999-999999999999");
        UUID moduleId = UUID.fromString("faaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID itemId = UUID.fromString("fbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        ItemResponse updated = new ItemResponse(itemId, collectionId, moduleId, "WISHLIST", "Dune", Map.of("title", "Dune"), UUID.randomUUID(), OffsetDateTime.parse("2025-01-01T00:00:00Z"), OffsetDateTime.parse("2025-01-04T00:00:00Z"));
        when(itemService.changeState(any(), any(), any(ChangeStateRequest.class))).thenReturn(updated);

        mockMvc.perform(post("/api/collections/" + collectionId + "/items/" + itemId + "/state")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stateKey\":\"WISHLIST\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stateKey").value("WISHLIST"));
    }

    @Test
    void deleteRemovesItem() throws Exception {
        UUID collectionId = UUID.fromString("fcccccccc-cccc-cccc-cccc-ccccccccccc");
        UUID itemId = UUID.fromString("fddddddd-dddd-dddd-dddd-dddddddddddd");

        mockMvc.perform(delete("/api/collections/" + collectionId + "/items/" + itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));
    }
}
