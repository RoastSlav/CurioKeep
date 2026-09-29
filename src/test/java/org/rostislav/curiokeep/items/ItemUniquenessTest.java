package org.rostislav.curiokeep.items;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rostislav.curiokeep.modules.contract.Constraints;
import org.rostislav.curiokeep.modules.contract.FieldContract;
import org.rostislav.curiokeep.modules.contract.FieldType;
import org.rostislav.curiokeep.modules.contract.ModuleContract;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemUniquenessTest {

    private static final UUID COLLECTION = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MODULE = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID ITEM = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @Mock
    ItemUniquenessRepository repository;

    private final ObjectMapper json = new ObjectMapper();

    private FieldContract field(String key, FieldType type, boolean unique, boolean active, boolean deprecated) {
        return new FieldContract(key, key, type, false, false, false, false, 0, active, deprecated, null, List.of(), List.of(),
                new Constraints(null, null, null, null, null, null, unique), null, List.of(), Map.of(), null);
    }

    private ModuleContract module(FieldContract... fields) {
        return new ModuleContract("books", "1.0.0", "Books", null, null, List.of(), List.of(), List.of(fields), List.of(), Map.of());
    }

    private JsonNode attributes(String text) {
        return json.readTree(text);
    }

    private ItemUniqueness uniqueness() {
        return new ItemUniqueness(repository);
    }

    @Test
    void onlyLiveScalarFieldsThatAskForUniquenessAreEnforced() {
        assertThat(ItemUniqueness.isEnforced(field("isbn", FieldType.TEXT, true, true, false))).isTrue();
        assertThat(ItemUniqueness.isEnforced(field("year", FieldType.NUMBER, true, true, false))).isTrue();
        assertThat(ItemUniqueness.isEnforced(field("plain", FieldType.TEXT, false, true, false))).isFalse();
        assertThat(ItemUniqueness.isEnforced(field("tags", FieldType.TAGS, true, true, false))).isFalse();
        assertThat(ItemUniqueness.isEnforced(field("retired", FieldType.TEXT, true, true, true))).isFalse();
        assertThat(ItemUniqueness.isEnforced(field("off", FieldType.TEXT, true, false, false))).isFalse();
    }

    @Test
    void aNewItemWhoseValueIsTakenIsRejectedWithAConflict() {
        when(repository.isTaken(COLLECTION, MODULE, "isbn", "978-1", null)).thenReturn(true);

        assertThatThrownBy(() -> uniqueness().requireUnique(COLLECTION, MODULE, module(field("isbn", FieldType.TEXT, true, true, false)),
                attributes("{\"isbn\":\" 978-1 \"}"), null, null))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ex.getReason()).isEqualTo("DUPLICATE_FIELD_isbn");
                });
    }

    @Test
    void numbersAreComparedByTheirText() {
        when(repository.isTaken(COLLECTION, MODULE, "year", "1965", null)).thenReturn(true);

        assertThatThrownBy(() -> uniqueness().requireUnique(COLLECTION, MODULE, module(field("year", FieldType.NUMBER, true, true, false)),
                attributes("{\"year\":1965}"), null, null)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void anEmptyValueNeverConflictsAndIsNotLookedUp() {
        assertThatCode(() -> uniqueness().requireUnique(COLLECTION, MODULE, module(field("isbn", FieldType.TEXT, true, true, false)),
                attributes("{\"isbn\":\"  \",\"other\":\"x\"}"), null, null)).doesNotThrowAnyException();
        assertThatCode(() -> uniqueness().requireUnique(COLLECTION, MODULE, module(field("isbn", FieldType.TEXT, true, true, false)),
                attributes("{}"), null, null)).doesNotThrowAnyException();

        verifyNoInteractions(repository);
    }

    @Test
    void anEditKeepingItsValueIsNotCheckedSoAnExistingDuplicateDoesNotBlockIt() {
        assertThatCode(() -> uniqueness().requireUnique(COLLECTION, MODULE, module(field("isbn", FieldType.TEXT, true, true, false)),
                attributes("{\"isbn\":\"978-1\",\"title\":\"new\"}"), attributes("{\"isbn\":\" 978-1\"}"), ITEM)).doesNotThrowAnyException();

        verify(repository, never()).isTaken(any(), any(), any(), any(), any());
    }

    @Test
    void anEditChangingTheValueIsCheckedAgainstOtherItemsOnly() {
        when(repository.isTaken(COLLECTION, MODULE, "isbn", "978-2", ITEM)).thenReturn(false);

        assertThatCode(() -> uniqueness().requireUnique(COLLECTION, MODULE, module(field("isbn", FieldType.TEXT, true, true, false)),
                attributes("{\"isbn\":\"978-2\"}"), attributes("{\"isbn\":\"978-1\"}"), ITEM)).doesNotThrowAnyException();

        verify(repository).isTaken(COLLECTION, MODULE, "isbn", "978-2", ITEM);
    }

    @Test
    void anImportLoadsTheStoredValuesOnceAndComparesIgnoringCase() {
        when(repository.valuesOf(COLLECTION, MODULE, "isbn")).thenReturn(List.of("ABC-1"));
        ItemUniqueness.Claims claims = uniqueness().claims(COLLECTION);
        ModuleContract module = module(field("isbn", FieldType.TEXT, true, true, false));

        assertThatThrownBy(() -> claims.claim(MODULE, module, attributes("{\"isbn\":\"abc-1\"}"))).isInstanceOf(ResponseStatusException.class);
        claims.claim(MODULE, module, attributes("{\"isbn\":\"abc-2\"}"));
        assertThatThrownBy(() -> claims.claim(MODULE, module, attributes("{\"isbn\":\" ABC-2 \"}"))).isInstanceOf(ResponseStatusException.class);

        verify(repository).valuesOf(COLLECTION, MODULE, "isbn");
    }

    @Test
    void aRejectedImportItemClaimsNoneOfItsValues() {
        when(repository.valuesOf(COLLECTION, MODULE, "isbn")).thenReturn(List.of("taken"));
        when(repository.valuesOf(COLLECTION, MODULE, "asin")).thenReturn(List.of());
        ItemUniqueness.Claims claims = uniqueness().claims(COLLECTION);
        ModuleContract module = module(field("asin", FieldType.TEXT, true, true, false), field("isbn", FieldType.TEXT, true, true, false));

        assertThatThrownBy(() -> claims.claim(MODULE, module, attributes("{\"asin\":\"B1\",\"isbn\":\"taken\"}"))).isInstanceOf(ResponseStatusException.class);

        assertThatCode(() -> claims.claim(MODULE, module, attributes("{\"asin\":\"B1\",\"isbn\":\"free\"}"))).doesNotThrowAnyException();
    }
}
