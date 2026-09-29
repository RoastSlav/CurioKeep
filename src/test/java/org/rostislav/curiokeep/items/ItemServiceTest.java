package org.rostislav.curiokeep.items;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rostislav.curiokeep.collections.CollectionAccessService;
import org.rostislav.curiokeep.collections.api.dto.Role;
import org.rostislav.curiokeep.items.api.dto.ChangeStateRequest;
import org.rostislav.curiokeep.items.api.dto.CreateItemRequest;
import org.rostislav.curiokeep.items.api.dto.ItemResponse;
import org.rostislav.curiokeep.items.entities.ItemEntity;
import org.rostislav.curiokeep.modules.ModuleQueryService;
import org.rostislav.curiokeep.modules.contract.FieldContract;
import org.rostislav.curiokeep.modules.contract.FieldType;
import org.rostislav.curiokeep.modules.contract.ModuleContract;
import org.rostislav.curiokeep.modules.contract.StateContract;
import org.rostislav.curiokeep.modules.entities.ModuleDefinitionEntity;
import org.rostislav.curiokeep.user.CurrentUserService;
import org.rostislav.curiokeep.user.entities.AppUserEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    private static final UUID COLLECTION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_COLLECTION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID MODULE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID USER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID ITEM_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @Mock
    ItemRepository items;
    @Mock
    ItemIdentifierRepository identifiers;
    @Mock
    CurrentUserService currentUser;
    @Mock
    CollectionAccessService access;
    @Mock
    ModuleQueryService modules;
    @Mock
    ItemImageService imageService;

    ItemService service;
    ModuleDefinitionEntity moduleEntity;

    @BeforeEach
    void setUp() {
        service = new ItemService(items, identifiers, currentUser, access, modules, new ObjectMapper(), imageService);

        AppUserEntity user = new AppUserEntity();
        user.setId(USER_ID);
        when(currentUser.requireCurrentUser()).thenReturn(user);

        moduleEntity = new ModuleDefinitionEntity();
        moduleEntity.setId(MODULE_ID);
    }

    @Test
    void createRequiresEditorRoleAndStopsWhenAccessIsDenied() {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
                .when(access).requireRole(COLLECTION_ID, USER_ID, Role.EDITOR);

        assertThatThrownBy(() -> service.create(COLLECTION_ID, request("OWNED", Map.of("title", "Dune"))))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(items, never()).save(any());
    }

    @Test
    void createFailsWhenModuleDoesNotExist() {
        when(modules.getEntityById(MODULE_ID)).thenReturn(Optional.empty());

        assertBadRequest(() -> service.create(COLLECTION_ID, request("OWNED", Map.of("title", "Dune"))), "MODULE_NOT_FOUND");
    }

    @Test
    void createFailsWhenRequiredFieldIsMissing() {
        givenModule();

        assertBadRequest(() -> service.create(COLLECTION_ID, request("OWNED", Map.of("pages", 412))), "MISSING_REQUIRED_FIELD_title");
        verify(items, never()).save(any());
    }

    @Test
    void createFailsWhenRequiredTextFieldIsBlank() {
        givenModule();

        assertBadRequest(() -> service.create(COLLECTION_ID, request("OWNED", Map.of("title", "   "))), "MISSING_REQUIRED_FIELD_title");
    }

    @Test
    void createFailsWhenStateIsNotDeclaredByModule() {
        givenModule();

        assertBadRequest(() -> service.create(COLLECTION_ID, request("STOLEN", Map.of("title", "Dune"))), "INVALID_STATE");
    }

    @Test
    void createNormalizesStateKeyToUpperCase() {
        givenModule();

        ItemResponse response = service.create(COLLECTION_ID, request(" wishlist ", Map.of("title", "Dune")));

        assertThat(response.stateKey()).isEqualTo("WISHLIST");
    }

    @Test
    void createDefaultsToFirstDeclaredStateWhenNoneGiven() {
        givenModule();

        ItemResponse response = service.create(COLLECTION_ID, request(null, Map.of("title", "Dune")));

        assertThat(response.stateKey()).isEqualTo("OWNED");
    }

    @Test
    void createStoresAttributesAndRecordsCreator() {
        givenModule();

        service.create(COLLECTION_ID, request("OWNED", Map.of("title", "Dune", "pages", 412)));

        ArgumentCaptor<ItemEntity> saved = ArgumentCaptor.forClass(ItemEntity.class);
        verify(items).save(saved.capture());
        assertThat(saved.getValue().getCollectionId()).isEqualTo(COLLECTION_ID);
        assertThat(saved.getValue().getModuleId()).isEqualTo(MODULE_ID);
        assertThat(saved.getValue().getCreatedBy()).isEqualTo(USER_ID);
        assertThat(saved.getValue().getAttributes()).contains("\"title\":\"Dune\"").contains("\"pages\":412");
    }

    @Test
    void changeStateRejectsItemFromAnotherCollection() {
        ItemEntity item = item(OTHER_COLLECTION_ID);
        when(items.findById(ITEM_ID)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.changeState(COLLECTION_ID, ITEM_ID, new ChangeStateRequest("OWNED")))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(ex.getReason()).isEqualTo("ITEM_NOT_FOUND");
                });
        verify(items, never()).save(any());
    }

    @Test
    void changeStateRejectsUndeclaredState() {
        ItemEntity item = item(COLLECTION_ID);
        when(items.findById(ITEM_ID)).thenReturn(Optional.of(item));
        givenModule();

        assertBadRequest(() -> service.changeState(COLLECTION_ID, ITEM_ID, new ChangeStateRequest("STOLEN")), "INVALID_STATE");
        assertThat(item.getStateKey()).isEqualTo("OWNED");
    }

    @Test
    void changeStateUpdatesDeclaredState() {
        ItemEntity item = item(COLLECTION_ID);
        when(items.findById(ITEM_ID)).thenReturn(Optional.of(item));
        givenModule();

        ItemResponse response = service.changeState(COLLECTION_ID, ITEM_ID, new ChangeStateRequest("lost"));

        assertThat(response.stateKey()).isEqualTo("LOST");
        verify(items).save(item);
    }

    @Test
    void deleteRequiresAdminRole() {
        when(items.findById(ITEM_ID)).thenReturn(Optional.of(item(COLLECTION_ID)));
        when(identifiers.findAllByItemId(ITEM_ID)).thenReturn(List.of());

        service.delete(COLLECTION_ID, ITEM_ID);

        verify(access).requireRole(COLLECTION_ID, USER_ID, Role.ADMIN);
    }

    @Test
    void deleteDoesNotTouchItemFromAnotherCollection() {
        when(items.findById(ITEM_ID)).thenReturn(Optional.of(item(OTHER_COLLECTION_ID)));

        assertThatThrownBy(() -> service.delete(COLLECTION_ID, ITEM_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(items, never()).delete(any());
    }

    private void givenModule() {
        when(modules.getEntityById(MODULE_ID)).thenReturn(Optional.of(moduleEntity));
        when(modules.getContract(moduleEntity)).thenReturn(booksLikeContract());
    }

    private ModuleContract booksLikeContract() {
        return new ModuleContract(
                "books", "1.0.0", "Books", null, null,
                List.of(state("OWNED", 1), state("WISHLIST", 2), state("LOST", 3)),
                List.of(),
                List.of(field("title", FieldType.TEXT, true), field("pages", FieldType.NUMBER, false)),
                List.of(),
                Map.of()
        );
    }

    private StateContract state(String key, int order) {
        return new StateContract(key, key, order, true, false, Map.of());
    }

    private FieldContract field(String key, FieldType type, boolean required) {
        return new FieldContract(key, key, type, required, false, false, false, 0, true, false,
                null, List.of(), List.of(), null, null, List.of(), Map.of());
    }

    private CreateItemRequest request(String stateKey, Map<String, Object> attributes) {
        return new CreateItemRequest(MODULE_ID, stateKey, null, attributes, null);
    }

    private ItemEntity item(UUID collectionId) {
        ItemEntity item = new ItemEntity();
        item.setId(ITEM_ID);
        item.setCollectionId(collectionId);
        item.setModuleId(MODULE_ID);
        item.setStateKey("OWNED");
        item.setAttributes("{}");
        return item;
    }

    private void assertBadRequest(Runnable action, String reason) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getReason()).isEqualTo(reason);
                });
    }
}
