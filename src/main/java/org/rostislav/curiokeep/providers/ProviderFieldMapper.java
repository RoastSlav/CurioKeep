package org.rostislav.curiokeep.providers;

import org.rostislav.curiokeep.modules.entities.ModuleFieldEntity;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ProviderFieldMapper {

    private final ObjectMapper objectMapper;

    public ProviderFieldMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    private static String text(JsonNode n) {
        return (n == null || n.isNull() || n.isMissingNode()) ? null : n.asString(null);
    }

    private static Object toJava(JsonNode n) {
        if (n.isString()) return n.asString();
        if (n.isInt()) return n.asInt();
        if (n.isLong()) return n.asLong();
        if (n.isFloat() || n.isDouble() || n.isBigDecimal()) return n.asDouble();
        if (n.isBoolean()) return n.asBoolean();
        // for arrays/objects keep as raw JSON string for now (simple + safe)
        return n.toString();
    }

    /**
     * Applies a module-declared mapping transform (TRIM, JOIN_COMMA, FIRST, TO_INT) to a provider value.
     * Returns {@code null} when the transformed value is empty or cannot be converted, so the mapping is
     * skipped and the next mapping or provider can fill the field. A null or unknown transform, or one
     * that does not apply to the value's type, leaves the value unchanged.
     */
    private static Object applyTransform(JsonNode value, String transform) {
        if (transform == null) return toJava(value);
        switch (transform) {
            case "TRIM" -> {
                if (!value.isString()) return toJava(value);
                String trimmed = value.asString().trim();
                return trimmed.isEmpty() ? null : trimmed;
            }
            case "JOIN_COMMA" -> {
                if (!value.isArray()) return toJava(value);
                List<String> parts = new ArrayList<>();
                for (JsonNode element : value) {
                    String text = text(element);
                    if (text != null && !text.isBlank()) parts.add(text.trim());
                }
                return parts.isEmpty() ? null : String.join(", ", parts);
            }
            case "FIRST" -> {
                if (!value.isArray()) return toJava(value);
                for (JsonNode element : value) {
                    if (!element.isNull() && !(element.isString() && element.asString().isBlank())) {
                        return toJava(element);
                    }
                }
                return null;
            }
            case "TO_INT" -> {
                if (value.isNumber()) return value.asInt();
                if (!value.isString()) return toJava(value);
                try {
                    return Integer.parseInt(value.asString().trim());
                } catch (NumberFormatException ex) {
                    return null;
                }
            }
            default -> {
                return toJava(value);
            }
        }
    }

    /**
     * Maps provider JSON into module attribute map using module field providerMappings.
     * providerJson is the provider's normalized payload, not its raw API response.
     */
    public Map<String, Object> mapFields(JsonNode providerJson, Iterable<ModuleFieldEntity> moduleFields, String providerKey) {
        Map<String, Object> result = new HashMap<>();

        for (ModuleFieldEntity field : moduleFields) {
            JsonNode mappings = parse(field.getProviderMappings());
            if (mappings == null || !mappings.isArray()) continue;

            for (JsonNode m : mappings) {
                String p = text(m.get("provider"));
                String path = text(m.get("path"));
                String transform = text(m.get("transform"));
                if (p == null || path == null) continue;
                if (!providerKey.equals(p)) continue;

                JsonNode value = providerJson.at(path);
                if (value == null || value.isMissingNode() || value.isNull()) continue;

                Object mapped = applyTransform(value, transform);
                if (mapped == null) continue;

                result.put(field.getFieldKey(), mapped);
                break; // first match wins for this field
            }
        }

        return result;
    }

    private JsonNode parse(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }
}
