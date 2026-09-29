package org.rostislav.curiokeep.modules;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.mockito.ArgumentCaptor;
import org.rostislav.curiokeep.modules.contract.FieldContract;
import org.rostislav.curiokeep.modules.contract.ModuleContract;
import org.rostislav.curiokeep.modules.contract.ModuleSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import org.xml.sax.SAXParseException;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Runs the real XSD, parser, compiler and JSON-schema validation over module XML; only the database is mocked.
 */
class ModuleLoadTxTest {

    private NamedParameterJdbcTemplate jdbc;
    private ModuleLoadTx loader;

    @BeforeEach
    void setUp() throws Exception {
        ObjectMapper objectMapper = JsonMapper.builder()
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .build();
        ModuleProps props = new ModuleProps("classpath:schema/module-schema-v1.xsd", "./data/modules-imported");
        jdbc = mock(NamedParameterJdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), any(SqlParameterSource.class), ArgumentMatchers.<Class<UUID>>any()))
                .thenReturn(UUID.randomUUID());
        loader = new ModuleLoadTx(
                new ModuleXsdValidator(props, new DefaultResourceLoader()),
                new ModuleXmlParser(),
                new ModuleCompiler(),
                new ModuleContractValidator(objectMapper),
                jdbc,
                objectMapper,
                AppVersion.of("1.4.2-SNAPSHOT")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"books", "comics"})
    void bundledModuleLoadsAndIsPersisted(String moduleKey) throws Exception {
        loader.loadOneModule(new ClassPathResource("modules/" + moduleKey + ".xml"), moduleKey + ".xml", ModuleSource.BUILTIN);

        verify(jdbc).queryForObject(anyString(), any(SqlParameterSource.class), ArgumentMatchers.<Class<UUID>>any());
        verify(jdbc, times(2)).batchUpdate(anyString(), ArgumentMatchers.<SqlParameterSource[]>any());
    }

    @Test
    void unchangedModuleIsNotWrittenAgain() throws Exception {
        when(jdbc.query(anyString(), any(SqlParameterSource.class), ArgumentMatchers.<ResultSetExtractor<UUID>>any()))
                .thenReturn(UUID.randomUUID());

        loader.loadOneModule(new ClassPathResource("modules/books.xml"), "books.xml", ModuleSource.BUILTIN);

        verify(jdbc, never()).queryForObject(anyString(), any(SqlParameterSource.class), ArgumentMatchers.<Class<UUID>>any());
        verify(jdbc, never()).batchUpdate(anyString(), ArgumentMatchers.<SqlParameterSource[]>any());
    }

    @Test
    void moduleWithoutOwnedStateIsRejected() throws Exception {
        String xml = booksXml().replace("<state key=\"OWNED\" label=\"Owned\" order=\"1\"/>", "");

        assertRejected(xml, "must define state OWNED");
    }

    @Test
    void duplicateFieldKeysAreRejectedBySchema() throws Exception {
        String xml = booksXml().replace("<field key=\"notes\"", "<field key=\"title\"");

        assertRejectedBySchema(xml, "uniq_field_key");
    }

    @Test
    void mappingToUndeclaredProviderIsRejected() throws Exception {
        String xml = booksXml().replace("<map provider=\"googlebooks\" path=\"/title\"/>", "<map provider=\"nosuchprovider\" path=\"/title\"/>");

        assertRejected(xml, "unknown provider 'nosuchprovider'");
    }

    @Test
    void mappingPathWithoutLeadingSlashIsRejectedBySchema() throws Exception {
        String xml = booksXml().replace("path=\"/subtitle\"", "path=\"subtitle\"");

        assertRejectedBySchema(xml, "JsonPointerType");
    }

    @Test
    void workflowReferencingUnknownFieldIsRejected() throws Exception {
        String xml = booksXml().replace("<step type=\"PROMPT\" field=\"title\"/>", "<step type=\"PROMPT\" field=\"ghost\"/>");

        assertRejected(xml, "unknown field 'ghost'");
    }

    @Test
    void workflowReferencingUnknownProviderIsRejected() throws Exception {
        String xml = booksXml().replace("providers=\"openlibrary,googlebooks\"", "providers=\"openlibrary,ghost\"");

        assertRejected(xml, "unknown provider 'ghost'");
    }

    @Test
    void xmlViolatingTheSchemaIsRejectedBeforeAnythingIsWritten() throws Exception {
        String xml = booksXml().replace("type=\"NUMBER\"", "type=\"NOT_A_TYPE\"");

        assertThatThrownBy(() -> load(xml)).isInstanceOf(Exception.class);
        verify(jdbc, never()).batchUpdate(anyString(), ArgumentMatchers.<SqlParameterSource[]>any());
    }

    private static final String NOTES_FIELD = "<field key=\"notes\" label=\"Notes\" type=\"TEXT\" order=\"90\"/>";

    private SqlParameterSource fieldRow(String key) {
        ArgumentCaptor<SqlParameterSource[]> batches = ArgumentCaptor.forClass(SqlParameterSource[].class);
        verify(jdbc, times(2)).batchUpdate(anyString(), batches.capture());
        SqlParameterSource[] fields = batches.getAllValues().get(1);
        return java.util.Arrays.stream(fields).filter(row -> key.equals(row.getValue("field_key"))).findFirst().orElseThrow();
    }

    @Test
    void aDeprecatedFieldNamingItsReplacementLoadsAndIsStoredAsDeprecated() throws Exception {
        load(booksXml().replace(NOTES_FIELD, "<field key=\"notes\" label=\"Notes\" type=\"TEXT\" order=\"90\" deprecated=\"true\" replacedBy=\"edition\"/>"));

        assertThat(fieldRow("notes").getValue("deprecated")).isEqualTo(true);
        assertThat(fieldRow("notes").getValue("active")).isEqualTo(true);
        assertThat(fieldRow("title").getValue("deprecated")).isEqualTo(false);
    }

    @Test
    void aFieldTheModuleMarksInactiveIsStoredAsInactive() throws Exception {
        load(booksXml().replace(NOTES_FIELD, "<field key=\"notes\" label=\"Notes\" type=\"TEXT\" order=\"90\" active=\"false\"/>"));

        assertThat(fieldRow("notes").getValue("active")).isEqualTo(false);
    }

    @Test
    void replacedByNeedsTheFieldToBeDeprecated() throws Exception {
        assertRejected(booksXml().replace(NOTES_FIELD, "<field key=\"notes\" label=\"Notes\" type=\"TEXT\" order=\"90\" replacedBy=\"edition\"/>"),
                "sets replacedBy but is not deprecated");
    }

    @Test
    void replacedByMustNameAFieldOfTheModule() throws Exception {
        assertRejected(booksXml().replace(NOTES_FIELD, "<field key=\"notes\" label=\"Notes\" type=\"TEXT\" order=\"90\" deprecated=\"true\" replacedBy=\"ghost\"/>"),
                "is replaced by unknown field 'ghost'");
    }

    @Test
    void aFieldCannotBeReplacedByItselfOrByAnotherDeprecatedField() throws Exception {
        assertRejected(booksXml().replace(NOTES_FIELD, "<field key=\"notes\" label=\"Notes\" type=\"TEXT\" order=\"90\" deprecated=\"true\" replacedBy=\"notes\"/>"),
                "must be replaced by a different field that is not itself deprecated");
        assertRejected(booksXml()
                        .replace(NOTES_FIELD, "<field key=\"notes\" label=\"Notes\" type=\"TEXT\" order=\"90\" deprecated=\"true\" replacedBy=\"printing\"/>")
                        .replace("<field key=\"printing\" label=\"Printing\" type=\"TEXT\" order=\"43\"/>", "<field key=\"printing\" label=\"Printing\" type=\"TEXT\" order=\"43\" deprecated=\"true\"/>"),
                "must be replaced by a different field that is not itself deprecated");
    }

    @Test
    void theSchemaRejectsAReplacedByThatIsNotAFieldKey() throws Exception {
        assertRejectedBySchema(booksXml().replace(NOTES_FIELD, "<field key=\"notes\" label=\"Notes\" type=\"TEXT\" order=\"90\" deprecated=\"true\" replacedBy=\"Not A Key\"/>"),
                "Value 'Not A Key' is not facet-valid");
    }

    /** Adds a default to the published_year field; a regex, because the bundled file has Windows line endings. */
    private String withYearDefault(String defaultValue) throws Exception {
        return booksXml().replaceFirst("(<field key=\"published_year\"[\\s\\S]*?</providerMappings>)", "$1<defaultValue>" + defaultValue + "</defaultValue>");
    }

    @Test
    void aDefaultDeclaredInTheXmlReachesTheCompiledContractAsTheFieldsType() throws Exception {
        ModuleContract contract = new ModuleCompiler().compile(new ModuleXmlParser().parse(withYearDefault("1999")));

        FieldContract year = contract.fields().stream().filter(f -> f.key().equals("published_year")).findFirst().orElseThrow();
        assertThat(year.defaultValue()).isEqualTo(new java.math.BigDecimal("1999"));
    }

    @Test
    void theBundledComicsPatternsAcceptTheirIdsAndRejectOthers() throws Exception {
        String xml;
        try (var in = new ClassPathResource("modules/comics.xml").getInputStream()) {
            xml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        ModuleContract contract = new ModuleCompiler().compile(new ModuleXmlParser().parse(xml));

        java.util.regex.Pattern comicVine = java.util.regex.Pattern.compile(contract.fields().stream()
                .filter(f -> f.key().equals("comicvine_id")).findFirst().orElseThrow().constraints().pattern());
        java.util.regex.Pattern metron = java.util.regex.Pattern.compile(contract.fields().stream()
                .filter(f -> f.key().equals("metron_id")).findFirst().orElseThrow().constraints().pattern());

        assertThat(comicVine.matcher("4000-12345").find()).isTrue();
        assertThat(comicVine.matcher("12345").find()).isFalse();
        assertThat(comicVine.matcher("4000-abc").find()).isFalse();
        assertThat(metron.matcher("11518").find()).isTrue();
        assertThat(metron.matcher("11518x").find()).isFalse();
    }

    @Test
    void anUnreadableDefaultIsDroppedInsteadOfBreakingTheModule() throws Exception {
        ModuleContract contract = new ModuleCompiler().compile(new ModuleXmlParser().parse(withYearDefault("not a number")));

        assertThat(contract.fields().stream().filter(f -> f.key().equals("published_year")).findFirst().orElseThrow().defaultValue()).isNull();
    }

    /** The bundled books module declares 0.0.1, so a test picks the requirement by replacing it. */
    private String requiring(String minimum) throws Exception {
        return booksXml().replace("<minAppVersion>0.0.1</minAppVersion>", "<minAppVersion>" + minimum + "</minAppVersion>");
    }

    @Test
    void aModuleNeedingANewerApplicationIsRefusedWithAClearMessage() throws Exception {
        assertThatThrownBy(() -> load(requiring("1.5.0")))
                .isInstanceOf(ModuleRequiresNewerAppException.class)
                .hasMessageContaining("needs CurioKeep 1.5.0 or newer, this is 1.4.2-SNAPSHOT");
        verify(jdbc, never()).batchUpdate(anyString(), ArgumentMatchers.<SqlParameterSource[]>any());
    }

    @Test
    void aModuleNeedingThisApplicationOrAnOlderOneLoads() throws Exception {
        load(requiring("1.4.2"));
        load(requiring("0.9.0"));
    }

    private String withMigration(String to, String steps) throws Exception {
        return booksXml().replace("</workflows>", "</workflows><migrations><migration to=\"" + to + "\">" + steps + "</migration></migrations>");
    }

    @Test
    void aModuleWithMigrationsCompilesThemOldestFirst() throws Exception {
        String xml = booksXml().replace("version=\"1.0.0\"", "version=\"2.0.0\"").replace("</workflows>",
                "</workflows><migrations>"
                        + "<migration to=\"2.0.0\"><step op=\"DROP\" field=\"legacy\"/></migration>"
                        + "<migration to=\"1.10.0\"><step op=\"MOVE\" from=\"old_notes\" to=\"notes\" transform=\"TRIM\"/></migration>"
                        + "</migrations>");

        ModuleContract contract = new ModuleCompiler().compile(new ModuleXmlParser().parse(xml));

        assertThat(contract.migrations()).extracting(m -> m.to()).containsExactly("1.10.0", "2.0.0");
        load(xml);
    }

    @Test
    void migrationStepsMustCarryTheAttributesTheirOperationNeeds() throws Exception {
        assertRejected(withMigration("1.0.0", "<step op=\"MOVE\" from=\"old\"/>"), "needs from and to");
        assertRejected(withMigration("1.0.0", "<step op=\"MAP\" field=\"binding\"/>"), "needs field and at least one mapping");
        assertRejected(withMigration("1.0.0", "<step op=\"DEFAULT\" field=\"format\"/>"), "needs field and value");
        assertRejected(withMigration("1.0.0", "<step op=\"DROP\"/>"), "needs field");
        assertRejected(withMigration("1.0.0", "<step op=\"DROP\" field=\"gone\" value=\"x\"/>"), "has attributes that DROP does not use");
    }

    @Test
    void aMigrationCannotTargetAVersionNewerThanTheModule() throws Exception {
        assertRejected(withMigration("1.1.0", "<step op=\"DROP\" field=\"gone\"/>"), "is newer than the module version 1.0.0");
    }

    @Test
    void aMigrationMustMoveIntoALiveDeclaredField() throws Exception {
        assertRejected(withMigration("1.0.0", "<step op=\"MOVE\" from=\"old\" to=\"ghost\"/>"), "names field 'ghost', which the module does not declare");
        assertRejected(withMigration("1.0.0", "<step op=\"MOVE\" from=\"notes\" to=\"notes\"/>"), "different from and to");
    }

    @Test
    void aMigrationCannotDropALiveField() throws Exception {
        assertRejected(withMigration("1.0.0", "<step op=\"DROP\" field=\"title\"/>"), "drops 'title', which is still a live field");
    }

    @Test
    void mapStepsNeedAnEnumOrTagsFieldAndDeclaredTargets() throws Exception {
        assertRejected(withMigration("1.0.0", "<step op=\"MAP\" field=\"title\"><mapping from=\"a\" to=\"b\"/></step>"), "not an ENUM or TAGS field");
        assertRejected(withMigration("1.0.0", "<step op=\"MAP\" field=\"binding\"><mapping from=\"OLD\" to=\"NOPE\"/></step>"), "'NOPE', which is not a value of 'binding'");
        assertRejected(withMigration("1.0.0", "<step op=\"MAP\" field=\"binding\"><mapping from=\"OLD\" to=\"SEWN\"/><mapping from=\"OLD\" to=\"GLUED\"/></step>"), "maps the same value twice");
    }

    @Test
    void aDefaultStepMustHoldAValueTheFieldAccepts() throws Exception {
        assertRejected(withMigration("1.0.0", "<step op=\"DEFAULT\" field=\"format\" value=\"FOLIO\"/>"), "not valid for field 'format'");
        load(withMigration("1.0.0", "<step op=\"DEFAULT\" field=\"format\" value=\"PAPERBACK\"/>"));
    }

    @Test
    void twoMigrationsToTheSameVersionAreRejectedBySchema() throws Exception {
        String xml = booksXml().replace("</workflows>", "</workflows><migrations>"
                + "<migration to=\"1.0.0\"><step op=\"DROP\" field=\"a_b\"/></migration>"
                + "<migration to=\"1.0.0\"><step op=\"DROP\" field=\"c_d\"/></migration></migrations>");

        assertRejectedBySchema(xml, "uniq_migration_target");
    }

    private void assertRejected(String xml, String expectedMessagePart) {
        assertThatThrownBy(() -> load(xml))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(expectedMessagePart);
        verify(jdbc, never()).batchUpdate(anyString(), ArgumentMatchers.<SqlParameterSource[]>any());
    }

    private void assertRejectedBySchema(String xml, String expectedMessagePart) {
        assertThatThrownBy(() -> load(xml))
                .isInstanceOf(SAXParseException.class)
                .hasMessageContaining(expectedMessagePart);
        verify(jdbc, never()).batchUpdate(anyString(), ArgumentMatchers.<SqlParameterSource[]>any());
    }

    private void load(String xml) throws Exception {
        Resource resource = new ByteArrayResource(xml.getBytes(StandardCharsets.UTF_8));
        loader.loadOneModule(resource, "test.xml", ModuleSource.IMPORTED);
    }

    private String booksXml() throws Exception {
        try (var in = new ClassPathResource("modules/books.xml").getInputStream()) {
            String xml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(xml).contains("key=\"books\"");
            return xml;
        }
    }
}
