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
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class ComicVineProvider implements MetadataProvider {

    private static final Logger log = LoggerFactory.getLogger(ComicVineProvider.class);

    private static final String HOST = "comicvine.gamespot.com";
    private static final String BASE_PATH = "/api";
    private static final Duration MIN_DELAY = Duration.ofMillis(150); // gentle throttle

    private static final List<ProviderCredentialField> CREDENTIAL_FIELDS = List.of(
            ProviderCredentialField.secret("apiKey", "Comic Vine API key", "Comic Vine API key from your account")
    );

    private final RestClient http;
    private final ObjectMapper objectMapper;
    private final ProviderCredentialLookup credentialLookup;
    private long lastCallNanos = 0L;

    public ComicVineProvider(RestClient http, ObjectMapper objectMapper, ProviderCredentialLookup credentialLookup) {
        this.http = http;
        this.objectMapper = objectMapper;
        this.credentialLookup = credentialLookup;
    }

    @Override
    public String key() {
        return "comicvine";
    }

    @Override
    public boolean supports(ItemIdentifierEntity.IdType idType) {
        return idType == ItemIdentifierEntity.IdType.ISBN10
                || idType == ItemIdentifierEntity.IdType.ISBN13
                || idType == ItemIdentifierEntity.IdType.UPC
                || idType == ItemIdentifierEntity.IdType.CUSTOM;
    }

    @Override
    public List<ProviderCredentialField> credentialFields() {
        return CREDENTIAL_FIELDS;
    }

    @Override
    public Optional<ProviderResult> fetch(ItemIdentifierEntity.IdType idType, String idValue) {
        Optional<ProviderCredential> stored = credentialLookup.getCredentials(key());
        if (stored.isEmpty()) {
            log.debug("comicvine apiKey not configured; skipping lookup");
            return Optional.empty();
        }
        String apiKey = stored.get().values().get("apiKey");
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("comicvine apiKey missing; skipping lookup");
            return Optional.empty();
        }

        String normalizedId = normalize(idValue);
        if (normalizedId == null) return Optional.empty();

        try {
            throttle();
            if (idType == ItemIdentifierEntity.IdType.CUSTOM) {
                String issueId = extractIssueId(normalizedId);
                if (issueId != null) {
                    return fetchIssueById(issueId, apiKey, 95, "ComicVine issue id match");
                }
            }

            if (idType == ItemIdentifierEntity.IdType.UPC || idType == ItemIdentifierEntity.IdType.ISBN10 || idType == ItemIdentifierEntity.IdType.ISBN13) {
                return searchIssues(apiKey, normalizedId, idType == ItemIdentifierEntity.IdType.UPC ? 85 : 80, "UPC/ISBN search");
            }

            // fallback: treat custom as title+issue search
            return searchIssues(apiKey, normalizedId, 65, "Title search");
        } catch (Exception e) {
            log.warn("comicvine lookup failed idType={} idValue={} error={}", idType, idValue, e.getMessage());
            return Optional.empty();
        }
    }

    private void throttle() {
        long now = System.nanoTime();
        long minGapNanos = MIN_DELAY.toNanos();
        if (lastCallNanos > 0 && now - lastCallNanos < minGapNanos) {
            long sleep = minGapNanos - (now - lastCallNanos);
            try {
                Thread.sleep(Duration.ofNanos(sleep));
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        lastCallNanos = System.nanoTime();
    }

    private Optional<ProviderResult> fetchIssueById(String issueId, String apiKey, int score, String reason) throws Exception {
        ResponseEntity<String> resp = http.get()
                .uri(uriBuilder -> uriBuilder
                    .scheme("https")
                    .host(HOST)
                    .path(BASE_PATH + "/issue/4000-" + issueId + "/")
                    .queryParam("api_key", apiKey)
                    .queryParam("format", "json")
                    .build())
                .retrieve()
                .toEntity(String.class);

        if (!resp.getStatusCode().is2xxSuccessful()) {
            log.warn("comicvine issue lookup failed status={} id={}", resp.getStatusCode(), issueId);
            return Optional.empty();
        }

        String body = resp.getBody();
        if (body == null || body.isBlank()) return Optional.empty();

        JsonNode root = objectMapper.readTree(body);
        JsonNode result = root.get("results");
        if (result == null || result.isNull()) return Optional.empty();

        ProviderResult pr = buildResult(result, body, score, reason, issueId, null);
        return Optional.ofNullable(pr);
    }

    private Optional<ProviderResult> searchIssues(String apiKey, String query, int score, String reason) throws Exception {
        ResponseEntity<String> resp = http.get()
                .uri(uriBuilder -> uriBuilder
                .scheme("https")
                .host(HOST)
                .path(BASE_PATH + "/search/")
                        .queryParam("api_key", apiKey)
                        .queryParam("format", "json")
                        .queryParam("resources", "issue")
                        .queryParam("query", query)
                        .build())
                .retrieve()
                .toEntity(String.class);

        if (!resp.getStatusCode().is2xxSuccessful()) {
            log.warn("comicvine search failed status={} query={}", resp.getStatusCode(), query);
            return Optional.empty();
        }

        String body = resp.getBody();
        if (body == null || body.isBlank()) return Optional.empty();

        JsonNode root = objectMapper.readTree(body);
        JsonNode results = root.get("results");
        if (results == null || !results.isArray() || results.isEmpty()) return Optional.empty();

        JsonNode best = results.get(0);
        String issueId = text(best.get("id"));
        ProviderResult pr = buildResult(best, body, score, reason, issueId, null);
        return Optional.ofNullable(pr);
    }

    private ProviderResult buildResult(JsonNode node, String rawBody, int score, String reason, String issueId, String volumeId) {
        ObjectNode normalized = objectMapper.createObjectNode();

        // Series title comes from the volume; issue title from the issue name
        String issueTitle = text(node.get("name"));
        putText(normalized, "issue_title", node.get("name"));
        JsonNode volume = node.get("volume");
        if (volume != null && volume.isObject()) {
            String seriesName = text(volume.get("name"));
            if (seriesName != null) {
                normalized.put("title", seriesName);
                normalized.put("series", seriesName);
            }
            putText(normalized, "comicvine_volume_id", volume.get("id"));
        }
        putText(normalized, "issue_number", node.get("issue_number"));
        putText(normalized, "publisher", node.path("publisher").get("name"));
        putText(normalized, "cover_date", node.get("cover_date"));
        String desc = stripHtml(text(node.get("description")));
        if (desc != null) normalized.put("description", desc);
        if (issueId != null) normalized.put("comicvine_issue_id", issueId);
        if (issueId != null) normalized.put("comicvine_id", "4000-" + issueId);
        if (volumeId != null) normalized.put("comicvine_volume_id", volumeId);

        // creators (person_credits array)
        JsonNode credits = node.get("person_credits");
        if (credits != null && credits.isArray()) {
            collectByRole(normalized, credits, "writer", "writers");
            collectByRole(normalized, credits, "artist", "artists");
            collectByRole(normalized, credits, "penciler", "artists");
            collectByRole(normalized, credits, "inker", "inker");
            collectByRole(normalized, credits, "colorist", "colorist");
            collectByRole(normalized, credits, "letterer", "letterer");
            collectByRole(normalized, credits, "cover", "cover_artists");
        }

        // Canonical URL for reference
        putText(normalized, "canonical_url", node.get("site_detail_url"));

        // Derive published year from cover date when present
        String coverDate = text(node.get("cover_date"));
        if (coverDate != null && coverDate.length() >= 4) {
            normalized.put("published_year", coverDate.substring(0, 4));
        }

        List<ProviderAsset> assets = new ArrayList<>();
        JsonNode image = node.get("image");
        if (image != null && image.isObject()) {
            String orig = text(image.get("original_url"));
            if (orig != null) assets.add(new ProviderAsset(AssetType.COVER, URI.create(orig), null, null));
            String thumb = text(image.get("thumb_url"));
            if (thumb != null) assets.add(new ProviderAsset(AssetType.THUMBNAIL, URI.create(thumb), null, null));
        }

        normalized.put("attribution", "Data provided by Comic Vine");

        ProviderConfidence confidence = new ProviderConfidence(score, reason);

        return new ProviderResult(
                key(),
                Map.of("json", rawBody),
                Map.of("json", normalized.toString()),
                assets,
                confidence
        );
    }

    private void collectByRole(ObjectNode target, JsonNode credits, String roleKey, String outKey) {
        List<String> names = new ArrayList<>();
        for (JsonNode c : credits) {
            if (!roleKey.equalsIgnoreCase(text(c.get("role")))) continue;
            String name = text(c.get("name"));
            if (name != null) names.add(name);
        }
        if (!names.isEmpty()) target.put(outKey, String.join(", ", names));
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

    private String normalize(String raw) {
        if (raw == null) return null;
        String v = raw.trim();
        return v.isEmpty() ? null : v;
    }

    private String extractIssueId(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.matches("\\d+")) {
            return trimmed;
        }
        if (trimmed.matches("4000-\\d+")) {
            return trimmed.substring("4000-".length());
        }
        return null;
    }

    private String stripHtml(String in) {
        if (in == null) return null;
        return in.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
    }
}
