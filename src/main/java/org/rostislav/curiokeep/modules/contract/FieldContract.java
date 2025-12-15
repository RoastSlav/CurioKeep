package org.rostislav.curiokeep.modules.contract;

import java.util.List;
import java.util.Map;

public record FieldContract(
        String key,
        String label,
        FieldType type,
        boolean required,
        boolean searchable,
        boolean filterable,
        boolean sortable,
        int order,
        boolean active,
        boolean deprecated,
        Object defaultValue,
        List<IdentifierType> identifiers,
        List<EnumValue> enumValues,
        Constraints constraints,
        UiHints ui,
        List<ProviderMapping> providerMappings,
        Map<String, Object> extensions
) {
    public FieldContract {
        identifiers = identifiers == null ? List.of() : List.copyOf(identifiers);
        enumValues = enumValues == null ? List.of() : List.copyOf(enumValues);
        providerMappings = providerMappings == null ? List.of() : List.copyOf(providerMappings);
        extensions = extensions == null ? Map.of() : Map.copyOf(extensions);
    }
}