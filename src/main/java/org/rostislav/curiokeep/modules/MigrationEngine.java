package org.rostislav.curiokeep.modules;

import org.rostislav.curiokeep.modules.contract.FieldContract;
import org.rostislav.curiokeep.modules.contract.MigrationContract;
import org.rostislav.curiokeep.modules.contract.MigrationStep;
import org.rostislav.curiokeep.modules.contract.ModuleContract;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.StringNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * Applies the migration steps a module declares to the attributes of one item. Steps are conservative: they never overwrite a
 * value the item already has and never store a value the target field would reject, so a step that does not fit an item leaves
 * that item as it was.
 */
public final class MigrationEngine {

    private MigrationEngine() {
    }

    /** The steps that bring an item saved under {@code fromVersion} up to the module's current version, oldest first. */
    public static List<MigrationStep> stepsSince(ModuleContract module, String fromVersion) {
        return module.migrations().stream()
                .filter(m -> ModuleVersion.compare(m.to(), fromVersion) > 0 && ModuleVersion.compare(m.to(), module.version()) <= 0)
                .map(MigrationContract::steps)
                .flatMap(List::stream)
                .toList();
    }

    /** Returns a migrated copy of {@code attributes}; the argument is not changed. */
    public static ObjectNode apply(ModuleContract module, List<MigrationStep> steps, ObjectNode attributes) {
        Map<String, FieldContract> fields = new HashMap<>();
        module.fields().forEach(field -> fields.put(field.key(), field));
        ObjectNode result = attributes.deepCopy();
        for (MigrationStep step : steps) {
            switch (step.op()) {
                case MOVE -> transfer(step, fields, result, true);
                case COPY -> transfer(step, fields, result, false);
                case MAP -> mapValues(step, result);
                case DEFAULT -> setDefault(step, fields, result);
                case DROP -> result.remove(step.field());
            }
        }
        return result;
    }

    private static void transfer(MigrationStep step, Map<String, FieldContract> fields, ObjectNode attributes, boolean removeSource) {
        JsonNode source = attributes.get(step.from());
        if (isEmpty(source)) {
            if (removeSource) attributes.remove(step.from());
            return;
        }
        FieldContract target = fields.get(step.to());
        JsonNode value = ValueTransforms.apply(source, step.transform());
        if (target == null || value == null || !isEmpty(attributes.get(step.to())) || !FieldValues.isValid(target, value)) return;
        attributes.set(step.to(), value);
        if (removeSource) attributes.remove(step.from());
    }

    private static void mapValues(MigrationStep step, ObjectNode attributes) {
        JsonNode current = attributes.get(step.field());
        if (current == null) return;
        Map<String, String> table = new HashMap<>();
        step.mappings().forEach(m -> table.put(m.from(), m.to()));
        UnaryOperator<JsonNode> replace = value -> value.isString() && table.containsKey(value.asString())
                ? StringNode.valueOf(table.get(value.asString())) : value;
        if (current.isArray()) {
            ArrayNode mapped = attributes.arrayNode();
            current.forEach(element -> mapped.add(replace.apply(element)));
            attributes.set(step.field(), mapped);
        } else {
            attributes.set(step.field(), replace.apply(current));
        }
    }

    private static void setDefault(MigrationStep step, Map<String, FieldContract> fields, ObjectNode attributes) {
        FieldContract field = fields.get(step.field());
        if (field == null || !isEmpty(attributes.get(step.field()))) return;
        FieldValues.parse(field, step.value()).ifPresent(value -> attributes.set(step.field(), value));
    }

    private static boolean isEmpty(JsonNode value) {
        return value == null || value.isNull() || (value.isString() && value.asString().isBlank()) || (value.isArray() && value.isEmpty());
    }
}
