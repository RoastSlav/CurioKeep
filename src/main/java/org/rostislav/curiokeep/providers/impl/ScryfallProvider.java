package org.rostislav.curiokeep.providers.impl;

import org.rostislav.curiokeep.items.entities.ItemIdentifierEntity;
import org.rostislav.curiokeep.providers.AssetType;
import org.rostislav.curiokeep.providers.MetadataProvider;
import org.rostislav.curiokeep.providers.ProviderAsset;
import org.rostislav.curiokeep.providers.ProviderConfidence;
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
public class ScryfallProvider implements MetadataProvider {

    private static final Logger log = LoggerFactory.getLogger(ScryfallProvider.class);

    private final RestClient http;
    private final ObjectMapper objectMapper;

    public ScryfallProvider(RestClient http, ObjectMapper objectMapper) {
        this.http = http;
        this.objectMapper = objectMapper;
    }

    @Override
    public String key() {
        return "scryfall";
    }

    @Override
    public boolean supports(ItemIdentifierEntity.IdType idType) {
        return idType == ItemIdentifierEntity.IdType.CUSTOM;
    }

    @Override
    public Optional<ProviderResult> fetch(ItemIdentifierEntity.IdType idType, String idValue) {
        String id = normalizeId(idValue);
        if (id == null) return Optional.empty();

        try {
                ResponseEntity<String> response = http.get()
                    .uri("https://api.scryfall.com/cards/" + id)
                    .retrieve()
                    .toEntity(String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("scryfall lookup failed status={} id={}", response.getStatusCode(), id);
                return Optional.empty();
            }

            String body = response.getBody();
            if (body == null || body.isBlank()) return Optional.empty();

            JsonNode root = objectMapper.readTree(body);
            ObjectNode normalized = objectMapper.createObjectNode();

            putText(normalized, "title", root.get("name"));
            putText(normalized, "description", root.get("oracle_text"));
            Integer year = yearFromDate(root.get("released_at"));
            if (year != null) normalized.put("published_year", year);
            normalized.put("scryfall_id", id);
            putText(normalized, "canonical_url", root.get("uri"));

            List<ProviderAsset> assets = new ArrayList<>();
            JsonNode images = root.get("image_uris");
            if (images != null && images.isObject()) {
                String large = text(images.get("large"));
                if (large != null) assets.add(new ProviderAsset(AssetType.COVER, URI.create(large), null, null));
                String small = text(images.get("small"));
                if (small != null) assets.add(new ProviderAsset(AssetType.THUMBNAIL, URI.create(small), null, null));
            }

            ProviderConfidence confidence = new ProviderConfidence(70, "Scryfall card match");

            return Optional.of(new ProviderResult(
                    key(),
                    Map.of("json", body),
                    Map.of("json", normalized.toString()),
                    assets,
                    confidence
            ));
        } catch (Exception e) {
            log.warn("scryfall lookup failed id={} error={}", idValue, e.getMessage());
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
        if (v.isEmpty()) return null;
        return v;
    }
}
