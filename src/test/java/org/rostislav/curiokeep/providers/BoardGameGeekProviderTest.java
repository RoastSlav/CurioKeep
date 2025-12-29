package org.rostislav.curiokeep.providers;

import org.junit.jupiter.api.Test;
import org.rostislav.curiokeep.items.entities.ItemIdentifierEntity;
import org.rostislav.curiokeep.providers.impl.BoardGameGeekProvider;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BoardGameGeekProviderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mapsBoardGameFieldsFromXml() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient client = builder.build();

        String xml = """
                <items totalitems=\"1\">
                  <item id=\"174430\" type=\"boardgame\">
                    <thumbnail>https://example.com/thumb.jpg</thumbnail>
                    <image>https://example.com/image.jpg</image>
                    <name type=\"primary\" value=\"Terraforming Mars\" />
                    <yearpublished value=\"2016\" />
                    <minplayers value=\"1\" />
                    <maxplayers value=\"5\" />
                    <playingtime value=\"120\" />
                    <minage value=\"12\" />
                    <link type=\"boardgamepublisher\" id=\"123\" value=\"Stronghold Games\" />
                  </item>
                </items>
                """;

        String bearerToken = "bgg-test-token";
        server.expect(requestTo("https://boardgamegeek.com/xmlapi2/thing?id=174430&stats=1"))
            .andExpect(header("Authorization", "Bearer " + bearerToken))
            .andRespond(withSuccess(xml, MediaType.APPLICATION_XML));

        TestProviderCredentialLookup credentials = new TestProviderCredentialLookup()
            .with("boardgamegeek", Map.of("token", bearerToken));

        BoardGameGeekProvider provider = new BoardGameGeekProvider(client, objectMapper, credentials);

        Optional<ProviderResult> result = provider.fetch(ItemIdentifierEntity.IdType.CUSTOM, "174430");

        assertThat(result).isPresent();
        ProviderResult r = result.get();
        JsonNode normalized = objectMapper.readTree((String) r.normalizedFields().get("json"));
        assertThat(normalized.path("title").asText()).isEqualTo("Terraforming Mars");
        assertThat(normalized.path("published_year").asInt()).isEqualTo(2016);
        assertThat(normalized.path("publisher").asText()).isEqualTo("Stronghold Games");
        assertThat(normalized.path("bgg_id").asText()).isEqualTo("174430");
        assertThat(normalized.path("canonical_url").asText()).isEqualTo("https://boardgamegeek.com/boardgame/174430");
        assertThat(normalized.path("min_players").asInt()).isEqualTo(1);
        assertThat(normalized.path("max_players").asInt()).isEqualTo(5);
        assertThat(normalized.path("playing_time").asInt()).isEqualTo(120);
        assertThat(normalized.path("min_age").asInt()).isEqualTo(12);
        assertThat(r.assets()).hasSize(2);
    }
}
