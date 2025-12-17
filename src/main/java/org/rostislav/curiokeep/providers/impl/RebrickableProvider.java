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
import org.springframework.http.HttpHeaders;
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
public class RebrickableProvider implements MetadataProvider {

    private static final Logger log = LoggerFactory.getLogger(RebrickableProvider.class);
    private static final String USER_AGENT = "CurioKeep/1.0 (+https://github.com/RoastSlav/CurioKeep)";

    private final RestClient http;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public RebrickableProvider(RestClient http, ObjectMapper objectMapper, @Value("${curiokeep.providers.rebrickable.apiKey:}") String apiKey) {
        this.http = http;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    @Override
    public String key() {
        return "rebrickable";
    }

    @Override
    public boolean supports(ItemIdentifierEntity.IdType idType) {
        return idType == ItemIdentifierEntity.IdType.CUSTOM;
    }

    @Override
    public Optional<ProviderResult> fetch(ItemIdentifierEntity.IdType idType, String idValue) {
        if (apiKey.isEmpty()) {
            log.debug("rebrickable apiKey not configured; skipping lookup");
            return Optional.empty();
        }
        String id = normalizeId(idValue);
        if (id == null) return Optional.empty();

        try {
            ResponseEntity<String> response = http.get()
                    .uri("https://rebrickable.com/api/v3/lego/sets/" + id + "/")
                    .header(HttpHeaders.AUTHORIZATION, "key " + apiKey)
                    .header("User-Agent", USER_AGENT)
                    .retrieve()
                    .toEntity(String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("rebrickable lookup failed status={} id={}", response.getStatusCode(), id);
                return Optional.empty();
            }

            String body = response.getBody();
            if (body == null || body.isBlank()) return Optional.empty();

            JsonNode root = objectMapper.readTree(body);
            ObjectNode normalized = objectMapper.createObjectNode();

            putText(normalized, "title", root.get("name"));
            putText(normalized, "set_number", root.get("set_num"));
            if (root.has("year")) normalized.put("published_year", root.get("year").asInt());
            if (root.has("num_parts")) normalized.put("pieces", root.get("num_parts").asInt());
            normalized.put("rebrickable_id", id);
            String setNum = text(root.get("set_num"));
            if (setNum != null) normalized.put("canonical_url", "https://rebrickable.com/sets/" + setNum);

            List<ProviderAsset> assets = new ArrayList<>();
            String image = text(root.get("set_img_url"));
            if (image != null) assets.add(new ProviderAsset(AssetType.COVER, URI.create(image), null, null));

            ProviderConfidence confidence = new ProviderConfidence(70, "Rebrickable match");

            return Optional.of(new ProviderResult(
                    key(),
                    Map.of("json", body),
                    Map.of("json", normalized.toString()),
                    assets,
                    confidence
            ));
        } catch (Exception e) {
            log.warn("rebrickable lookup failed id={} error={}", idValue, e.getMessage());
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
        if (v.isEmpty()) return null;
        return v;
    }
}
