package org.rostislav.curiokeep.items;

import org.junit.jupiter.api.Test;
import org.rostislav.curiokeep.modules.contract.Constraints;
import org.rostislav.curiokeep.modules.contract.EnumValue;
import org.rostislav.curiokeep.modules.contract.FieldContract;
import org.rostislav.curiokeep.modules.contract.FieldType;
import org.rostislav.curiokeep.modules.contract.ModuleContract;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItemAttributeValidatorTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private static FieldContract field(String key, FieldType type, boolean required, Constraints constraints, List<EnumValue> values) {
        return new FieldContract(key, key, type, required, false, false, false, 0, true, false,
                null, List.of(), values, constraints, null, List.of(), Map.of(), null);
    }

    private ModuleContract contract(FieldContract... fields) {
        return new ModuleContract("m", "1.0.0", "M", null, null, List.of(), List.of(), List.of(fields), List.of(), Map.of());
    }

    private JsonNode json(String text) {
        return mapper.readTree(text);
    }

    private void assertRejected(ModuleContract contract, String attributes, String reason) {
        assertThatThrownBy(() -> ItemAttributeValidator.validate(contract, json(attributes)))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getReason()).isEqualTo(reason);
                });
    }

    private void assertAccepted(ModuleContract contract, String attributes) {
        assertThatCode(() -> ItemAttributeValidator.validate(contract, json(attributes))).doesNotThrowAnyException();
    }

    @Test
    void requiresAnObjectAndTheRequiredFields() {
        ModuleContract c = contract(field("title", FieldType.TEXT, true, null, List.of()));

        assertRejected(c, "[]", "ATTRIBUTES_MUST_BE_OBJECT");
        assertRejected(c, "{}", "MISSING_REQUIRED_FIELD_title");
        assertRejected(c, "{\"title\":\"  \"}", "MISSING_REQUIRED_FIELD_title");
        assertRejected(c, "{\"title\":null}", "MISSING_REQUIRED_FIELD_title");
        assertAccepted(c, "{\"title\":\"Dune\"}");
    }

    @Test
    void optionalFieldsMayBeAbsentOrEmpty() {
        ModuleContract c = contract(field("pages", FieldType.NUMBER, false, null, List.of()));

        assertAccepted(c, "{}");
        assertAccepted(c, "{\"pages\":null}");
        assertAccepted(c, "{\"pages\":\"\"}");
    }

    @Test
    void textMustBeAStringWithinItsConstraints() {
        ModuleContract c = contract(field("code", FieldType.TEXT, false, new Constraints(null, null, 2, 5, "^[A-Z]+$", null, null), List.of()));

        assertAccepted(c, "{\"code\":\"ABC\"}");
        assertRejected(c, "{\"code\":5}", "INVALID_FIELD_code");
        assertRejected(c, "{\"code\":\"A\"}", "INVALID_FIELD_code");
        assertRejected(c, "{\"code\":\"ABCDEF\"}", "INVALID_FIELD_code");
        assertRejected(c, "{\"code\":\"abc\"}", "INVALID_FIELD_code");
    }

    @Test
    void aBrokenPatternInTheModuleDoesNotBlockSaving() {
        ModuleContract c = contract(field("code", FieldType.TEXT, false, new Constraints(null, null, null, null, "([", null, null), List.of()));

        assertAccepted(c, "{\"code\":\"anything\"}");
    }

    @Test
    void numbersMustBeNumbersInsideTheirRange() {
        ModuleContract c = contract(field("pages", FieldType.NUMBER, false, new Constraints(1.0, 5000.0, null, null, null, null, null), List.of()));

        assertAccepted(c, "{\"pages\":412}");
        assertAccepted(c, "{\"pages\":12.5}");
        assertRejected(c, "{\"pages\":\"abc\"}", "INVALID_FIELD_pages");
        assertRejected(c, "{\"pages\":\"412\"}", "INVALID_FIELD_pages");
        assertRejected(c, "{\"pages\":0}", "INVALID_FIELD_pages");
        assertRejected(c, "{\"pages\":5001}", "INVALID_FIELD_pages");
        assertRejected(c, "{\"pages\":[1]}", "INVALID_FIELD_pages");
    }

    @Test
    void booleansDatesAndTags() {
        ModuleContract c = contract(
                field("signed", FieldType.BOOLEAN, false, null, List.of()),
                field("bought", FieldType.DATE, false, null, List.of()),
                field("labels", FieldType.TAGS, false, null, List.of()));

        assertAccepted(c, "{\"signed\":true,\"bought\":\"2020-05-17\",\"labels\":[\"a\",\"b\"]}");
        assertAccepted(c, "{\"bought\":\"2020\"}");
        assertAccepted(c, "{\"bought\":\"2020-05\"}");
        assertRejected(c, "{\"signed\":\"yes\"}", "INVALID_FIELD_signed");
        assertRejected(c, "{\"bought\":\"May 2020\"}", "INVALID_FIELD_bought");
        assertRejected(c, "{\"bought\":20200517}", "INVALID_FIELD_bought");
        assertRejected(c, "{\"labels\":\"a,b\"}", "INVALID_FIELD_labels");
        assertRejected(c, "{\"labels\":[\"a\",1]}", "INVALID_FIELD_labels");
    }

    @Test
    void enumValuesMustBeDeclaredChoicesAndMultiAllowsALIst() {
        List<EnumValue> choices = List.of(new EnumValue("NEW", "New"), new EnumValue("GOOD", "Good"));
        ModuleContract single = contract(field("condition", FieldType.ENUM, false, null, choices));
        ModuleContract multi = contract(field("condition", FieldType.ENUM, false, new Constraints(null, null, null, null, null, true, null), choices));
        ModuleContract open = contract(field("condition", FieldType.ENUM, false, null, List.of()));

        assertAccepted(single, "{\"condition\":\"NEW\"}");
        assertRejected(single, "{\"condition\":\"BROKEN\"}", "INVALID_FIELD_condition");
        assertRejected(single, "{\"condition\":[\"NEW\"]}", "INVALID_FIELD_condition");
        assertAccepted(multi, "{\"condition\":[\"NEW\",\"GOOD\"]}");
        assertRejected(multi, "{\"condition\":[\"NEW\",\"BROKEN\"]}", "INVALID_FIELD_condition");
        assertAccepted(open, "{\"condition\":\"whatever\"}");
    }

    private static FieldContract retired(String key, boolean active, boolean deprecated) {
        return new FieldContract(key, key, FieldType.TEXT, true, false, false, false, 0, active, deprecated,
                null, List.of(), List.of(), null, null, List.of(), Map.of(), null);
    }

    @Test
    void aDeprecatedOrInactiveFieldIsNoLongerRequired() {
        ModuleContract c = contract(retired("old", true, true), retired("hidden", false, false), field("title", FieldType.TEXT, true, null, List.of()));

        assertAccepted(c, "{\"title\":\"Dune\"}");
        assertRejected(c, "{}", "MISSING_REQUIRED_FIELD_title");
    }

    @Test
    void aValueInADeprecatedFieldMustStillBeValid() {
        ModuleContract c = contract(new FieldContract("old_pages", "Old pages", FieldType.NUMBER, false, false, false, false, 0, true, true,
                null, List.of(), List.of(), null, null, List.of(), Map.of(), "pages"));

        assertAccepted(c, "{\"old_pages\":412}");
        assertRejected(c, "{\"old_pages\":\"lots\"}", "INVALID_FIELD_old_pages");
    }

    @Test
    void keysTheContractDoesNotDeclareAreLeftAlone() {
        ModuleContract c = contract(field("title", FieldType.TEXT, true, null, List.of()));

        assertAccepted(c, "{\"title\":\"Dune\",\"providerImageUrl\":\"/api/assets/x.png\",\"removed_field\":{\"a\":1}}");
    }

    @Test
    void refusesAnOversizeAttributeBlob() {
        ModuleContract c = contract(field("notes", FieldType.JSON, false, null, List.of()));
        String blob = "x".repeat(ItemAttributeValidator.MAX_ATTRIBUTES_BYTES + 1);

        assertRejected(c, "{\"notes\":\"" + blob + "\"}", "ATTRIBUTES_TOO_LARGE");
    }

    @Test
    void veryLongTextIsRefusedEvenWithoutAConstraint() {
        ModuleContract c = contract(field("notes", FieldType.TEXT, false, null, List.of()));

        assertRejected(c, "{\"notes\":\"" + "x".repeat(20_001) + "\"}", "INVALID_FIELD_notes");
    }
}
