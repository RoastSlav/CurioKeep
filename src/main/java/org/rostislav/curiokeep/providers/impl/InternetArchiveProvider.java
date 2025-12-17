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
public class InternetArchiveProvider implements MetadataProvider {

    private static final Logger log = LoggerFactory.getLogger(InternetArchiveProvider.class);
    private static final String USER_AGENT = "CurioKeep/1.0 (+https://github.com/RoastSlav/CurioKeep)";

    private final RestClient http;
    private final ObjectMapper objectMapper;

    public InternetArchiveProvider(RestClient http, ObjectMapper objectMapper) {
        this.http = http;
        this.objectMapper = objectMapper;
    }

    @Override
    public String key() {
        return "internetarchive";
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
                    .uri("https://archive.org/metadata/" + id)
                    .header("User-Agent", USER_AGENT)
                    .retrieve()
                    .toEntity(String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("internetarchive lookup failed status={} id={}", response.getStatusCode(), id);
                return Optional.empty();
            }

            String body = response.getBody();
            if (body == null || body.isBlank()) return Optional.empty();

            JsonNode root = objectMapper.readTree(body);
            ObjectNode metadata = root.path("metadata").isObject() ? (ObjectNode) root.get("metadata") : objectMapper.createObjectNode();

            ObjectNode normalized = objectMapper.createObjectNode();
            putText(normalized, "title", metadata.get("title"));
            putText(normalized, "description", metadata.get("description"));
            Integer year = yearFromText(metadata.get("date"));
            if (year != null) normalized.put("published_year", year);
            putText(normalized, "language", metadata.get("language"));
            normalized.put("internet_archive_id", id);
            normalized.put("canonical_url", "https://archive.org/details/" + id);

            List<ProviderAsset> assets = new ArrayList<>();
            JsonNode files = root.get("files");
            if (files != null && files.isArray()) {
                for (JsonNode f : files) {
                    String name = text(f.get("name"));
                    if (name != null && name.toLowerCase().endsWith(".jpg")) {
                        assets.add(new ProviderAsset(AssetType.THUMBNAIL, URI.create("https://archive.org/download/" + id + "/" + name), null, null));
                        break;
                    }
                }
            }

            ProviderConfidence confidence = new ProviderConfidence(60, "Internet Archive match");

            return Optional.of(new ProviderResult(
                    key(),
                    Map.of("json", body),
                    Map.of("json", normalized.toString()),
                    assets,
                    confidence
            ));
        } catch (Exception e) {
            log.warn("internetarchive lookup failed id={} error={}", idValue, e.getMessage());
            return Optional.empty();
        }
    }

    private Integer yearFromText(JsonNode node) {
        String v = text(node);
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
