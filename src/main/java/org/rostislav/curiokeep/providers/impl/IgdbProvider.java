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
import org.springframework.http.MediaType;
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
public class IgdbProvider implements MetadataProvider {

    private static final Logger log = LoggerFactory.getLogger(IgdbProvider.class);
    private static final String USER_AGENT = "CurioKeep/1.0 (+https://github.com/RoastSlav/CurioKeep)";

    private static final List<ProviderCredentialField> CREDENTIAL_FIELDS = List.of(
            ProviderCredentialField.text("clientId", "Client ID", "Twitch IGDB Client ID"),
            ProviderCredentialField.secret("token", "Bearer token", "IGDB bearer token")
    );

    private final RestClient http;
    private final ObjectMapper objectMapper;
    private final ProviderCredentialLookup credentialLookup;

    public IgdbProvider(RestClient http,
                        ObjectMapper objectMapper,
                        ProviderCredentialLookup credentialLookup) {
        this.http = http;
        this.objectMapper = objectMapper;
        this.credentialLookup = credentialLookup;
    }

    @Override
    public String key() {
        return "igdb";
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
            log.debug("igdb clientId/token not configured; skipping lookup");
            return Optional.empty();
        }
        Map<String, String> storedValues = stored.get().values();
        String clientId = storedValues.get("clientId");
        String bearerToken = storedValues.get("token");
        if (clientId == null || clientId.isBlank() || bearerToken == null || bearerToken.isBlank()) {
            log.debug("igdb clientId/token not configured; skipping lookup");
            return Optional.empty();
        }
        String id = normalizeId(idValue);
        if (id == null) return Optional.empty();

        try {
            String bodyRequest = "fields name, summary, first_release_date, cover.url, slug; where id = " + id + ";";

            ResponseEntity<String> response = http.post()
                    .uri("https://api.igdb.com/v4/games")
                    .header("Client-ID", clientId)
                    .header("Authorization", "Bearer " + bearerToken)
                    .header("User-Agent", USER_AGENT)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(bodyRequest)
                    .retrieve()
                    .toEntity(String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("igdb lookup failed: status={} id={}", response.getStatusCode(), id);
                return Optional.empty();
            }

            String body = response.getBody();
            if (body == null || body.isBlank()) return Optional.empty();

            JsonNode root = objectMapper.readTree(body);
            if (!root.isArray() || root.isEmpty()) return Optional.empty();
            JsonNode node = root.get(0);

            ObjectNode normalized = objectMapper.createObjectNode();
            putText(normalized, "title", node.get("name"));
            putText(normalized, "description", node.get("summary"));
            Integer year = yearFromTimestamp(node.get("first_release_date"));
            if (year != null) normalized.put("published_year", year);
            normalized.put("igdb_id", id);
            String slug = text(node.get("slug"));
            if (slug != null) {
                normalized.put("canonical_url", "https://www.igdb.com/games/" + slug);
            } else {
                normalized.put("canonical_url", "https://www.igdb.com/games/" + id);
            }

            List<ProviderAsset> assets = new ArrayList<>();
            JsonNode cover = node.get("cover");
            if (cover != null && cover.isObject()) {
                String url = text(cover.get("url"));
                if (url != null) {
                    String absolute = url.startsWith("//") ? "https:" + url : url;
                    assets.add(new ProviderAsset(AssetType.COVER, URI.create(absolute), null, null));
                }
            }

            ProviderConfidence confidence = new ProviderConfidence(80, "IGDB match");

            return Optional.of(new ProviderResult(
                    key(),
                    Map.of("json", body),
                    Map.of("json", normalized.toString()),
                    assets,
                    confidence
            ));
        } catch (Exception e) {
            log.warn("igdb lookup failed id={} error={}", idValue, e.getMessage());
            return Optional.empty();
        }
    }

    private Integer yearFromTimestamp(JsonNode ts) {
        if (ts == null || !ts.isNumber()) return null;
        long seconds = ts.asLong();
        if (seconds <= 0) return null;
        java.time.Instant instant = java.time.Instant.ofEpochSecond(seconds);
        return java.time.ZonedDateTime.ofInstant(instant, java.time.ZoneOffset.UTC).getYear();
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
