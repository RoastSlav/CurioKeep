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
public class OpenProductDataProvider implements MetadataProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenProductDataProvider.class);
    private static final String USER_AGENT = "CurioKeep/1.0 (+https://github.com/RoastSlav/CurioKeep)";

    private final RestClient http;
    private final ObjectMapper objectMapper;

    public OpenProductDataProvider(RestClient http, ObjectMapper objectMapper) {
        this.http = http;
        this.objectMapper = objectMapper;
    }

    @Override
    public String key() {
        return "openproduct";
    }

    @Override
    public boolean supports(ItemIdentifierEntity.IdType idType) {
        return idType == ItemIdentifierEntity.IdType.UPC || idType == ItemIdentifierEntity.IdType.CUSTOM;
    }

    @Override
    public Optional<ProviderResult> fetch(ItemIdentifierEntity.IdType idType, String idValue) {
        String id = normalizeId(idValue);
        if (id == null) return Optional.empty();

        try {
            ResponseEntity<String> response = http.get()
                    .uri("https://world.openfoodfacts.org/api/v2/product/" + id)
                    .header("User-Agent", USER_AGENT)
                    .retrieve()
                    .toEntity(String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("openproduct lookup failed status={} id={}", response.getStatusCode(), id);
                return Optional.empty();
            }

            String body = response.getBody();
            if (body == null || body.isBlank()) return Optional.empty();

            JsonNode root = objectMapper.readTree(body).path("product");
            if (root.isMissingNode() || root.isNull()) return Optional.empty();

            ObjectNode normalized = objectMapper.createObjectNode();
            putText(normalized, "title", root.get("product_name"));
            putText(normalized, "brand", root.get("brands"));
            normalized.put("gtin", id);
            putText(normalized, "canonical_url", root.get("url"));
            putText(normalized, "categories", root.get("categories"));
            putText(normalized, "ingredients", root.get("ingredients_text"));

            List<ProviderAsset> assets = new ArrayList<>();
            String image = text(root.get("image_front_url"));
            if (image == null) image = text(root.get("image_url"));
            if (image != null) assets.add(new ProviderAsset(AssetType.COVER, URI.create(image), null, null));
            String thumb = text(root.get("image_thumb_url"));
            if (thumb != null) assets.add(new ProviderAsset(AssetType.THUMBNAIL, URI.create(thumb), null, null));

            ProviderConfidence confidence = new ProviderConfidence(60, "Open Product match");

            return Optional.of(new ProviderResult(
                    key(),
                    Map.of("json", body),
                    Map.of("json", normalized.toString()),
                    assets,
                    confidence
            ));
        } catch (Exception e) {
            log.warn("openproduct lookup failed id={} error={}", idValue, e.getMessage());
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
        if (!v.matches("\\d{8,14}")) return null;
        return v;
    }
}
