package org.rostislav.curiokeep.items;

import org.rostislav.curiokeep.modules.FieldValues;
import org.rostislav.curiokeep.modules.contract.FieldContract;
import org.rostislav.curiokeep.modules.contract.ModuleContract;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

/**
 * Checks item attributes against the module contract: required fields are present and every declared field holds a value
 * of its type that satisfies the field's constraints. Keys the contract does not declare are left alone, because a module
 * can drop a field without rewriting stored items.
 * <p>
 * Rejections use the reasons {@code MISSING_REQUIRED_FIELD_<key>} and {@code INVALID_FIELD_<key>}.
 */
final class ItemAttributeValidator {

    static final int MAX_ATTRIBUTES_BYTES = 256 * 1024;

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
            if (!FieldValues.isValid(field, value)) throw badRequest("INVALID_FIELD_" + field.key());
        }
    }

    private static ResponseStatusException badRequest(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }
}
