package org.rostislav.curiokeep.providers.impl;

import org.rostislav.curiokeep.items.entities.ItemIdentifierEntity;
import org.rostislav.curiokeep.providers.AssetType;
import org.rostislav.curiokeep.providers.MetadataProvider;
import org.rostislav.curiokeep.providers.ProviderAsset;
import org.rostislav.curiokeep.providers.ProviderConfidence;
import org.rostislav.curiokeep.providers.ProviderCredential;
import org.rostislav.curiokeep.providers.ProviderCredentialField;
import org.rostislav.curiokeep.providers.ProviderCredentialLookup;
import org.rostislav.curiokeep.providers.ProviderResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TmdbProvider implements MetadataProvider {

    private static final Logger log = LoggerFactory.getLogger(TmdbProvider.class);

    private static final List<ProviderCredentialField> CREDENTIAL_FIELDS = List.of(
            ProviderCredentialField.secret("apiKey", "TMDB API key", "TMDB API key for movie lookups")
    );

    private final RestClient http;
    private final ObjectMapper objectMapper;
    private final ProviderCredentialLookup credentialLookup;

    public TmdbProvider(RestClient http, ObjectMapper objectMapper, ProviderCredentialLookup credentialLookup) {
        this.http = http;
        this.objectMapper = objectMapper;
        this.credentialLookup = credentialLookup;
    }

    @Override
    public String key() {
        return "tmdb";
    }

    @Override
    public boolean supports(ItemIdentifierEntity.IdType idType) {
        return idType == ItemIdentifierEntity.IdType.CUSTOM;
    }

    @Override
    public List<ProviderCredentialField> credentialFields() {
        return CREDENTIAL_FIELDS;
    }

    @Override
    public Optional<ProviderResult> fetch(ItemIdentifierEntity.IdType idType, String idValue) {
        Optional<ProviderCredential> stored = credentialLookup.getCredentials(key());
        if (stored.isEmpty()) {
            log.debug("tmdb apiKey not configured; skipping lookup");
            return Optional.empty();
        }
        String apiKey = stored.get().values().get("apiKey");
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("tmdb apiKey not configured; skipping lookup");
            return Optional.empty();
        }
        String id = normalizeId(idValue);
        if (id == null) return Optional.empty();

        try {
            ResponseEntity<String> response = http.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("api.themoviedb.org")
                            .path("/3/movie/" + id)
                            .queryParam("api_key", apiKey)
                            .queryParam("append_to_response", "credits,images")
                            .build())
                    .retrieve()
                    .toEntity(String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("tmdb lookup failed: status={} id={}", response.getStatusCode(), id);
                return Optional.empty();
            }

            String body = response.getBody();
            if (body == null || body.isBlank()) return Optional.empty();

            JsonNode root = objectMapper.readTree(body);
            ObjectNode normalized = objectMapper.createObjectNode();

            putText(normalized, "title", root.get("title"));
            putText(normalized, "description", root.get("overview"));
            Integer year = yearFromDate(root.get("release_date"));
            if (year != null) normalized.put("published_year", year);
            putText(normalized, "language", root.get("original_language"));
            normalized.put("tmdb_id", id);
            normalized.put("canonical_url", "https://www.themoviedb.org/movie/" + id);

            List<ProviderAsset> assets = new ArrayList<>();
            String poster = text(root.get("poster_path"));
            if (poster != null) assets.add(new ProviderAsset(AssetType.COVER, tmdbImage(poster, "original"), null, null));
            String backdrop = text(root.get("backdrop_path"));
            if (backdrop != null) assets.add(new ProviderAsset(AssetType.THUMBNAIL, tmdbImage(backdrop, "w780"), null, null));

            ProviderConfidence confidence = new ProviderConfidence(80, "TMDB movie lookup");

            return Optional.of(new ProviderResult(
                    key(),
                    Map.of("json", body),
                    Map.of("json", normalized.toString()),
                    assets,
                    confidence
            ));
        } catch (Exception e) {
            log.warn("tmdb lookup failed id={} error={}", idValue, e.getMessage());
            return Optional.empty();
        }
    }

    private URI tmdbImage(String path, String size) {
        String p = path.startsWith("/") ? path : "/" + path;
        return URI.create("https://image.tmdb.org/t/p/" + size + p);
    }

    private Integer yearFromDate(JsonNode dateNode) {
        String v = text(dateNode);
        if (v == null) return null;
        var m = java.util.regex.Pattern.compile("^(\\d{4})").matcher(v);
        if (m.find()) return Integer.parseInt(m.group(1));
        return null;
    }

    private String text(JsonNode n) {
        if (n == null || n.isNull() || n.isMissingNode()) return null;
        String v = n.asText(null);
        if (v == null) return null;
        v = v.trim();
        return v.isEmpty() ? null : v;
    }

    private void putText(ObjectNode target, String key, JsonNode value) {
        String v = text(value);
        if (v != null) target.put(key, v);
    }

    private String normalizeId(String raw) {
        if (raw == null) return null;
        String v = raw.trim();
        if (!v.matches("\\d+")) return null;
        return v;
    }
}
