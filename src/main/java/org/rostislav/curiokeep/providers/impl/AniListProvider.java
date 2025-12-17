package org.rostislav.curiokeep.providers.impl;

import org.rostislav.curiokeep.items.entities.ItemIdentifierEntity;
import org.rostislav.curiokeep.providers.AssetType;
import org.rostislav.curiokeep.providers.MetadataProvider;
import org.rostislav.curiokeep.providers.ProviderAsset;
import org.rostislav.curiokeep.providers.ProviderConfidence;
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
public class AniListProvider implements MetadataProvider {

    private static final Logger log = LoggerFactory.getLogger(AniListProvider.class);
    private static final String USER_AGENT = "CurioKeep/1.0 (+https://github.com/RoastSlav/CurioKeep)";
    private static final String QUERY = "query ($id: Int) { Media(id: $id, type: ANIME) { id title { romaji english native } description(asHtml: false) startDate { year } seasonYear season coverImage { extraLarge large medium } siteUrl } }";

    private final RestClient http;
    private final ObjectMapper objectMapper;

    public AniListProvider(RestClient http, ObjectMapper objectMapper) {
        this.http = http;
        this.objectMapper = objectMapper;
    }

    @Override
    public String key() {
        return "anilist";
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
            ObjectNode variables = objectMapper.createObjectNode();
            variables.put("id", Integer.parseInt(id));
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("query", QUERY);
            payload.set("variables", variables);

            ResponseEntity<String> response = http.post()
                    .uri("https://graphql.anilist.co")
                    .header("User-Agent", USER_AGENT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload.toString())
                    .retrieve()
                    .toEntity(String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("anilist lookup failed status={} id={}", response.getStatusCode(), id);
                return Optional.empty();
            }

            String body = response.getBody();
            if (body == null || body.isBlank()) return Optional.empty();

            JsonNode root = objectMapper.readTree(body).path("data").path("Media");
            if (root.isMissingNode() || root.isNull()) return Optional.empty();

            ObjectNode normalized = objectMapper.createObjectNode();
            JsonNode titleNode = root.get("title");
            String title = firstNonBlank(titleNode == null ? null : titleNode.get("english"),
                    titleNode == null ? null : titleNode.get("romaji"),
                    titleNode == null ? null : titleNode.get("native"));
            if (title != null) normalized.put("title", title);
            putText(normalized, "description", root.get("description"));
            Integer year = intValue(root.get("seasonYear"));
            if (year == null) year = intValue(root.path("startDate").get("year"));
            if (year != null) normalized.put("published_year", year);
            normalized.put("anilist_id", id);
            putText(normalized, "canonical_url", root.get("siteUrl"));

            List<ProviderAsset> assets = new ArrayList<>();
            JsonNode covers = root.get("coverImage");
            if (covers != null && covers.isObject()) {
                String xl = text(covers.get("extraLarge"));
                String lg = text(covers.get("large"));
                String md = text(covers.get("medium"));
                if (xl != null) assets.add(new ProviderAsset(AssetType.COVER, URI.create(xl), null, null));
                if (lg != null) assets.add(new ProviderAsset(AssetType.THUMBNAIL, URI.create(lg), null, null));
                if (md != null) assets.add(new ProviderAsset(AssetType.THUMBNAIL, URI.create(md), null, null));
            }

            ProviderConfidence confidence = new ProviderConfidence(70, "AniList anime lookup");

            return Optional.of(new ProviderResult(
                    key(),
                    Map.of("json", body),
                    Map.of("json", normalized.toString()),
                    assets,
                    confidence
            ));
        } catch (Exception e) {
            log.warn("anilist lookup failed id={} error={}", idValue, e.getMessage());
            return Optional.empty();
        }
    }

    private Integer intValue(JsonNode n) {
        if (n == null || !n.isInt()) return null;
        return n.asInt();
    }

    private String firstNonBlank(JsonNode... nodes) {
        for (JsonNode n : nodes) {
            String v = text(n);
            if (v != null) return v;
        }
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
