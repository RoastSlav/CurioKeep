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
import org.rostislav.curiokeep.items.api.dto.ItemCountsResponse;
import org.rostislav.curiokeep.items.api.dto.ItemIdentifierDto;
import org.rostislav.curiokeep.items.api.dto.ItemResponse;
import org.rostislav.curiokeep.items.entities.ItemEntity;
import org.rostislav.curiokeep.items.entities.ItemIdentifierEntity;
import org.rostislav.curiokeep.modules.ModuleQueryService;
import org.rostislav.curiokeep.modules.contract.FieldContract;
import org.rostislav.curiokeep.modules.contract.FieldType;
import org.rostislav.curiokeep.modules.contract.ModuleContract;
import org.rostislav.curiokeep.modules.contract.StateContract;
import org.rostislav.curiokeep.modules.entities.ModuleDefinitionEntity;
import org.rostislav.curiokeep.user.CurrentUserService;
import org.rostislav.curiokeep.user.entities.AppUserEntity;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
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
import static org.mockito.Mockito.mock;
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
    @Mock
    ItemSearchRepository search;

    ItemService service;
    ModuleDefinitionEntity moduleEntity;

    @BeforeEach
    void setUp() {
        service = new ItemService(items, identifiers, currentUser, access, modules, new ObjectMapper(), imageService,
                search, new TransactionTemplate(mock(PlatformTransactionManager.class)));

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

    @Test
    void deleteRemovesTheCoverFileWhenNoOtherItemUsesIt() {
        ItemEntity item = item(COLLECTION_ID);
        item.setImageName("0123456789abcdef0123456789abcdef.png");
        when(items.findById(ITEM_ID)).thenReturn(Optional.of(item));
        when(identifiers.findAllByItemId(ITEM_ID)).thenReturn(List.of());
        when(items.existsByImageName("0123456789abcdef0123456789abcdef.png")).thenReturn(false);

        service.delete(COLLECTION_ID, ITEM_ID);

        verify(imageService).delete("0123456789abcdef0123456789abcdef.png");
    }

    @Test
    void deleteKeepsACoverFileThatAnotherItemStillUses() {
        ItemEntity item = item(COLLECTION_ID);
        item.setImageName("0123456789abcdef0123456789abcdef.png");
        when(items.findById(ITEM_ID)).thenReturn(Optional.of(item));
        when(identifiers.findAllByItemId(ITEM_ID)).thenReturn(List.of());
        when(items.existsByImageName("0123456789abcdef0123456789abcdef.png")).thenReturn(true);

        service.delete(COLLECTION_ID, ITEM_ID);

        verify(imageService, never()).delete(any());
    }

    @Test
    void createStoresADownloadedCoverLocallyAndPointsTheAttributeAtIt() {
        givenModule();
        when(imageService.downloadToLocal("https://example.com/cover.png"))
                .thenReturn(Optional.of("0123456789abcdef0123456789abcdef.png"));

        service.create(COLLECTION_ID, request("OWNED", Map.of("title", "Dune", "providerImageUrl", "https://example.com/cover.png")));

        ArgumentCaptor<ItemEntity> saved = ArgumentCaptor.forClass(ItemEntity.class);
        verify(items).save(saved.capture());
        assertThat(saved.getValue().getImageName()).isEqualTo("0123456789abcdef0123456789abcdef.png");
        assertThat(saved.getValue().getAttributes()).contains("/api/assets/0123456789abcdef0123456789abcdef.png");
    }

    @Test
    void createRemovesTheDownloadedCoverWhenSavingFails() {
        givenModule();
        when(imageService.downloadToLocal("https://example.com/cover.png"))
                .thenReturn(Optional.of("0123456789abcdef0123456789abcdef.png"));
        when(items.save(any())).thenThrow(new IllegalStateException("database down"));

        assertThatThrownBy(() -> service.create(COLLECTION_ID,
                request("OWNED", Map.of("title", "Dune", "providerImageUrl", "https://example.com/cover.png"))))
                .isInstanceOf(IllegalStateException.class);

        verify(imageService).delete("0123456789abcdef0123456789abcdef.png");
    }

    @Test
    void createIgnoresACoverPathThatIsNotAStoredFileName() {
        givenModule();

        service.create(COLLECTION_ID, request("OWNED", Map.of("title", "Dune", "providerImageUrl", "/api/assets/../../etc/passwd")));

        ArgumentCaptor<ItemEntity> saved = ArgumentCaptor.forClass(ItemEntity.class);
        verify(items).save(saved.capture());
        assertThat(saved.getValue().getImageName()).isNull();
        assertThat(saved.getValue().getAttributes()).doesNotContain("etc/passwd");
    }

    @Test
    void createKeepsOneIdentifierPerTypeAndTheLastValueWins() {
        givenModule();
        var ids = List.of(
                new ItemIdentifierDto(ItemIdentifierEntity.IdType.ISBN13, "111"),
                new ItemIdentifierDto(ItemIdentifierEntity.IdType.ISBN13, " 222 "),
                new ItemIdentifierDto(ItemIdentifierEntity.IdType.UPC, "333"));

        service.create(COLLECTION_ID, new CreateItemRequest(MODULE_ID, "OWNED", null, Map.of("title", "Dune"), ids));

        ArgumentCaptor<ItemIdentifierEntity> saved = ArgumentCaptor.forClass(ItemIdentifierEntity.class);
        verify(identifiers, org.mockito.Mockito.times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(ItemIdentifierEntity::getIdValue).containsExactly("222", "333");
    }

    private static ItemRepository.StateCount row(UUID moduleId, String state, long count) {
        return new ItemRepository.StateCount() {
            @Override
            public UUID getModuleId() {
                return moduleId;
            }

            @Override
            public String getStateKey() {
                return state;
            }

            @Override
            public long getCount() {
                return count;
            }
        };
    }

    @Test
    void countsSumsTheStatesOfEachModuleAndNeedsOnlyViewerAccess() {
        UUID otherModule = UUID.fromString("66666666-6666-6666-6666-666666666666");
        when(items.countByModuleAndState(COLLECTION_ID)).thenReturn(List.of(
                row(MODULE_ID, "OWNED", 3), row(MODULE_ID, "WISHLIST", 2), row(otherModule, "OWNED", 1)));

        ItemCountsResponse counts = service.counts(COLLECTION_ID);

        verify(access).requireRole(COLLECTION_ID, USER_ID, Role.VIEWER);
        assertThat(counts.modules().get(MODULE_ID).total()).isEqualTo(5);
        assertThat(counts.modules().get(MODULE_ID).byState()).containsEntry("OWNED", 3L).containsEntry("WISHLIST", 2L);
        assertThat(counts.modules().get(otherModule).total()).isEqualTo(1);
    }

    @Test
    void countsReportHowManyItemsStillHoldAValueInADeprecatedField() {
        when(items.countByModuleAndState(COLLECTION_ID)).thenReturn(List.of(row(MODULE_ID, "OWNED", 7)));
        when(modules.getEntityById(MODULE_ID)).thenReturn(Optional.of(moduleEntity));
        FieldContract deprecated = new FieldContract("old_authors", "Old authors", FieldType.TEXT, true, false, false, false, 0, true, true,
                null, List.of(), List.of(), null, null, List.of(), Map.of(), "authors");
        FieldContract current = new FieldContract("authors", "Authors", FieldType.TEXT, false, false, false, false, 0, true, false,
                null, List.of(), List.of(), null, null, List.of(), Map.of(), null);
        when(modules.getContract(moduleEntity)).thenReturn(new ModuleContract("books", "1.0.0", "Books", null, null, List.of(), List.of(),
                List.of(deprecated, current), List.of(), Map.of()));
        when(search.count(any())).thenReturn(4L);

        ItemCountsResponse counts = service.counts(COLLECTION_ID);

        assertThat(counts.modules().get(MODULE_ID).deprecatedFieldUse()).containsOnly(Map.entry("old_authors", 4L));
        ArgumentCaptor<ItemQuery> asked = ArgumentCaptor.forClass(ItemQuery.class);
        verify(search).count(asked.capture());
        assertThat(asked.getValue().filters()).singleElement().satisfies(filter -> {
            assertThat(filter.fieldKey()).isEqualTo("old_authors");
            assertThat(filter.operator()).isEqualTo(ItemQuery.FilterOperator.HAS);
        });
    }

    @Test
    void aDeprecatedFieldNobodyUsesAnymoreIsNotReported() {
        when(items.countByModuleAndState(COLLECTION_ID)).thenReturn(List.of(row(MODULE_ID, "OWNED", 7)));
        when(modules.getEntityById(MODULE_ID)).thenReturn(Optional.of(moduleEntity));
        FieldContract deprecated = new FieldContract("old_authors", "Old authors", FieldType.TEXT, false, false, false, false, 0, true, true,
                null, List.of(), List.of(), null, null, List.of(), Map.of(), null);
        when(modules.getContract(moduleEntity)).thenReturn(new ModuleContract("books", "1.0.0", "Books", null, null, List.of(), List.of(),
                List.of(deprecated), List.of(), Map.of()));
        when(search.count(any())).thenReturn(0L);

        assertThat(service.counts(COLLECTION_ID).modules().get(MODULE_ID).deprecatedFieldUse()).isEmpty();
    }

    @Test
    void countsAreEmptyForACollectionWithoutItems() {
        when(items.countByModuleAndState(COLLECTION_ID)).thenReturn(List.of());

        assertThat(service.counts(COLLECTION_ID).modules()).isEmpty();
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
                null, List.of(), List.of(), null, null, List.of(), Map.of(), null);
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
