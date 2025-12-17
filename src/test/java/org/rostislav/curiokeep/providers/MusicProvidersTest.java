package org.rostislav.curiokeep.providers;

import org.junit.jupiter.api.Test;
import org.rostislav.curiokeep.items.entities.ItemIdentifierEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.rostislav.curiokeep.providers.impl.MusicBrainzProvider;
import org.rostislav.curiokeep.providers.impl.DiscogsProvider;
import org.rostislav.curiokeep.providers.impl.CoverArtArchiveProvider;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MusicProvidersTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void musicBrainzMapsReleaseAndCoverArt() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        RestClient client = builder.build();

        String releaseJson = """
                {"releases":[{"id":"mbid-123","title":"Test Release","date":"2020-08-01","country":"US","artist-credit":[{"name":"Artist A","joinphrase":", "},{"name":"Artist B"}],"label-info":[{"label":{"name":"Test Label"}}]}]}
                """;
        String coverJson = """
                {"images":[{"front":true,"image":"https://covers.example/full.jpg","thumbnails":{"large":"https://covers.example/large.jpg","small":"https://covers.example/small.jpg"}}]}
                """;

        server.expect(requestTo(containsString("musicbrainz.org/ws/2/release/")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(releaseJson, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://coverartarchive.org/release/mbid-123"))
                .andRespond(withSuccess(coverJson, MediaType.APPLICATION_JSON));

        MusicBrainzProvider provider = new MusicBrainzProvider(client, objectMapper);

        Optional<ProviderResult> result = provider.fetch(ItemIdentifierEntity.IdType.UPC, "012345678905");

        assertThat(result).isPresent();
        ProviderResult r = result.get();
        JsonNode normalized = objectMapper.readTree((String) r.normalizedFields().get("json"));
        assertThat(normalized.path("title").asText()).isEqualTo("Test Release");
        assertThat(normalized.path("artists").asText()).isEqualTo("Artist A, Artist B");
        assertThat(normalized.path("publisher").asText()).isEqualTo("Test Label");
        assertThat(normalized.path("published_year").asInt()).isEqualTo(2020);
        assertThat(normalized.path("upc").asText()).isEqualTo("012345678905");
        assertThat(r.assets()).hasSize(2);
    }

    @Test
    void discogsUsesBarcodeAndToken() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient client = builder.build();

        String discogsJson = """
                {"results":[{"id":321,"title":"Artist A - Test Release","year":2020,"country":"US","label":["Test Label"],"cover_image":"https://images.discogs.com/full.jpg","thumb":"https://images.discogs.com/thumb.jpg","uri":"https://www.discogs.com/release/321"}]}
                """;

        server.expect(requestTo(containsString("api.discogs.com/database/search")))
                .andRespond(withSuccess(discogsJson, MediaType.APPLICATION_JSON));

        DiscogsProvider provider = new DiscogsProvider(client, objectMapper, "token123");

        Optional<ProviderResult> result = provider.fetch(ItemIdentifierEntity.IdType.UPC, "012345678905");

        assertThat(result).isPresent();
        ProviderResult r = result.get();
        JsonNode normalized = objectMapper.readTree((String) r.normalizedFields().get("json"));
        assertThat(normalized.path("title").asText()).isEqualTo("Artist A - Test Release");
        assertThat(normalized.path("publisher").asText()).isEqualTo("Test Label");
        assertThat(normalized.path("published_year").asInt()).isEqualTo(2020);
        assertThat(normalized.path("upc").asText()).isEqualTo("012345678905");
        assertThat(normalized.path("discogs_id").asInt()).isEqualTo(321);
        assertThat(r.assets()).isNotEmpty();
    }

        @Test
        void coverArtArchiveUsesMbidAndReturnsAssets() throws Exception {
                RestClient.Builder builder = RestClient.builder();
                MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
                RestClient client = builder.build();

                String mbid = "12345678-1234-1234-1234-123456789012";
                String coverJson = """
                                {"images":[{"front":true,"image":"https://covers.example/full.jpg","thumbnails":{"large":"https://covers.example/large.jpg"}}]}
                                """;

                server.expect(requestTo("https://coverartarchive.org/release/" + mbid))
                                .andRespond(withSuccess(coverJson, MediaType.APPLICATION_JSON));

                CoverArtArchiveProvider provider = new CoverArtArchiveProvider(client, objectMapper);

                Optional<ProviderResult> result = provider.fetch(ItemIdentifierEntity.IdType.CUSTOM, mbid);

                assertThat(result).isPresent();
                ProviderResult r = result.get();
                JsonNode normalized = objectMapper.readTree((String) r.normalizedFields().get("json"));
                assertThat(normalized.path("musicbrainz_id").asText()).isEqualTo(mbid);
                assertThat(normalized.path("canonical_url").asText()).isEqualTo("https://musicbrainz.org/release/" + mbid);
                assertThat(r.assets()).hasSize(2);
        }
}
