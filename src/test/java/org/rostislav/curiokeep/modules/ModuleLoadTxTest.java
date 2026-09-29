package org.rostislav.curiokeep.modules;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
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
                objectMapper
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
