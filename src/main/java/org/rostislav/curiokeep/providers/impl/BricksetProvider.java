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
public class BricksetProvider implements MetadataProvider {

    private static final Logger log = LoggerFactory.getLogger(BricksetProvider.class);

    private static final List<ProviderCredentialField> CREDENTIAL_FIELDS = List.of(
            ProviderCredentialField.secret("apiKey", "Brickset API key", "Brickset API key")
    );

    private final RestClient http;
    private final ObjectMapper objectMapper;
    private final ProviderCredentialLookup credentialLookup;

    public BricksetProvider(RestClient http, ObjectMapper objectMapper, ProviderCredentialLookup credentialLookup) {
        this.http = http;
        this.objectMapper = objectMapper;
        this.credentialLookup = credentialLookup;
    }

    @Override
    public String key() {
        return "brickset";
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
            log.debug("brickset apiKey not configured; skipping lookup");
            return Optional.empty();
        }
        String apiKey = stored.get().values().get("apiKey");
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("brickset apiKey not configured; skipping lookup");
            return Optional.empty();
        }
        String id = normalizeId(idValue);
        if (id == null) return Optional.empty();

        try {
                ResponseEntity<String> response = http.get()
                    .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("brickset.com")
                        .path("/api/v3.asmx/getSet")
                        .queryParam("apiKey", apiKey)
                        .queryParam("setID", id)
                        .build())
                    .retrieve()
                    .toEntity(String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("brickset lookup failed status={} id={}", response.getStatusCode(), id);
                return Optional.empty();
            }

            String body = response.getBody();
            if (body == null || body.isBlank()) return Optional.empty();

            JsonNode root = objectMapper.readTree(body);
            if (!root.isArray() || root.isEmpty()) return Optional.empty();
            JsonNode node = root.get(0);

            ObjectNode normalized = objectMapper.createObjectNode();
            putText(normalized, "title", node.get("name"));
            putText(normalized, "set_number", node.get("setNumber"));
            if (node.has("year")) normalized.put("published_year", node.get("year").asInt());
            if (node.has("pieces")) normalized.put("pieces", node.get("pieces").asInt());
            if (node.has("minifigs")) normalized.put("minifigs", node.get("minifigs").asInt());
            normalized.put("brickset_id", id);
            String setNum = text(node.get("setNumber"));
            if (setNum != null) normalized.put("canonical_url", "https://brickset.com/sets/" + setNum);

            List<ProviderAsset> assets = new ArrayList<>();
            String image = text(node.get("image"));
            if (image != null) assets.add(new ProviderAsset(AssetType.COVER, URI.create(image), null, null));

            ProviderConfidence confidence = new ProviderConfidence(70, "Brickset match");

            return Optional.of(new ProviderResult(
                    key(),
                    Map.of("json", body),
                    Map.of("json", normalized.toString()),
                    assets,
                    confidence
            ));
        } catch (Exception e) {
            log.warn("brickset lookup failed id={} error={}", idValue, e.getMessage());
            return Optional.empty();
        }
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
        if (!v.matches("[0-9A-Za-z-]+")) return null;
        return v;
    }
}
