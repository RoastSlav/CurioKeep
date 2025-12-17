package org.rostislav.curiokeep.providers;

import org.junit.jupiter.api.Test;
import org.rostislav.curiokeep.items.entities.ItemIdentifierEntity;
import org.rostislav.curiokeep.providers.impl.AniListProvider;
import org.rostislav.curiokeep.providers.impl.TmdbProvider;
import org.rostislav.curiokeep.providers.impl.TvMazeProvider;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ScreenProvidersTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void tmdbMapsMovieFields() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient client = builder.build();

        String movieJson = """
                {"id":550,"title":"Fight Club","overview":"An insomniac...","release_date":"1999-10-15","original_language":"en","poster_path":"/poster.jpg","backdrop_path":"/backdrop.jpg"}
                """;

        server.expect(requestTo("https://api.themoviedb.org/3/movie/550?api_key=token123&append_to_response=credits,images"))
                .andRespond(withSuccess(movieJson, MediaType.APPLICATION_JSON));

        TmdbProvider provider = new TmdbProvider(client, objectMapper, "token123");

        Optional<ProviderResult> result = provider.fetch(ItemIdentifierEntity.IdType.CUSTOM, "550");

        assertThat(result).isPresent();
        ProviderResult r = result.get();
        JsonNode normalized = objectMapper.readTree((String) r.normalizedFields().get("json"));
        assertThat(normalized.path("title").asText()).isEqualTo("Fight Club");
        assertThat(normalized.path("published_year").asInt()).isEqualTo(1999);
        assertThat(normalized.path("language").asText()).isEqualTo("en");
        assertThat(normalized.path("tmdb_id").asText()).isEqualTo("550");
        assertThat(normalized.path("canonical_url").asText()).isEqualTo("https://www.themoviedb.org/movie/550");
        assertThat(r.assets()).hasSize(2);
    }

    @Test
    void tvMazeMapsShowFields() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient client = builder.build();

        String showJson = """
                {"id":123,"name":"Test Show","summary":"<p>Great show</p>","premiered":"2010-01-01","language":"en","image":{"original":"https://images.tvmaze.com/original.jpg","medium":"https://images.tvmaze.com/medium.jpg"}}
                """;

        server.expect(requestTo("https://api.tvmaze.com/shows/123?embed=episodes,cast"))
                .andRespond(withSuccess(showJson, MediaType.APPLICATION_JSON));

        TvMazeProvider provider = new TvMazeProvider(client, objectMapper);

        Optional<ProviderResult> result = provider.fetch(ItemIdentifierEntity.IdType.CUSTOM, "123");

        assertThat(result).isPresent();
        ProviderResult r = result.get();
        JsonNode normalized = objectMapper.readTree((String) r.normalizedFields().get("json"));
        assertThat(normalized.path("title").asText()).isEqualTo("Test Show");
        assertThat(normalized.path("published_year").asInt()).isEqualTo(2010);
        assertThat(normalized.path("language").asText()).isEqualTo("en");
        assertThat(normalized.path("tvmaze_id").asText()).isEqualTo("123");
        assertThat(normalized.path("canonical_url").asText()).isEqualTo("https://www.tvmaze.com/shows/123");
        assertThat(r.assets()).hasSize(2);
    }

    @Test
    void anilistMapsAnimeFields() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient client = builder.build();

        String gqlResponse = """
                {"data":{"Media":{"id":42,"title":{"romaji":"Test Anime","english":"Test Anime EN"},"description":"A hero...","seasonYear":2021,"coverImage":{"extraLarge":"https://img.xl.jpg","large":"https://img.large.jpg","medium":"https://img.medium.jpg"},"siteUrl":"https://anilist.co/anime/42"}}}
                """;

        server.expect(requestTo("https://graphql.anilist.co"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(gqlResponse, MediaType.APPLICATION_JSON));

        AniListProvider provider = new AniListProvider(client, objectMapper);

        Optional<ProviderResult> result = provider.fetch(ItemIdentifierEntity.IdType.CUSTOM, "42");

        assertThat(result).isPresent();
        ProviderResult r = result.get();
        JsonNode normalized = objectMapper.readTree((String) r.normalizedFields().get("json"));
        assertThat(normalized.path("title").asText()).isEqualTo("Test Anime EN");
        assertThat(normalized.path("published_year").asInt()).isEqualTo(2021);
        assertThat(normalized.path("anilist_id").asText()).isEqualTo("42");
        assertThat(normalized.path("canonical_url").asText()).isEqualTo("https://anilist.co/anime/42");
        assertThat(r.assets()).hasSize(3);
    }
}
