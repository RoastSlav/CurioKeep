package org.rostislav.curiokeep.items;

import org.rostislav.curiokeep.modules.contract.Constraints;
import org.rostislav.curiokeep.modules.contract.EnumValue;
import org.rostislav.curiokeep.modules.contract.FieldContract;
import org.rostislav.curiokeep.modules.contract.FieldType;
import org.rostislav.curiokeep.modules.contract.ModuleContract;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Checks item attributes against the module contract: required fields are present and every declared field holds a value
 * of its type that satisfies the field's constraints. Keys the contract does not declare are left alone, because a module
 * can drop a field without rewriting stored items.
 * <p>
 * Rejections use the reasons {@code MISSING_REQUIRED_FIELD_<key>} and {@code INVALID_FIELD_<key>}.
 */
final class ItemAttributeValidator {

    static final int MAX_ATTRIBUTES_BYTES = 256 * 1024;
    private static final int MAX_TEXT_LENGTH = 20_000;
    private static final int MAX_TAGS = 200;
    private static final Pattern PARTIAL_ISO_DATE = Pattern.compile("\\d{4}(-\\d{2}(-\\d{2})?)?");

    private ItemAttributeValidator() {
    }

    static void validate(ModuleContract contract, JsonNode attributes) {
        if (attributes == null || !attributes.isObject()) {
            throw badRequest("ATTRIBUTES_MUST_BE_OBJECT");
        }
        if (attributes.toString().length() > MAX_ATTRIBUTES_BYTES) {
            throw badRequest("ATTRIBUTES_TOO_LARGE");
        }
        for (FieldContract field : contract.fields()) {
            JsonNode value = attributes.get(field.key());
            boolean absent = value == null || value.isNull() || (value.isString() && value.asString().isBlank());
            if (absent) {
                // A retired field can no longer be required: nothing on screen lets the user fill it in.
                if (field.required() && field.active() && !field.deprecated()) throw badRequest("MISSING_REQUIRED_FIELD_" + field.key());
                continue;
            }
            if (!isValid(field, value)) throw badRequest("INVALID_FIELD_" + field.key());
        }
    }

    private static boolean isValid(FieldContract field, JsonNode value) {
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
        boolean multi = field.constraints() != null && Boolean.TRUE.equals(field.constraints().multi());
        if (value.isString()) return declared(field, value.asString());
        if (multi && value.isArray() && value.size() <= MAX_TAGS) {
            for (JsonNode element : value) {
                if (!element.isString() || !declared(field, element.asString())) return false;
            }
            return true;
        }
        return false;
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

    private static ResponseStatusException badRequest(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }
}
