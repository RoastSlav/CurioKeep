package org.rostislav.curiokeep.providers;

import com.fasterxml.jackson.databind.JsonNode;
import org.rostislav.curiokeep.modules.entities.ModuleFieldEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ProviderFieldMapper {

    private static String text(JsonNode n) {
        return (n == null || n.isNull() || n.isMissingNode()) ? null : n.asText(null);
    }

    private static Object toJava(JsonNode n) {
        if (n.isTextual()) return n.asText();
        if (n.isInt()) return n.asInt();
        if (n.isLong()) return n.asLong();
        if (n.isFloat() || n.isDouble() || n.isBigDecimal()) return n.asDouble();
        if (n.isBoolean()) return n.asBoolean();
        // for arrays/objects keep as raw JSON string for now (simple + safe)
        return n.toString();
    }

    /**
     * Maps provider JSON into module attribute map using module field providerMappings.
     * providerJson is expected to be normalized for the provider (your decision).
     */
    public Map<String, Object> mapFields(JsonNode providerJson, Iterable<ModuleFieldEntity> moduleFields, String providerKey) {
        Map<String, Object> result = new HashMap<>();

        for (ModuleFieldEntity field : moduleFields) {
            JsonNode mappings = field.getProviderMappings(); // you implement this
            if (mappings == null || !mappings.isArray()) continue;

            for (JsonNode m : mappings) {
                String p = text(m.get("provider"));
                String path = text(m.get("path"));
                if (p == null || path == null) continue;
                if (!providerKey.equals(p)) continue;

                JsonNode value = providerJson.at(path);
                if (value == null || value.isMissingNode() || value.isNull()) continue;

                result.put(field.getFieldKey(), toJava(value));
                break; // first match wins for this field
            }
        }

        return result;
    }
}
