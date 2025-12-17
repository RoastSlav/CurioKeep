package org.rostislav.curiokeep.providers;

import org.rostislav.curiokeep.modules.entities.ModuleDefinitionEntity;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public record ModuleProviderSpec(String key, int priority, boolean enabled) {

    public static List<ModuleProviderSpec> fromModule(ModuleDefinitionEntity module, ObjectMapper objectMapper) {
        JsonNode root = parse(module.getDefinitionJson(), objectMapper);
        if (root == null || root.isNull() || root.isMissingNode()) return List.of();

        List<ModuleProviderSpec> out = new ArrayList<>();

        JsonNode providers = root.path("providers");
        if (providers.isArray()) {
            for (JsonNode p : providers) {
                String key = text(p.get("key"));
                boolean enabled = p.path("enabled").asBoolean(true);
                int prio = p.path("priority").asInt(100);
                if (key != null) out.add(new ModuleProviderSpec(key, prio, enabled));
            }
        }

        out.sort(Comparator.comparingInt(ModuleProviderSpec::priority));
        return out;
    }

    private static JsonNode parse(String json, ObjectMapper objectMapper) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }

    private static String text(JsonNode n) {
        if (n == null || n.isNull() || n.isMissingNode()) return null;
        String v = n.asText(null);
        return (v == null || v.isBlank()) ? null : v;
    }
}