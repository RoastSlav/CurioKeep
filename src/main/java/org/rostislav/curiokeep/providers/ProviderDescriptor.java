package org.rostislav.curiokeep.providers;

import org.rostislav.curiokeep.items.entities.ItemIdentifierEntity;
import org.rostislav.curiokeep.providers.MetadataProvider;

import java.util.Arrays;
import java.util.List;

public record ProviderDescriptor(
        String key,
        String displayName,
        String description,
        List<ItemIdentifierEntity.IdType> supportedIdTypes,
        Integer priority
) {
    public ProviderDescriptor {
        displayName = displayName == null || displayName.isBlank() ? humanize(key) : displayName;
        description = description;
        supportedIdTypes = supportedIdTypes == null ? List.of() : List.copyOf(supportedIdTypes);
        priority = priority;
    }

    public static ProviderDescriptor basic(MetadataProvider provider) {
        List<ItemIdentifierEntity.IdType> supported = Arrays.stream(ItemIdentifierEntity.IdType.values())
                .filter(provider::supports)
                .toList();
        return new ProviderDescriptor(provider.key(), null, null, supported, null);
    }

    private static String humanize(String key) {
        if (key == null || key.isBlank()) {
            return "Provider";
        }
        String cleaned = key.replace('_', ' ').replace('-', ' ');
        String[] parts = cleaned.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) sb.append(p.substring(1));
            sb.append(' ');
        }
        return sb.toString().trim();
    }
}
