package org.rostislav.curiokeep.providers;

import org.junit.jupiter.api.Test;
import org.rostislav.curiokeep.items.entities.ItemIdentifierEntity;
import org.rostislav.curiokeep.providers.impl.BricksetProvider;
import org.rostislav.curiokeep.providers.impl.RebrickableProvider;
import org.rostislav.curiokeep.providers.impl.InternetArchiveProvider;
import org.rostislav.curiokeep.providers.impl.OpenProductDataProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class LegoAndArchiveProvidersTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void bricksetMapsSetFields() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient client = builder.build();

        String response = """
                [{"name":"Space Cruiser","setNumber":"928-1","year":1979,"pieces":338,"minifigs":2,"image":"https://img.brickset.com/sets/928-1.jpg"}]
                """;

        server.expect(requestTo(containsString("brickset.com/api/v3.asmx/getSet")))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        TestProviderCredentialLookup credentials = new TestProviderCredentialLookup()
                .with("brickset", Map.of("apiKey", "brick-key"));
        BricksetProvider provider = new BricksetProvider(client, objectMapper, credentials);

        Optional<ProviderResult> result = provider.fetch(ItemIdentifierEntity.IdType.CUSTOM, "9999");

        assertThat(result).isPresent();
        ProviderResult r = result.get();
        JsonNode normalized = objectMapper.readTree((String) r.normalizedFields().get("json"));
        assertThat(normalized.path("title").asText()).isEqualTo("Space Cruiser");
        assertThat(normalized.path("set_number").asText()).isEqualTo("928-1");
        assertThat(normalized.path("published_year").asInt()).isEqualTo(1979);
        assertThat(normalized.path("pieces").asInt()).isEqualTo(338);
        assertThat(normalized.path("minifigs").asInt()).isEqualTo(2);
        assertThat(normalized.path("brickset_id").asText()).isEqualTo("9999");
        assertThat(normalized.path("canonical_url").asText()).isEqualTo("https://brickset.com/sets/928-1");
        assertThat(r.assets()).hasSize(1);
    }

    @Test
    void rebrickableMapsSetFields() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient client = builder.build();

        String response = """
                {"name":"Galaxy Explorer","set_num":"497-1","year":1979,"num_parts":338,"set_img_url":"https://rebrickable.com/497-1.jpg"}
                """;

        server.expect(requestTo("https://rebrickable.com/api/v3/lego/sets/497-1/"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "key rebrick-key"))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        TestProviderCredentialLookup credentials = new TestProviderCredentialLookup()
                .with("rebrickable", Map.of("apiKey", "rebrick-key"));
        RebrickableProvider provider = new RebrickableProvider(client, objectMapper, credentials);

        Optional<ProviderResult> result = provider.fetch(ItemIdentifierEntity.IdType.CUSTOM, "497-1");

        assertThat(result).isPresent();
        ProviderResult r = result.get();
        JsonNode normalized = objectMapper.readTree((String) r.normalizedFields().get("json"));
        assertThat(normalized.path("title").asText()).isEqualTo("Galaxy Explorer");
        assertThat(normalized.path("set_number").asText()).isEqualTo("497-1");
        assertThat(normalized.path("published_year").asInt()).isEqualTo(1979);
        assertThat(normalized.path("pieces").asInt()).isEqualTo(338);
        assertThat(normalized.path("rebrickable_id").asText()).isEqualTo("497-1");
        assertThat(normalized.path("canonical_url").asText()).isEqualTo("https://rebrickable.com/sets/497-1");
        assertThat(r.assets()).hasSize(1);
    }

    @Test
    void internetArchiveMapsFields() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient client = builder.build();

        String response = """
                {"metadata":{"title":"Archive Item","description":"Public domain","date":"2012-05-01","language":"eng"},"files":[{"name":"thumb.jpg"},{"name":"doc.pdf"}]}
                """;

        server.expect(requestTo("https://archive.org/metadata/sample-item"))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        InternetArchiveProvider provider = new InternetArchiveProvider(client, objectMapper);

        Optional<ProviderResult> result = provider.fetch(ItemIdentifierEntity.IdType.CUSTOM, "sample-item");

        assertThat(result).isPresent();
        ProviderResult r = result.get();
        JsonNode normalized = objectMapper.readTree((String) r.normalizedFields().get("json"));
        assertThat(normalized.path("title").asText()).isEqualTo("Archive Item");
        assertThat(normalized.path("description").asText()).contains("Public domain");
        assertThat(normalized.path("published_year").asInt()).isEqualTo(2012);
        assertThat(normalized.path("language").asText()).isEqualTo("eng");
        assertThat(normalized.path("internet_archive_id").asText()).isEqualTo("sample-item");
        assertThat(normalized.path("canonical_url").asText()).isEqualTo("https://archive.org/details/sample-item");
        assertThat(r.assets()).hasSize(1);
    }

    @Test
    void openProductDataMapsFields() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient client = builder.build();

        String response = """
                {"product":{"product_name":"Test Snack","brands":"SnackCorp","url":"https://openfoodfacts.org/product/012345678905","categories":"Snacks","ingredients_text":"sugar, salt","image_front_url":"https://images.off/front.jpg","image_thumb_url":"https://images.off/thumb.jpg"}}
                """;

        server.expect(requestTo("https://world.openfoodfacts.org/api/v2/product/012345678905"))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        OpenProductDataProvider provider = new OpenProductDataProvider(client, objectMapper);

        Optional<ProviderResult> result = provider.fetch(ItemIdentifierEntity.IdType.UPC, "012345678905");

        assertThat(result).isPresent();
        ProviderResult r = result.get();
        JsonNode normalized = objectMapper.readTree((String) r.normalizedFields().get("json"));
        assertThat(normalized.path("title").asText()).isEqualTo("Test Snack");
        assertThat(normalized.path("brand").asText()).isEqualTo("SnackCorp");
        assertThat(normalized.path("gtin").asText()).isEqualTo("012345678905");
        assertThat(normalized.path("canonical_url").asText()).isEqualTo("https://openfoodfacts.org/product/012345678905");
        assertThat(normalized.path("categories").asText()).contains("Snacks");
        assertThat(normalized.path("ingredients").asText()).contains("sugar");
        assertThat(r.assets()).hasSize(2);
    }
}
