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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class CoverArtArchiveProvider implements MetadataProvider {

    private static final Logger log = LoggerFactory.getLogger(CoverArtArchiveProvider.class);
    private static final String USER_AGENT = "CurioKeep/1.0 (+https://github.com/RoastSlav/CurioKeep)";
    private static final Pattern MBID = Pattern.compile("^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$");

    private final RestClient http;
    private final ObjectMapper objectMapper;

    public CoverArtArchiveProvider(RestClient http, ObjectMapper objectMapper) {
        this.http = http;
        this.objectMapper = objectMapper;
    }

    @Override
    public String key() {
        return "coverartarchive";
    }

    @Override
    public boolean supports(ItemIdentifierEntity.IdType idType) {
        return idType == ItemIdentifierEntity.IdType.CUSTOM;
    }

    @Override
    public Optional<ProviderResult> fetch(ItemIdentifierEntity.IdType idType, String idValue) {
        if (idValue == null || !MBID.matcher(idValue.trim()).matches()) {
            return Optional.empty();
        }
        String mbid = idValue.trim();
        try {
            ResponseEntity<String> response = http.get()
                    .uri("https://coverartarchive.org/release/{mbid}", mbid)
                    .header("User-Agent", USER_AGENT)
                    .retrieve()
                    .toEntity(String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("coverartarchive lookup failed: status={} mbid={}", response.getStatusCode(), mbid);
                return Optional.empty();
            }

            String body = response.getBody();
            if (body == null || body.isBlank()) return Optional.empty();

            JsonNode root = objectMapper.readTree(body);
            JsonNode images = root.path("images");
            if (!images.isArray() || images.size() == 0) return Optional.empty();

            JsonNode picked = pickImage(images);
            if (picked == null) return Optional.empty();

            List<ProviderAsset> assets = new ArrayList<>();
            String full = text(picked.get("image"));
            if (full != null) assets.add(new ProviderAsset(AssetType.COVER, URI.create(full), null, null));

            JsonNode thumbnails = picked.path("thumbnails");
            String large = text(thumbnails.get("large"));
            if (large != null) assets.add(new ProviderAsset(AssetType.THUMBNAIL, URI.create(large), null, null));

                Map<String,Object> normalized = Map.<String,Object>of(
                    "json", objectMapper.createObjectNode()
                        .put("musicbrainz_id", mbid)
                        .put("canonical_url", "https://musicbrainz.org/release/" + mbid)
                        .toString()
                );

            ProviderConfidence confidence = new ProviderConfidence(60, "Cover Art Archive by MBID");
            return Optional.of(new ProviderResult(key(), Map.of("json", body), normalized, assets, confidence));
        } catch (Exception e) {
            log.warn("coverartarchive lookup failed mbid={} error={}", idValue, e.getMessage());
            return Optional.empty();
        }
    }

    private JsonNode pickImage(JsonNode images) {
        for (JsonNode img : images) {
            if (img.path("front").asBoolean(false)) return img;
        }
        return images.size() == 0 ? null : images.get(0);
    }

    private String text(JsonNode n) {
        if (n == null || n.isNull() || n.isMissingNode()) return null;
        String v = n.asText(null);
        if (v == null) return null;
        v = v.trim();
        return v.isEmpty() ? null : v;
    }
}
