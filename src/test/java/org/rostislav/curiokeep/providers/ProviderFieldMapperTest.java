package org.rostislav.curiokeep.providers;

import org.junit.jupiter.api.Test;
import org.rostislav.curiokeep.modules.entities.ModuleFieldEntity;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderFieldMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ProviderFieldMapper mapper = new ProviderFieldMapper(objectMapper);

    @Test
    void mapsValueAtJsonPointerForMatchingProvider() throws Exception {
        JsonNode payload = objectMapper.readTree("{\"title\":\"Dune\",\"pages\":412}");

        Map<String, Object> result = mapper.mapFields(payload, List.of(
                field("title", mapping("openlibrary", "/title", null)),
                field("pages", mapping("openlibrary", "/pages", null))
        ), "openlibrary");

        assertThat(result).containsEntry("title", "Dune").containsEntry("pages", 412);
    }

    @Test
    void ignoresMappingsDeclaredForOtherProviders() throws Exception {
        JsonNode payload = objectMapper.readTree("{\"title\":\"Dune\"}");

        Map<String, Object> result = mapper.mapFields(payload, List.of(
                field("title", mapping("googlebooks", "/title", null))
        ), "openlibrary");

        assertThat(result).isEmpty();
    }

    @Test
    void trimTransformStripsSurroundingWhitespace() throws Exception {
        JsonNode payload = objectMapper.readTree("{\"publisher\":\"  Ace Books \\n\"}");

        Map<String, Object> result = mapper.mapFields(payload, List.of(
                field("publisher", mapping("openlibrary", "/publisher", "TRIM"))
        ), "openlibrary");

        assertThat(result).containsEntry("publisher", "Ace Books");
    }

    @Test
    void trimTransformSkipsBlankValueSoNextMappingCanFillTheField() throws Exception {
        JsonNode payload = objectMapper.readTree("{\"publisher\":\"   \",\"imprint\":\"Tor\"}");

        Map<String, Object> result = mapper.mapFields(payload, List.of(
                field("publisher",
                        mapping("openlibrary", "/publisher", "TRIM"),
                        mapping("openlibrary", "/imprint", null))
        ), "openlibrary");

        assertThat(result).containsEntry("publisher", "Tor");
    }

    @Test
    void joinCommaTransformJoinsArrayElementsAndDropsBlanks() throws Exception {
        JsonNode payload = objectMapper.readTree("{\"authors\":[\"Frank Herbert\",\"\",\" Brian Herbert \"]}");

        Map<String, Object> result = mapper.mapFields(payload, List.of(
                field("authors", mapping("openlibrary", "/authors", "JOIN_COMMA"))
        ), "openlibrary");

        assertThat(result).containsEntry("authors", "Frank Herbert, Brian Herbert");
    }

    @Test
    void joinCommaTransformOnEmptyArrayLeavesFieldUnset() throws Exception {
        JsonNode payload = objectMapper.readTree("{\"authors\":[]}");

        Map<String, Object> result = mapper.mapFields(payload, List.of(
                field("authors", mapping("openlibrary", "/authors", "JOIN_COMMA"))
        ), "openlibrary");

        assertThat(result).doesNotContainKey("authors");
    }

    @Test
    void joinCommaTransformLeavesScalarValueUnchanged() throws Exception {
        JsonNode payload = objectMapper.readTree("{\"authors\":\"Frank Herbert\"}");

        Map<String, Object> result = mapper.mapFields(payload, List.of(
                field("authors", mapping("openlibrary", "/authors", "JOIN_COMMA"))
        ), "openlibrary");

        assertThat(result).containsEntry("authors", "Frank Herbert");
    }

    @Test
    void firstTransformTakesTheFirstNonBlankArrayElementKeepingItsType() throws Exception {
        JsonNode payload = objectMapper.readTree("{\"names\":[\"\",\"Frank\",\"Brian\"],\"years\":[1965,1969]}");

        Map<String, Object> result = mapper.mapFields(payload, List.of(
                field("name", mapping("openlibrary", "/names", "FIRST")),
                field("year", mapping("openlibrary", "/years", "FIRST"))
        ), "openlibrary");

        assertThat(result).containsEntry("name", "Frank").containsEntry("year", 1965);
    }

    @Test
    void firstTransformOnEmptyArrayLeavesFieldUnset() throws Exception {
        JsonNode payload = objectMapper.readTree("{\"names\":[]}");

        Map<String, Object> result = mapper.mapFields(payload, List.of(
                field("name", mapping("openlibrary", "/names", "FIRST"))
        ), "openlibrary");

        assertThat(result).isEmpty();
    }

    @Test
    void toIntTransformParsesNumericTextAndTruncatesNumbers() throws Exception {
        JsonNode payload = objectMapper.readTree("{\"pages\":\" 412 \",\"year\":1965.0}");

        Map<String, Object> result = mapper.mapFields(payload, List.of(
                field("pages", mapping("openlibrary", "/pages", "TO_INT")),
                field("year", mapping("openlibrary", "/year", "TO_INT"))
        ), "openlibrary");

        assertThat(result).containsEntry("pages", 412).containsEntry("year", 1965);
    }

    @Test
    void toIntTransformSkipsTextThatIsNotANumber() throws Exception {
        JsonNode payload = objectMapper.readTree("{\"pages\":\"about 400\",\"alt\":\"399\"}");

        Map<String, Object> result = mapper.mapFields(payload, List.of(
                field("pages",
                        mapping("openlibrary", "/pages", "TO_INT"),
                        mapping("openlibrary", "/alt", "TO_INT"))
        ), "openlibrary");

        assertThat(result).containsEntry("pages", 399);
    }

    @Test
    void unknownTransformLeavesTheValueUnchanged() throws Exception {
        JsonNode payload = objectMapper.readTree("{\"title\":\" Dune \"}");

        Map<String, Object> result = mapper.mapFields(payload, List.of(
                field("title", mapping("openlibrary", "/title", "SHOUT"))
        ), "openlibrary");

        assertThat(result).containsEntry("title", " Dune ");
    }

    @Test
    void arrayWithoutTransformIsKeptAsRawJsonText() throws Exception {
        JsonNode payload = objectMapper.readTree("{\"authors\":[\"A\",\"B\"]}");

        Map<String, Object> result = mapper.mapFields(payload, List.of(
                field("authors", mapping("openlibrary", "/authors", null))
        ), "openlibrary");

        assertThat(result).containsEntry("authors", "[\"A\",\"B\"]");
    }

    @Test
    void missingPathLeavesFieldUnset() throws Exception {
        JsonNode payload = objectMapper.readTree("{\"title\":\"Dune\"}");

        Map<String, Object> result = mapper.mapFields(payload, List.of(
                field("subtitle", mapping("openlibrary", "/subtitle", null))
        ), "openlibrary");

        assertThat(result).isEmpty();
    }

    private Map<String, String> mapping(String provider, String path, String transform) {
        return transform == null
                ? Map.of("provider", provider, "path", path)
                : Map.of("provider", provider, "path", path, "transform", transform);
    }

    @SafeVarargs
    private ModuleFieldEntity field(String key, Map<String, String>... mappings) throws Exception {
        ModuleFieldEntity f = new ModuleFieldEntity();
        f.setFieldKey(key);
        f.setLabel(key);
        f.setFieldType(ModuleFieldEntity.FieldType.TEXT);
        f.setProviderMappings(objectMapper.writeValueAsString(List.of(mappings)));
        return f;
    }
}
