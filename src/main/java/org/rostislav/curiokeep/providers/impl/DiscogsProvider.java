package org.rostislav.curiokeep.providers.impl;

import org.rostislav.curiokeep.items.entities.ItemIdentifierEntity;
import org.rostislav.curiokeep.providers.AssetType;
import org.rostislav.curiokeep.providers.MetadataProvider;
import org.rostislav.curiokeep.providers.ProviderAsset;
import org.rostislav.curiokeep.providers.ProviderConfidence;
import org.rostislav.curiokeep.providers.ProviderResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Component
public class DiscogsProvider implements MetadataProvider {

    private static final Logger log = LoggerFactory.getLogger(DiscogsProvider.class);
    private static final String USER_AGENT = "CurioKeep/1.0 (+https://github.com/RoastSlav/CurioKeep)";

    private final RestClient http;
    private final ObjectMapper objectMapper;
    private final String token;

    public DiscogsProvider(RestClient http,
                           ObjectMapper objectMapper,
                           @Value("${curiokeep.providers.discogs.token:}") String token) {
        this.http = http;
        this.objectMapper = objectMapper;
        this.token = token == null ? "" : token.trim();
    }

    @Override
    public String key() {
        return "discogs";
    }

    @Override
    public boolean supports(ItemIdentifierEntity.IdType idType) {
        return idType == ItemIdentifierEntity.IdType.UPC || idType == ItemIdentifierEntity.IdType.EAN;
    }

    @Override
    public Optional<ProviderResult> fetch(ItemIdentifierEntity.IdType idType, String idValue) {
        if (token.isEmpty()) {
            log.debug("discogs token not configured, skipping lookup");
            return Optional.empty();
        }

        String barcode = normalizeBarcode(idValue);
        if (barcode == null) return Optional.empty();

        try {
            ResponseEntity<String> response = http.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("api.discogs.com")
                            .path("/database/search")
                            .queryParam("barcode", barcode)
                            .queryParam("type", "release")
                            .queryParam("per_page", "1")
                            .build())
                    .header("User-Agent", USER_AGENT)
                    .header("Authorization", "Discogs token=" + token)
                    .retrieve()
                    .toEntity(String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("discogs lookup failed: status={} barcode={}", response.getStatusCode(), barcode);
                return Optional.empty();
            }

            String body = response.getBody();
            if (body == null || body.isBlank()) return Optional.empty();

            JsonNode root = objectMapper.readTree(body);
            JsonNode results = root.path("results");
            if (!results.isArray() || results.isEmpty()) return Optional.empty();

            JsonNode first = results.get(0);
            ProviderResult result = buildResult(idType, barcode, first);
            if (result == null) return Optional.empty();

            return Optional.of(result);
        } catch (Exception e) {
            log.warn("discogs lookup failed barcode={} error={}", idValue, e.getMessage());
            return Optional.empty();
        }
    }

    private ProviderResult buildResult(ItemIdentifierEntity.IdType idType, String barcode, JsonNode node) {
        ObjectNode normalized = objectMapper.createObjectNode();

        putText(normalized, "title", node.get("title"));
        putText(normalized, "country", node.get("country"));
        putText(normalized, "publisher", firstLabel(node.path("label")));

        if (idType == ItemIdentifierEntity.IdType.UPC) normalized.put("upc", barcode);
        if (idType == ItemIdentifierEntity.IdType.EAN) normalized.put("ean", barcode);

        if (node.hasNonNull("year")) normalized.put("published_year", node.get("year").asInt());
        if (node.hasNonNull("id")) normalized.put("discogs_id", node.get("id").asInt());
        putText(normalized, "canonical_url", node.get("uri"));

        List<ProviderAsset> assets = new ArrayList<>();
        String cover = text(node.get("cover_image"));
        if (cover != null) assets.add(new ProviderAsset(AssetType.COVER, URI.create(cover), null, null));
        String thumb = text(node.get("thumb"));
        if (thumb != null && (cover == null || !cover.equals(thumb))) {
            assets.add(new ProviderAsset(AssetType.THUMBNAIL, URI.create(thumb), null, null));
        }

        ProviderConfidence confidence = new ProviderConfidence(75, "Discogs barcode match");

        return new ProviderResult(
                key(),
                Map.of("json", node.toString()),
                Map.of("json", normalized.toString()),
                assets,
                confidence
        );
    }

    private String firstLabel(JsonNode label) {
        if (label == null || label.isMissingNode() || label.isNull()) return null;
        if (label.isArray() && !label.isEmpty()) return text(label.get(0));
        return text(label);
    }

    private void putText(ObjectNode target, String key, String value) {
        if (value == null || value.isBlank()) return;
        target.put(key, value);
    }

    private void putText(ObjectNode target, String key, JsonNode value) {
        putText(target, key, text(value));
    }

    private String text(JsonNode n) {
        if (n == null || n.isNull() || n.isMissingNode()) return null;
        String v = n.asText(null);
        if (v == null) return null;
        v = v.trim();
        return v.isEmpty() ? null : v;
    }

    private String normalizeBarcode(String in) {
        if (in == null) return null;
        String v = in.replaceAll("[^0-9]", "");
        if (v.length() < 8) return null;
        return v.toUpperCase(Locale.ROOT);
    }
}
