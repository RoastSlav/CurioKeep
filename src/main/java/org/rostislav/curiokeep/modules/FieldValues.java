package org.rostislav.curiokeep.modules;

import org.rostislav.curiokeep.modules.contract.Constraints;
import org.rostislav.curiokeep.modules.contract.EnumValue;
import org.rostislav.curiokeep.modules.contract.FieldContract;
import org.rostislav.curiokeep.modules.contract.FieldType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.BooleanNode;
import tools.jackson.databind.node.DoubleNode;
import tools.jackson.databind.node.IntNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.LongNode;
import tools.jackson.databind.node.StringNode;

import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Decides whether a JSON value is a valid value for a module field, and turns declared text into one. */
public final class FieldValues {

    private static final int MAX_TEXT_LENGTH = 20_000;
    private static final int MAX_TAGS = 200;
    private static final Pattern PARTIAL_ISO_DATE = Pattern.compile("\\d{4}(-\\d{2}(-\\d{2})?)?");

    private FieldValues() {
    }

    /** True when {@code value} has the field's type and satisfies its constraints. */
    public static boolean isValid(FieldContract field, JsonNode value) {
        Constraints c = field.constraints();
        return switch (field.type()) {
            case TEXT, LINK -> value.isString() && validText(value.asString(), c);
            case NUMBER -> value.isNumber() && inRange(value.doubleValue(), c);
            case BOOLEAN -> value.isBoolean();
            case DATE -> value.isString() && PARTIAL_ISO_DATE.matcher(value.asString().trim()).matches();
            case ENUM -> validEnum(field, value);
            case TAGS -> value.isArray() && value.size() <= MAX_TAGS && allStrings(value);
            case JSON -> true;
        };
    }

    /** Whether the field's value is a single scalar that can be compared, so a module can ask for it to be unique in a collection. */
    public static boolean canBeUnique(FieldType type) {
        return type == FieldType.TEXT || type == FieldType.LINK || type == FieldType.NUMBER || type == FieldType.DATE;
    }

    /**
     * Reads the text of a declared value (a migration's {@code value} attribute) as a value of the field's type: a number for
     * NUMBER, {@code true} or {@code false} for BOOLEAN, comma separated entries for TAGS. Empty when the text does not make
     * a valid value, and always empty for JSON fields.
     */
    public static Optional<JsonNode> parse(FieldContract field, String text) {
        JsonNode parsed = switch (field.type()) {
            case TEXT, LINK, DATE -> StringNode.valueOf(text);
            case NUMBER -> parseNumber(text);
            case BOOLEAN -> parseBoolean(text);
            case ENUM -> multi(field) ? array(text) : StringNode.valueOf(text);
            case TAGS -> array(text);
            case JSON -> null;
        };
        return Optional.ofNullable(parsed).filter(value -> isValid(field, value));
    }

    private static JsonNode parseNumber(String text) {
        try {
            long whole = Long.parseLong(text.trim());
            return whole == (int) whole ? IntNode.valueOf((int) whole) : LongNode.valueOf(whole);
        } catch (NumberFormatException notWhole) {
            try {
                return DoubleNode.valueOf(Double.parseDouble(text.trim()));
            } catch (NumberFormatException notNumber) {
                return null;
            }
        }
    }

    private static JsonNode parseBoolean(String text) {
        String trimmed = text.trim();
        if (trimmed.equalsIgnoreCase("true")) return BooleanNode.TRUE;
        if (trimmed.equalsIgnoreCase("false")) return BooleanNode.FALSE;
        return null;
    }

    private static JsonNode array(String text) {
        ArrayNode entries = JsonNodeFactory.instance.arrayNode();
        for (String entry : text.split(",")) {
            if (!entry.isBlank()) entries.add(entry.trim());
        }
        return entries.isEmpty() ? null : entries;
    }

    private static boolean validText(String text, Constraints c) {
        if (text.length() > MAX_TEXT_LENGTH) return false;
        if (c == null) return true;
        if (c.minLength() != null && text.length() < c.minLength()) return false;
        if (c.maxLength() != null && text.length() > c.maxLength()) return false;
        if (c.pattern() == null || c.pattern().isBlank()) return true;
        try {
            return Pattern.compile(c.pattern()).matcher(text).find();
        } catch (PatternSyntaxException e) {
            return true; // a broken pattern is the module author's mistake; it must not block every save
        }
    }

    private static boolean inRange(double number, Constraints c) {
        if (Double.isNaN(number) || Double.isInfinite(number)) return false;
        if (c == null) return true;
        return (c.min() == null || number >= c.min()) && (c.max() == null || number <= c.max());
    }

    private static boolean validEnum(FieldContract field, JsonNode value) {
        if (value.isString()) return declared(field, value.asString());
        if (multi(field) && value.isArray() && value.size() <= MAX_TAGS) {
            for (JsonNode element : value) {
                if (!element.isString() || !declared(field, element.asString())) return false;
            }
            return true;
        }
        return false;
    }

    private static boolean multi(FieldContract field) {
        return field.constraints() != null && Boolean.TRUE.equals(field.constraints().multi());
    }

    /** With no declared choices anything goes, which the module docs describe as an empty choice list. */
    private static boolean declared(FieldContract field, String key) {
        if (field.enumValues().isEmpty()) return true;
        return field.enumValues().stream().map(EnumValue::key).anyMatch(key::equals);
    }

    private static boolean allStrings(JsonNode array) {
        for (JsonNode element : array) {
            if (!element.isString() || element.asString().length() > MAX_TEXT_LENGTH) return false;
        }
        return true;
    }
}
