package org.rostislav.curiokeep.providers;

import org.junit.jupiter.api.Test;
import org.rostislav.curiokeep.items.entities.ItemIdentifierEntity;
import org.rostislav.curiokeep.modules.entities.ModuleDefinitionEntity;
import org.rostislav.curiokeep.modules.entities.ModuleFieldEntity;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.rostislav.curiokeep.providers.api.dto.LookupResponse;

class ProviderLookupServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsesNormalizedJsonStringsAndMergesAttributes() throws Exception {
    ModuleDefinitionEntity module = new ModuleDefinitionEntity();
    module.setDefinitionJson("""
        {"providers":[{"key":"openlibrary","priority":10,"enabled":true},{"key":"googlebooks","priority":20,"enabled":true}]}
        """);

    ModuleFieldEntity title = field("title", mappingMap("openlibrary", "/title"), mappingMap("googlebooks", "/title"));
    ModuleFieldEntity subtitle = field("subtitle", mappingMap("openlibrary", "/subtitle"), mappingMap("googlebooks", "/subtitle"));
    ModuleFieldEntity publisher = field("publisher", mappingMap("openlibrary", "/publisher"), mappingMap("googlebooks", "/publisher"));
    ModuleFieldEntity isbn13 = field("isbn13", mappingMap("openlibrary", "/isbn13"), mappingMap("googlebooks", "/isbn13"));
    module.setFields(List.of(title, subtitle, publisher, isbn13));

    ItemIdentifierEntity id = new ItemIdentifierEntity();
    id.setIdType(ItemIdentifierEntity.IdType.ISBN13);
    id.setIdValue("9780261103573");

    ProviderResult openLibrary = new ProviderResult(
        "openlibrary",
        Map.of("json", "{}"),
        Map.of("json", """
            {"title":"The Fellowship of the Ring","subtitle":"Being the First Part of the Lord of the Rings","publisher":"HarperCollins Publishers","published_year":1997,"pages":416,"language":"eng","isbn13":"9780261103573"}
            """),
        List.of(
            new ProviderAsset(AssetType.COVER, URI.create("https://covers.openlibrary.org/b/isbn/9780261103573-L.jpg"), null, null),
            new ProviderAsset(AssetType.THUMBNAIL, URI.create("https://covers.openlibrary.org/b/isbn/9780261103573-M.jpg"), null, null)
        ),
        new ProviderConfidence(80, "OpenLibrary ISBN match")
    );

    ProviderResult google = new ProviderResult(
        "googlebooks",
        Map.of("json", "{}"),
        Map.of("json", """
            {"title":"The lord of the rings","subtitle":"The fellowship of the ring","publisher":"Google Pub","published_year":1997,"pages":398,"language":"en","isbn13":"9780261103573"}
            """),
        List.of(
            new ProviderAsset(AssetType.COVER, URI.create("https://covers.openlibrary.org/b/isbn/9780261103573-L.jpg"), null, null),
            new ProviderAsset(AssetType.THUMBNAIL, URI.create("http://books.google.com/books/content?id=L5ABrgEACAAJ&printsec=frontcover&img=1&zoom=5&source=gbs_api"), null, null)
        ),
        new ProviderConfidence(90, "GoogleBooks ISBN match")
    );

    ProviderLookupService service = new ProviderLookupService(
        new ProviderRegistry(List.of(new StubMetadataProvider(openLibrary), new StubMetadataProvider(google))),
        new ProviderFieldMapper(objectMapper),
        objectMapper
    );

    LookupResponse resp = service.lookup(module, List.of(id));

    assertThat(resp.best().providerKey()).isEqualTo("googlebooks");
    assertThat(resp.assets())
        .hasSize(3)
        .extracting(ProviderAsset::url)
        .containsExactly(
            URI.create("https://covers.openlibrary.org/b/isbn/9780261103573-L.jpg"),
            URI.create("https://covers.openlibrary.org/b/isbn/9780261103573-M.jpg"),
            URI.create("http://books.google.com/books/content?id=L5ABrgEACAAJ&printsec=frontcover&img=1&zoom=5&source=gbs_api")
        );

