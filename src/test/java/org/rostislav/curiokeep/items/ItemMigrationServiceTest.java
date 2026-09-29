package org.rostislav.curiokeep.items;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rostislav.curiokeep.collections.CollectionAccessService;
import org.rostislav.curiokeep.collections.api.dto.Role;
import org.rostislav.curiokeep.items.api.dto.MigrationPreviewResponse;
import org.rostislav.curiokeep.items.api.dto.MigrationResultResponse;
import org.rostislav.curiokeep.items.entities.ItemEntity;
import org.rostislav.curiokeep.modules.ModuleQueryService;
import org.rostislav.curiokeep.modules.contract.FieldContract;
import org.rostislav.curiokeep.modules.contract.FieldType;
import org.rostislav.curiokeep.modules.contract.MigrationContract;
import org.rostislav.curiokeep.modules.contract.MigrationOp;
import org.rostislav.curiokeep.modules.contract.MigrationStep;
import org.rostislav.curiokeep.modules.contract.ModuleContract;
import org.rostislav.curiokeep.modules.entities.ModuleDefinitionEntity;
import org.rostislav.curiokeep.user.CurrentUserService;
import org.rostislav.curiokeep.user.entities.AppUserEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemMigrationServiceTest {

    private static final UUID COLLECTION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MODULE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID USER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Mock
    ItemRepository items;
    @Mock
    ModuleQueryService modules;
    @Mock
    CollectionAccessService access;
    @Mock
    CurrentUserService currentUser;

    ItemMigrationService service;
    ModuleDefinitionEntity moduleEntity;

    @BeforeEach
    void setUp() {
        service = new ItemMigrationService(items, modules, access, currentUser, new ObjectMapper(),
                new TransactionTemplate(mock(PlatformTransactionManager.class)));
        AppUserEntity user = new AppUserEntity();
        user.setId(USER_ID);
        when(currentUser.requireCurrentUser()).thenReturn(user);
        moduleEntity = new ModuleDefinitionEntity();
        moduleEntity.setId(MODULE_ID);
    }

    private void givenModuleWithMigration() {
        when(modules.getEntityById(MODULE_ID)).thenReturn(Optional.of(moduleEntity));
        FieldContract notes = new FieldContract("notes", "Notes", FieldType.TEXT, false, false, false, false, 0, true, false,
                null, List.of(), List.of(), null, null, List.of(), Map.of(), null);
        MigrationContract toTwo = new MigrationContract("2.0.0",
                List.of(new MigrationStep(MigrationOp.MOVE, "old_notes", "notes", null, null, null, List.of())));
        when(modules.getContract(moduleEntity)).thenReturn(new ModuleContract("books", "2.0.0", "Books", null, null, List.of(), List.of(),
                List.of(notes), List.of(), Map.of(), List.of(toTwo)));
    }

    private ItemEntity item(String version, String attributes) {
        ItemEntity item = new ItemEntity();
        item.setId(UUID.randomUUID());
        item.setCollectionId(COLLECTION_ID);
        item.setModuleId(MODULE_ID);
        item.setModuleVersion(version);
        item.setAttributes(attributes);
        item.setCreatedAt(OffsetDateTime.parse("2026-01-01T00:00:00Z"));
        return item;
    }

    @Test
    void previewAndAcceptNeedTheAdminRoleAndTouchNothingWhenItIsMissing() {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN)).when(access).requireRole(COLLECTION_ID, USER_ID, Role.ADMIN);

        assertThatThrownBy(() -> service.preview(COLLECTION_ID, MODULE_ID)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.apply(COLLECTION_ID, MODULE_ID)).isInstanceOf(ResponseStatusException.class);

        verifyNoInteractions(items, modules);
    }

    @Test
    void anUnknownModuleIsABadRequest() {
        when(modules.getEntityById(MODULE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.preview(COLLECTION_ID, MODULE_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> assertThat(ex.getReason()).isEqualTo("MODULE_NOT_FOUND"));
    }

    @Test
    void previewCountsWhatWouldChangeAndSavesNothing() {
        givenModuleWithMigration();
        when(items.findBehindFirstBatch(eq(COLLECTION_ID), eq(MODULE_ID), eq("2.0.0"), any(Pageable.class))).thenReturn(List.of(
                item("1.0.0", "{\"old_notes\":\"a\"}"),
                item("1.0.0", "{\"title\":\"nothing to move\"}"),
                item("3.0.0", "{\"old_notes\":\"from the future\"}"),
                item("1.5.0", "not json")));

        MigrationPreviewResponse preview = service.preview(COLLECTION_ID, MODULE_ID);

        assertThat(preview.behind()).isEqualTo(2);
        assertThat(preview.changed()).isEqualTo(1);
        assertThat(preview.versions()).singleElement().satisfies(v -> assertThat(v.items()).isEqualTo(2));
        assertThat(preview.samples()).singleElement().satisfies(sample -> assertThat(sample.changes())
                .extracting(MigrationPreviewResponse.FieldChange::field).containsExactly("notes", "old_notes"));
        verify(items, never()).save(any());
    }

    @Test
    void acceptingRewritesChangedItemsMovesAllToTheCurrentVersionAndSkipsUnreadableOnes() {
        givenModuleWithMigration();
        ItemEntity changed = item("1.0.0", "{\"old_notes\":\"a\"}");
        ItemEntity untouched = item("1.0.0", "{\"title\":\"x\"}");
        ItemEntity newer = item("3.0.0", "{\"old_notes\":\"from the future\"}");
        ItemEntity broken = item("1.0.0", "not json");
        when(items.findBehindFirstBatch(eq(COLLECTION_ID), eq(MODULE_ID), eq("2.0.0"), any(Pageable.class)))
                .thenReturn(List.of(changed, untouched, newer, broken));

        MigrationResultResponse result = service.apply(COLLECTION_ID, MODULE_ID);

        assertThat(result).isEqualTo(new MigrationResultResponse(2, 1, 1));
        assertThat(changed.getAttributes()).isEqualTo("{\"notes\":\"a\"}");
        assertThat(changed.getModuleVersion()).isEqualTo("2.0.0");
        assertThat(untouched.getAttributes()).isEqualTo("{\"title\":\"x\"}");
        assertThat(untouched.getModuleVersion()).isEqualTo("2.0.0");
        assertThat(newer.getModuleVersion()).isEqualTo("3.0.0");
        assertThat(broken.getModuleVersion()).isEqualTo("1.0.0");
    }
}
