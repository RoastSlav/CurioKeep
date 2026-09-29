package org.rostislav.curiokeep.items;

import org.rostislav.curiokeep.modules.FieldValues;
import org.rostislav.curiokeep.modules.contract.FieldContract;
import org.rostislav.curiokeep.modules.contract.ModuleContract;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Enforces {@code uniqueWithinCollection}: no two items of a module in one collection hold the same value in such a field. Values
 * are compared ignoring case and surrounding spaces, and an empty value never conflicts. The check happens when an item is saved,
 * so two saves at the same moment can both pass, and duplicates that already exist stay as they are.
 * <p>
 * Rejections use the reason {@code DUPLICATE_FIELD_<key>} with status 409.
 */
@Component
class ItemUniqueness {

    private final ItemUniquenessRepository repository;

    ItemUniqueness(ItemUniquenessRepository repository) {
        this.repository = repository;
    }

    /** Whether the module can enforce uniqueness on this field; retired fields cannot be edited, so they are not held to it. */
    static boolean isEnforced(FieldContract field) {
        return field.constraints() != null && Boolean.TRUE.equals(field.constraints().uniqueWithinCollection())
                && FieldValues.canBeUnique(field.type()) && field.active() && !field.deprecated();
    }

    /**
     * Rejects a new or changed item whose value is already used by another item. {@code stored} and {@code itemId} describe the
     * item being changed and are null for a new one; a value the item already held is not checked again, so an existing duplicate
     * does not block an unrelated edit.
     */
    void requireUnique(UUID collectionId, UUID moduleId, ModuleContract contract, JsonNode attributes, JsonNode stored, UUID itemId) {
        for (FieldContract field : contract.fields()) {
            if (!isEnforced(field)) continue;
            String value = comparable(attributes.get(field.key()));
            if (value == null) continue;
            if (stored != null && value.equalsIgnoreCase(comparable(stored.get(field.key())))) continue;
            if (repository.isTaken(collectionId, moduleId, field.key(), value, itemId)) throw duplicate(field);
        }
    }

    /** Starts an import into the collection, which checks its items against those already there and against each other. */
    Claims claims(UUID collectionId) {
        return new Claims(collectionId);
    }

    final class Claims {
        private final UUID collectionId;
        private final Map<String, Set<String>> taken = new HashMap<>();

        private Claims(UUID collectionId) {
            this.collectionId = collectionId;
        }

        /** Rejects the item if a unique value is taken, otherwise records its values so later items conflict with it. */
        void claim(UUID moduleId, ModuleContract contract, JsonNode attributes) {
            List<FieldContract> fields = contract.fields().stream().filter(ItemUniqueness::isEnforced).toList();
            for (FieldContract field : fields) {
                String value = comparable(attributes.get(field.key()));
                if (value != null && takenValues(moduleId, field).contains(normalize(value))) throw duplicate(field);
            }
            for (FieldContract field : fields) {
                String value = comparable(attributes.get(field.key()));
                if (value != null) takenValues(moduleId, field).add(normalize(value));
            }
        }

        private Set<String> takenValues(UUID moduleId, FieldContract field) {
            return taken.computeIfAbsent(moduleId + "/" + field.key(), key -> {
                Set<String> values = new HashSet<>();
                repository.valuesOf(collectionId, moduleId, field.key()).forEach(value -> values.add(normalize(value)));
                return values;
            });
        }
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    /** The text to compare, or null when the item has no value there. */
    private static String comparable(JsonNode value) {
        if (value == null || value.isNull()) return null;
        String text = value.isString() ? value.asString() : value.isNumber() ? value.asString() : null;
        return text == null || text.isBlank() ? null : text.trim();
    }

    private static ResponseStatusException duplicate(FieldContract field) {
        return new ResponseStatusException(HttpStatus.CONFLICT, "DUPLICATE_FIELD_" + field.key());
    }
}