    assertThat(resp.mergedAttributes())
        .containsEntry("title", "The Fellowship of the Ring")
        .containsEntry("subtitle", "Being the First Part of the Lord of the Rings")
        .containsEntry("publisher", "HarperCollins Publishers")
        .containsEntry("isbn13", "9780261103573");
    }

    @Test
    void aChainDeclaredByTheModuleBringsInTheRecordAnotherProviderReferences() throws Exception {
        ModuleDefinitionEntity module = new ModuleDefinitionEntity();
        module.setDefinitionJson("""
            {"providers":[
              {"key":"metron","priority":1,"enabled":true,"chains":[{"from":"comicvine_id","to":"comicvine","idType":"CUSTOM"}]},
              {"key":"comicvine","priority":2,"enabled":true}]}
            """);
        module.setFields(List.of(field("title", mappingMap("metron", "/title"), mappingMap("comicvine", "/title")),
                field("description", mappingMap("metron", "/description"), mappingMap("comicvine", "/description"))));
        ItemIdentifierEntity upc = new ItemIdentifierEntity();
        upc.setIdType(ItemIdentifierEntity.IdType.UPC);
        upc.setIdValue("75960608");

        List<String> comicVineAsked = new java.util.ArrayList<>();
        MetadataProvider metron = new MetadataProvider() {
            public String key() { return "metron"; }
            public boolean supports(ItemIdentifierEntity.IdType idType) { return idType == ItemIdentifierEntity.IdType.UPC; }
            public Optional<ProviderResult> fetch(ItemIdentifierEntity.IdType idType, String idValue) {
                return Optional.of(new ProviderResult("metron", Map.of(), Map.of("title", "Hulk", "comicvine_id", "4000-77"), List.of(), new ProviderConfidence(80, "upc")));
            }
        };
        MetadataProvider comicvine = new MetadataProvider() {
            public String key() { return "comicvine"; }
            public boolean supports(ItemIdentifierEntity.IdType idType) { return idType == ItemIdentifierEntity.IdType.CUSTOM; }
            public Optional<ProviderResult> fetch(ItemIdentifierEntity.IdType idType, String idValue) {
                comicVineAsked.add(idValue);
                return Optional.of(new ProviderResult("comicvine", Map.of(), Map.of("title", "Incredible Hulk", "description", "Smash"), List.of(), new ProviderConfidence(95, "id")));
            }
        };
        ProviderLookupService service = new ProviderLookupService(new ProviderRegistry(List.of(metron, comicvine)), new ProviderFieldMapper(objectMapper), objectMapper);

        LookupResponse response = service.lookup(module, List.of(upc));

        assertThat(comicVineAsked).containsExactly("4000-77");
        assertThat(response.results()).extracting(ProviderResult::providerKey).containsExactly("metron", "comicvine");
        assertThat(response.mergedAttributes()).containsEntry("title", "Hulk").containsEntry("description", "Smash");
    }

    @Test
    void withoutAChainNothingIsLookedUpInTheOtherProvider() throws Exception {
        ModuleDefinitionEntity module = new ModuleDefinitionEntity();
        module.setDefinitionJson("""
            {"providers":[{"key":"metron","priority":1,"enabled":true},{"key":"comicvine","priority":2,"enabled":true}]}
            """);
        module.setFields(List.of());
        ItemIdentifierEntity upc = new ItemIdentifierEntity();
        upc.setIdType(ItemIdentifierEntity.IdType.UPC);
        upc.setIdValue("75960608");
        List<String> comicVineAsked = new java.util.ArrayList<>();
        MetadataProvider metron = new StubMetadataProvider(new ProviderResult("metron", Map.of(), Map.of("comicvine_id", "4000-77"), List.of(), null));
        MetadataProvider comicvine = new MetadataProvider() {
            public String key() { return "comicvine"; }
            public boolean supports(ItemIdentifierEntity.IdType idType) { return idType == ItemIdentifierEntity.IdType.CUSTOM; }
            public Optional<ProviderResult> fetch(ItemIdentifierEntity.IdType idType, String idValue) {
                comicVineAsked.add(idValue);
                return Optional.empty();
            }
        };

        new ProviderLookupService(new ProviderRegistry(List.of(metron, comicvine)), new ProviderFieldMapper(objectMapper), objectMapper)
                .lookup(module, List.of(upc));

        assertThat(comicVineAsked).isEmpty();
    }

    private ModuleFieldEntity field(String key, Map<String, String>... mappings) throws Exception {
    ModuleFieldEntity f = new ModuleFieldEntity();
    f.setFieldKey(key);
    f.setLabel(key);
    f.setFieldType(ModuleFieldEntity.FieldType.TEXT);
    f.setProviderMappings(objectMapper.writeValueAsString(List.of(mappings)));
    return f;
    }

    private Map<String, String> mappingMap(String provider, String path) {
    return Map.of("provider", provider, "path", path);
    }

}
