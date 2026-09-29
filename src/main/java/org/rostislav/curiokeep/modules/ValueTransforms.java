package org.rostislav.curiokeep.modules;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.IntNode;
import tools.jackson.databind.node.StringNode;

import java.util.ArrayList;
import java.util.List;

/** The value transforms a module can name in a provider mapping or a migration step: TRIM, JOIN_COMMA, FIRST, TO_INT. */
public final class ValueTransforms {

    private ValueTransforms() {
    }

    /**
     * Applies {@code transform} to {@code value}. Returns {@code null} when the result is empty or cannot be converted, so the
     * caller can skip the value. A null or unknown transform, or one that does not apply to the value's type, returns the value
     * unchanged.
     */
    public static JsonNode apply(JsonNode value, String transform) {
        if (transform == null) return value;
        return switch (transform) {
            case "TRIM" -> trim(value);
            case "JOIN_COMMA" -> joinComma(value);
            case "FIRST" -> first(value);
            case "TO_INT" -> toInt(value);
            default -> value;
        };
    }

    private static JsonNode trim(JsonNode value) {
        if (!value.isString()) return value;
        String trimmed = value.asString().trim();
        return trimmed.isEmpty() ? null : StringNode.valueOf(trimmed);
    }

    private static JsonNode joinComma(JsonNode value) {
        if (!value.isArray()) return value;
        List<String> parts = new ArrayList<>();
        for (JsonNode element : value) {
            String text = element.isNull() ? null : element.asString(null);
            if (text != null && !text.isBlank()) parts.add(text.trim());
        }
        return parts.isEmpty() ? null : StringNode.valueOf(String.join(", ", parts));
    }

    private static JsonNode first(JsonNode value) {
        if (!value.isArray()) return value;
        for (JsonNode element : value) {
            if (!element.isNull() && !(element.isString() && element.asString().isBlank())) return element;
        }
        return null;
    }

    private static JsonNode toInt(JsonNode value) {
        if (value.isNumber()) return IntNode.valueOf(value.asInt());
        if (!value.isString()) return value;
        try {
            return IntNode.valueOf(Integer.parseInt(value.asString().trim()));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
