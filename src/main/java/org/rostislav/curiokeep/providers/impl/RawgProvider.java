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
public class RawgProvider implements MetadataProvider {

    private static final Logger log = LoggerFactory.getLogger(RawgProvider.class);
    private static final String USER_AGENT = "CurioKeep/1.0 (+https://github.com/RoastSlav/CurioKeep)";

    private static final List<ProviderCredentialField> CREDENTIAL_FIELDS = List.of(
            ProviderCredentialField.secret("apiKey", "RAWG API key", "Rawg API key")
    );

    private final RestClient http;
    private final ObjectMapper objectMapper;
    private final ProviderCredentialLookup credentialLookup;

    public RawgProvider(RestClient http, ObjectMapper objectMapper, ProviderCredentialLookup credentialLookup) {
        this.http = http;
        this.objectMapper = objectMapper;
        this.credentialLookup = credentialLookup;
    }

    @Override
    public String key() {
        return "rawg";
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
            log.debug("rawg apiKey not configured; skipping lookup");
            return Optional.empty();
        }
        String apiKey = stored.get().values().get("apiKey");
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("rawg apiKey not configured; skipping lookup");
            return Optional.empty();
        }
        String id = normalizeId(idValue);
        if (id == null) return Optional.empty();

        try {
            ResponseEntity<String> response = http.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("api.rawg.io")
                            .path("/api/games/" + id)
                            .queryParam("key", apiKey)
                            .build())
                    .header("User-Agent", USER_AGENT)
                    .retrieve()
                    .toEntity(String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("rawg lookup failed status={} id={}", response.getStatusCode(), id);
                return Optional.empty();
            }

            String body = response.getBody();
            if (body == null || body.isBlank()) return Optional.empty();

            JsonNode root = objectMapper.readTree(body);
            ObjectNode normalized = objectMapper.createObjectNode();

            putText(normalized, "title", root.get("name"));
            putText(normalized, "description", root.get("description_raw"));
            Integer year = yearFromDate(root.get("released"));
            if (year != null) normalized.put("published_year", year);
            normalized.put("rawg_id", id);
            String slug = text(root.get("slug"));
            if (slug != null) normalized.put("canonical_url", "https://rawg.io/games/" + slug);

            List<ProviderAsset> assets = new ArrayList<>();
            String bg = text(root.get("background_image"));
            if (bg != null) assets.add(new ProviderAsset(AssetType.COVER, URI.create(bg), null, null));
            String add = text(root.get("background_image_additional"));
            if (add != null) assets.add(new ProviderAsset(AssetType.THUMBNAIL, URI.create(add), null, null));

            ProviderConfidence confidence = new ProviderConfidence(75, "RAWG match");

            return Optional.of(new ProviderResult(
                    key(),
                    Map.of("json", body),
                    Map.of("json", normalized.toString()),
                    assets,
                    confidence
            ));
        } catch (Exception e) {
            log.warn("rawg lookup failed id={} error={}", idValue, e.getMessage());
            return Optional.empty();
        }
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
        if (!v.matches("[a-zA-Z0-9-]+")) return null;
        return v;
    }
}
