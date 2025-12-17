package org.rostislav.curiokeep.providers;

import org.junit.jupiter.api.Test;
import org.rostislav.curiokeep.items.entities.ItemIdentifierEntity;
import org.rostislav.curiokeep.providers.impl.IgdbProvider;
import org.rostislav.curiokeep.providers.impl.PokeApiProvider;
import org.rostislav.curiokeep.providers.impl.RawgProvider;
import org.rostislav.curiokeep.providers.impl.ScryfallProvider;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GameProvidersExtendedTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void igdbMapsGameFields() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient client = builder.build();

        String response = """
                [{"id":123,"name":"Test Game","summary":"Great game","first_release_date":1609459200,"slug":"test-game","cover":{"url":"//images.igdb.com/cover.jpg"}}]
                """;

        server.expect(requestTo("https://api.igdb.com/v4/games"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        TestProviderCredentialLookup credentials = new TestProviderCredentialLookup()
                .with("igdb", Map.of("clientId", "client123", "token", "token123"));
        IgdbProvider provider = new IgdbProvider(client, objectMapper, credentials);

        Optional<ProviderResult> result = provider.fetch(ItemIdentifierEntity.IdType.CUSTOM, "123");

        assertThat(result).isPresent();
        ProviderResult r = result.get();
        JsonNode normalized = objectMapper.readTree((String) r.normalizedFields().get("json"));
        assertThat(normalized.path("title").asText()).isEqualTo("Test Game");
        assertThat(normalized.path("description").asText()).contains("Great game");
        assertThat(normalized.path("published_year").asInt()).isEqualTo(2021);
        assertThat(normalized.path("igdb_id").asText()).isEqualTo("123");
        assertThat(normalized.path("canonical_url").asText()).isEqualTo("https://www.igdb.com/games/test-game");
        assertThat(r.assets()).hasSize(1);
    }

    @Test
    void rawgMapsGameFields() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient client = builder.build();

        String response = """
                {"id":42,"name":"Rawg Game","description_raw":"Adventure","released":"2019-07-15","slug":"rawg-game","background_image":"https://images.rawg.io/cover.jpg","background_image_additional":"https://images.rawg.io/extra.jpg"}
                """;

        server.expect(requestTo(containsString("api.rawg.io/api/games/42?key=rawg-key")))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        TestProviderCredentialLookup credentials = new TestProviderCredentialLookup()
                .with("rawg", Map.of("apiKey", "rawg-key"));
        RawgProvider provider = new RawgProvider(client, objectMapper, credentials);

        Optional<ProviderResult> result = provider.fetch(ItemIdentifierEntity.IdType.CUSTOM, "42");

        assertThat(result).isPresent();
        ProviderResult r = result.get();
        JsonNode normalized = objectMapper.readTree((String) r.normalizedFields().get("json"));
        assertThat(normalized.path("title").asText()).isEqualTo("Rawg Game");
        assertThat(normalized.path("description").asText()).isEqualTo("Adventure");
        assertThat(normalized.path("published_year").asInt()).isEqualTo(2019);
        assertThat(normalized.path("rawg_id").asText()).isEqualTo("42");
        assertThat(normalized.path("canonical_url").asText()).isEqualTo("https://rawg.io/games/rawg-game");
        assertThat(r.assets()).hasSize(2);
    }

    @Test
    void scryfallMapsCardFields() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient client = builder.build();

        String response = """
                {"id":"abcd","name":"Lotus","oracle_text":"Add three mana","released_at":"1993-08-05","uri":"https://scryfall.com/card/abcd","image_uris":{"large":"https://img.scryfall.com/large.jpg","small":"https://img.scryfall.com/small.jpg"}}
                """;

        server.expect(requestTo("https://api.scryfall.com/cards/abcd"))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        ScryfallProvider provider = new ScryfallProvider(client, objectMapper);

        Optional<ProviderResult> result = provider.fetch(ItemIdentifierEntity.IdType.CUSTOM, "abcd");

        assertThat(result).isPresent();
        ProviderResult r = result.get();
        JsonNode normalized = objectMapper.readTree((String) r.normalizedFields().get("json"));
        assertThat(normalized.path("title").asText()).isEqualTo("Lotus");
        assertThat(normalized.path("description").asText()).contains("Add three mana");
        assertThat(normalized.path("published_year").asInt()).isEqualTo(1993);
        assertThat(normalized.path("scryfall_id").asText()).isEqualTo("abcd");
        assertThat(normalized.path("canonical_url").asText()).isEqualTo("https://scryfall.com/card/abcd");
        assertThat(r.assets()).hasSize(2);
    }

    @Test
    void pokeapiMapsPokemonFields() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient client = builder.build();

        String response = """
                {"id":25,"name":"pikachu","types":[{"type":{"name":"electric"}}],"abilities":[{"ability":{"name":"static"}},{"ability":{"name":"lightning-rod"}}],"weight":60,"sprites":{"front_default":"https://pokeapi.co/front.png","back_default":"https://pokeapi.co/back.png"}}
                """;

        server.expect(requestTo("https://pokeapi.co/api/v2/pokemon/pikachu"))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        PokeApiProvider provider = new PokeApiProvider(client, objectMapper);

        Optional<ProviderResult> result = provider.fetch(ItemIdentifierEntity.IdType.CUSTOM, "pikachu");

        assertThat(result).isPresent();
        ProviderResult r = result.get();
        JsonNode normalized = objectMapper.readTree((String) r.normalizedFields().get("json"));
        assertThat(normalized.path("title").asText()).isEqualTo("pikachu");
        assertThat(normalized.path("pokedex_number").asInt()).isEqualTo(25);
        assertThat(normalized.path("canonical_url").asText()).isEqualTo("https://pokeapi.co/api/v2/pokemon/pikachu");
        assertThat(normalized.path("types").toString()).contains("electric");
        assertThat(normalized.path("abilities").toString()).contains("static");
        assertThat(normalized.path("weight").asInt()).isEqualTo(60);
        assertThat(r.assets()).hasSize(2);
    }
}
