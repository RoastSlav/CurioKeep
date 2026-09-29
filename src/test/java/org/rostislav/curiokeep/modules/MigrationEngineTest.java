package org.rostislav.curiokeep.modules;

import org.junit.jupiter.api.Test;
import org.rostislav.curiokeep.modules.contract.MigrationStep;
import org.rostislav.curiokeep.modules.contract.ModuleContract;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MigrationEngineTest {

    private final ObjectMapper json = JsonMapper.builder().build();

    /** The bundled books module plus a genres TAGS field and the given migrations, with its version raised to 3.0.0. */
    private ModuleContract books(String migrations) throws Exception {
        String xml;
        try (var in = new ClassPathResource("modules/books.xml").getInputStream()) {
            xml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        xml = xml.replace("version=\"1.0.0\"", "version=\"3.0.0\"").replace("</fields>", "<field key=\"genres\" label=\"Genres\" type=\"TAGS\" order=\"95\"/></fields>").replace("</workflows>", "</workflows><migrations>" + migrations + "</migrations>");
        return new ModuleCompiler().compile(new ModuleXmlParser().parse(xml));
    }

    private ObjectNode attrs(String text) throws Exception {
        return (ObjectNode) json.readTree(text);
    }

    private ObjectNode migrate(ModuleContract module, String from, String attributes) throws Exception {
        return MigrationEngine.apply(module, MigrationEngine.stepsSince(module, from), attrs(attributes));
    }

    private static String migration(String to, String steps) {
        return "<migration to=\"" + to + "\">" + steps + "</migration>";
    }

    @Test
    void stepsSinceReturnsOnlyMigrationsAfterTheItemsVersionUpToTheModulesVersion() throws Exception {
        ModuleContract module = books(migration("2.0.0", "<step op=\"DROP\" field=\"a_b\"/>")
                + migration("1.5.0", "<step op=\"DROP\" field=\"c_d\"/>")
                + migration("3.0.0", "<step op=\"DROP\" field=\"e_f\"/>"));

        assertThat(MigrationEngine.stepsSince(module, "1.0.0")).extracting(MigrationStep::field).containsExactly("c_d", "a_b", "e_f");
        assertThat(MigrationEngine.stepsSince(module, "1.5.0")).extracting(MigrationStep::field).containsExactly("a_b", "e_f");
        assertThat(MigrationEngine.stepsSince(module, "3.0.0")).isEmpty();
    }

    @Test
    void moveCarriesTheValueAndRemovesTheOldKey() throws Exception {
        ModuleContract module = books(migration("2.0.0", "<step op=\"MOVE\" from=\"old_notes\" to=\"notes\" transform=\"TRIM\"/>"));

        assertThat(migrate(module, "1.0.0", "{\"title\":\"Dune\",\"old_notes\":\"  first edition \"}"))
                .isEqualTo(attrs("{\"title\":\"Dune\",\"notes\":\"first edition\"}"));
    }

    @Test
    void copyKeepsTheOldKey() throws Exception {
        ModuleContract module = books(migration("2.0.0", "<step op=\"COPY\" from=\"old_notes\" to=\"notes\"/>"));

        assertThat(migrate(module, "1.0.0", "{\"old_notes\":\"kept\"}")).isEqualTo(attrs("{\"old_notes\":\"kept\",\"notes\":\"kept\"}"));
    }

    @Test
    void moveNeverOverwritesAValueTheTargetAlreadyHolds() throws Exception {
        ModuleContract module = books(migration("2.0.0", "<step op=\"MOVE\" from=\"old_notes\" to=\"notes\"/>"));

        assertThat(migrate(module, "1.0.0", "{\"old_notes\":\"old\",\"notes\":\"new\"}")).isEqualTo(attrs("{\"old_notes\":\"old\",\"notes\":\"new\"}"));
    }

    @Test
    void moveLeavesTheValueWhenTheTargetFieldWouldRejectIt() throws Exception {
        ModuleContract module = books(migration("2.0.0", "<step op=\"MOVE\" from=\"pages_text\" to=\"pages\"/>"));

        assertThat(migrate(module, "1.0.0", "{\"pages_text\":\"about 300\"}")).isEqualTo(attrs("{\"pages_text\":\"about 300\"}"));
    }

    @Test
    void moveConvertsWithATransformWhenTheTypesDiffer() throws Exception {
        ModuleContract module = books(migration("2.0.0", "<step op=\"MOVE\" from=\"pages_text\" to=\"pages\" transform=\"TO_INT\"/>"));

        assertThat(migrate(module, "1.0.0", "{\"pages_text\":\" 320 \"}")).isEqualTo(attrs("{\"pages\":320}"));
    }

    @Test
    void moveOfAnEmptySourceJustRemovesIt() throws Exception {
        ModuleContract module = books(migration("2.0.0", "<step op=\"MOVE\" from=\"old_notes\" to=\"notes\"/>"));

        assertThat(migrate(module, "1.0.0", "{\"old_notes\":\" \",\"title\":\"Dune\"}")).isEqualTo(attrs("{\"title\":\"Dune\"}"));
    }

    @Test
    void mapRenamesEnumValuesAndLeavesUnlistedOnesAlone() throws Exception {
        ModuleContract module = books(migration("2.0.0",
                "<step op=\"MAP\" field=\"binding\"><mapping from=\"PERFECT\" to=\"GLUED\"/></step>"));

        assertThat(migrate(module, "1.0.0", "{\"binding\":\"PERFECT\"}")).isEqualTo(attrs("{\"binding\":\"GLUED\"}"));
        assertThat(migrate(module, "1.0.0", "{\"binding\":\"SEWN\"}")).isEqualTo(attrs("{\"binding\":\"SEWN\"}"));
        assertThat(migrate(module, "1.0.0", "{\"title\":\"Dune\"}")).isEqualTo(attrs("{\"title\":\"Dune\"}"));
    }

    @Test
    void mapAlsoRewritesEntriesOfATagsField() throws Exception {
        ModuleContract module = books(migration("2.0.0",
                "<step op=\"MAP\" field=\"genres\"><mapping from=\"Sci-Fi\" to=\"Science fiction\"/></step>"));

        assertThat(migrate(module, "1.0.0", "{\"genres\":[\"Sci-Fi\",\"Horror\"]}")).isEqualTo(attrs("{\"genres\":[\"Science fiction\",\"Horror\"]}"));
    }

    @Test
    void defaultFillsOnlyEmptyFieldsWithATypedValue() throws Exception {
        ModuleContract module = books(migration("2.0.0",
                "<step op=\"DEFAULT\" field=\"format\" value=\"PAPERBACK\"/><step op=\"DEFAULT\" field=\"pages\" value=\"100\"/>"));

        assertThat(migrate(module, "1.0.0", "{\"title\":\"Dune\"}")).isEqualTo(attrs("{\"title\":\"Dune\",\"format\":\"PAPERBACK\",\"pages\":100}"));
        assertThat(migrate(module, "1.0.0", "{\"format\":\"HARDCOVER\",\"pages\":5}")).isEqualTo(attrs("{\"format\":\"HARDCOVER\",\"pages\":5}"));
    }

    @Test
    void dropRemovesAKeyTheModuleNoLongerDeclares() throws Exception {
        ModuleContract module = books(migration("2.0.0", "<step op=\"DROP\" field=\"legacy_notes\"/>"));

        assertThat(migrate(module, "1.0.0", "{\"legacy_notes\":\"x\",\"title\":\"Dune\"}")).isEqualTo(attrs("{\"title\":\"Dune\"}"));
    }

    @Test
    void stepsRunInOrderAcrossVersionsSoALaterStepSeesEarlierResults() throws Exception {
        ModuleContract module = books(migration("2.0.0", "<step op=\"MOVE\" from=\"old_notes\" to=\"notes\"/>")
                + migration("3.0.0", "<step op=\"DROP\" field=\"old_notes\"/><step op=\"MOVE\" from=\"notes\" to=\"edition\"/>"));

        assertThat(migrate(module, "1.0.0", "{\"old_notes\":\"2nd\"}")).isEqualTo(attrs("{\"edition\":\"2nd\"}"));
        assertThat(migrate(module, "2.0.0", "{\"notes\":\"2nd\"}")).isEqualTo(attrs("{\"edition\":\"2nd\"}"));
    }

    @Test
    void theInputAttributesAreNotChanged() throws Exception {
        ModuleContract module = books(migration("2.0.0", "<step op=\"DROP\" field=\"legacy_notes\"/>"));
        ObjectNode original = attrs("{\"legacy_notes\":\"x\"}");

        MigrationEngine.apply(module, MigrationEngine.stepsSince(module, "1.0.0"), original);

        assertThat(original).isEqualTo(attrs("{\"legacy_notes\":\"x\"}"));
        assertThat(List.of(original)).hasSize(1);
    }
}
