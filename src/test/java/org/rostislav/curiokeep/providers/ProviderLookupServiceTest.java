package org.rostislav.curiokeep.providers;

import org.junit.jupiter.api.Test;
import org.rostislav.curiokeep.items.entities.ItemIdentifierEntity;
import org.rostislav.curiokeep.modules.entities.ModuleDefinitionEntity;
import org.rostislav.curiokeep.modules.entities.ModuleFieldEntity;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.List;
import java.util.Map;

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
        new ProviderRegistry(List.of(new StubProvider(openLibrary), new StubProvider(google))),
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

    private record StubProvider(ProviderResult result) implements MetadataProvider {
    @Override
    public String key() {
        return result.providerKey();
    }

    @Override
    public boolean supports(ItemIdentifierEntity.IdType idType) {
        return true;
    }

    @Override
    public java.util.Optional<ProviderResult> fetch(ItemIdentifierEntity.IdType idType, String idValue) {
        return java.util.Optional.of(result);
    }
    }
}
