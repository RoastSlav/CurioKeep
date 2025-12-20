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
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class MetronProvider implements MetadataProvider {

    private static final Logger log = LoggerFactory.getLogger(MetronProvider.class);

    private static final String HOST = "metron.cloud";
    // Metron issue endpoints are under /api/issue/ (singular)
    private static final String BASE_PATH = "/api/issue";

        private static final List<ProviderCredentialField> CREDENTIAL_FIELDS = List.of(
            ProviderCredentialField.secret("username", "Metron username", "Metron account username (basic auth)"),
            ProviderCredentialField.secret("password", "Metron password", "Metron account password (basic auth)")
        );

    private final RestClient http;
    private final ObjectMapper objectMapper;
    private final ProviderCredentialLookup credentialLookup;

    public MetronProvider(RestClient http, ObjectMapper objectMapper, ProviderCredentialLookup credentialLookup) {
        this.http = http;
        this.objectMapper = objectMapper;
        this.credentialLookup = credentialLookup;
    }

    @Override
    public String key() {
        return "metron";
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
            log.debug("metron apiKey not configured; skipping lookup");
            return Optional.empty();
        }
        String username = stored.get().values().get("username");
        String password = stored.get().values().get("password");
        if (username == null || password == null || username.isBlank() || password.isBlank()) {
            log.debug("metron basic auth missing; skipping lookup");
            return Optional.empty();
        }

        String normalized = normalize(idValue);
        if (normalized == null) return Optional.empty();

        try {
            if (idType == ItemIdentifierEntity.IdType.CUSTOM && normalized.matches("\\d+")) {
                return fetchIssueById(normalized, username, password, 90, "Metron issue id match");
            }

            if (idType == ItemIdentifierEntity.IdType.UPC) {
                return searchIssuesAndHydrate("upc", normalized, username, password, 85, "Metron UPC match");
            }
            if (idType == ItemIdentifierEntity.IdType.ISBN10 || idType == ItemIdentifierEntity.IdType.ISBN13) {
                return searchIssuesAndHydrate("isbn", normalized, username, password, 85, "Metron ISBN match");
            }

            return searchIssuesAndHydrate("title", normalized, username, password, 65, "Metron title search");
        } catch (Exception e) {
            if (e instanceof RestClientResponseException ex) {
                String body = ex.getResponseBodyAsString();
                int len = body == null ? 0 : body.length();
                log.warn("metron lookup failed idType={} idValue={} status={} bodyLength={}", idType, idValue, ex.getStatusCode(), len);
            } else {
                log.warn("metron lookup failed idType={} idValue={} error={}", idType, idValue, e.getMessage());
            }
            return Optional.empty();
        }
    }

    private Optional<ProviderResult> fetchIssueById(String issueId, String username, String password, int score, String reason) throws Exception {
        JsonNode full = fetchIssueDetail(issueId, username, password);
        if (full == null) return Optional.empty();

        String rawBody = full.toString();
        ProviderResult pr = buildResult(full, rawBody, score, reason, issueId);
        return Optional.ofNullable(pr);
    }

    private JsonNode fetchIssueDetail(String issueId, String username, String password) throws Exception {
        ResponseEntity<String> resp = http.get()
            .uri(uriBuilder -> uriBuilder
                .scheme("https")
                .host(HOST)
                .path(BASE_PATH + "/" + issueId + "/")
                .build())
            .headers(headers -> headers.setBasicAuth(username, password, java.nio.charset.StandardCharsets.UTF_8))
            .retrieve()
            .toEntity(String.class);

        if (!resp.getStatusCode().is2xxSuccessful()) {
            log.warn("metron issue lookup failed status={} id={}", resp.getStatusCode(), issueId);
            return null;
        }

        String body = resp.getBody();
        if (body == null || body.isBlank()) return null;

        return objectMapper.readTree(body);
    }

    private Optional<ProviderResult> searchIssuesAndHydrate(String param, String value, String username, String password, int score, String reason) throws Exception {
        ResponseEntity<String> resp = http.get()
            .uri(uriBuilder -> uriBuilder
                .scheme("https")
                .host(HOST)
                        .path(BASE_PATH + "/")
                .queryParam(param, value)
                .build())
            .headers(headers -> headers.setBasicAuth(username, password, java.nio.charset.StandardCharsets.UTF_8))
            .retrieve()
            .toEntity(String.class);

        if (!resp.getStatusCode().is2xxSuccessful()) {
            log.warn("metron search failed status={} param={} value={}", resp.getStatusCode(), param, value);
            return Optional.empty();
        }

        String body = resp.getBody();
        if (body == null || body.isBlank()) return Optional.empty();

        JsonNode root = objectMapper.readTree(body);
        JsonNode results = root.isArray() ? root : root.get("results");
        if (results == null || !results.isArray() || results.isEmpty()) return Optional.empty();

        JsonNode best = results.get(0);
        String issueId = text(best.get("id"));
        if (issueId == null) return Optional.empty();

        JsonNode full = fetchIssueDetail(issueId, username, password);
        if (full == null) return Optional.empty();

        ProviderResult pr = buildResult(full, full.toString(), score, reason, issueId);
        return Optional.ofNullable(pr);
    }

    private ProviderResult buildResult(JsonNode node, String rawBody, int score, String reason, String issueId) {
        ObjectNode normalized = objectMapper.createObjectNode();

        // Metron response example (issue list item):
        // {
        //   "id": 17687,
        //   "series": {"name": "Rorschach", "volume": 1, "year_began": 2020},
        //   "number": "1",
        //   "issue": "Rorschach (2020) #1",
        //   "cover_date": "2020-12-01",
        //   "store_date": "2020-10-13",
        //   "image": "https://static.metron.cloud/media/issue/...",
        //   "cover_hash": "...",
        //   "modified": "2025-01-04T02:19:11.324858-05:00"
        // }

        JsonNode series = node.get("series");
        String seriesName = text(series != null ? series.get("name") : null);
        if (seriesName != null) {
            normalized.put("title", seriesName);
            normalized.put("series", seriesName);
        }
        putText(normalized, "volume_number", series != null ? series.get("volume") : null);

        putText(normalized, "issue_number", node.get("number"));
        putText(normalized, "issue_title", node.get("issue"));
        putText(normalized, "cover_date", node.get("cover_date"));

        String coverDate = text(node.get("cover_date"));
        if (coverDate != null && coverDate.length() >= 4) {
            normalized.put("published_year", coverDate.substring(0, 4));
        }

        // Some feeds may carry store_date instead; keep it for potential future mapping
        putText(normalized, "store_date", node.get("store_date"));

        // Identifiers
        JsonNode ids = node.get("identifiers");
        if (ids != null && ids.isObject()) {
            putText(normalized, "upc", ids.get("upc"));
            String isbn = text(ids.get("isbn"));
            if (isbn != null) {
                if (isbn.length() == 13) {
                    normalized.put("isbn13", isbn);
                    normalized.put("ean", isbn);
                } else if (isbn.length() == 10) {
                    normalized.put("isbn10", isbn);
                } else {
                    normalized.put("isbn", isbn);
                }
            }
        }

        // Assets
        List<ProviderAsset> assets = new ArrayList<>();
        JsonNode image = node.get("image");
        if (image != null && image.isTextual()) {
            String url = image.asText().trim();
            if (!url.isEmpty()) assets.add(new ProviderAsset(AssetType.COVER, URI.create(url), null, null));
        }

        if (issueId != null) normalized.put("metron_issue_id", issueId);
        // ComicVine cross-reference if present
        String cvId = text(node.get("cv_id"));
        if (cvId != null) {
            String formatted = cvId.startsWith("4000-") ? cvId : "4000-" + cvId;
            normalized.put("comicvine_id", formatted);
        }
        putText(normalized, "cover_hash", node.get("cover_hash"));
        putText(normalized, "modified", node.get("modified"));

        // Publisher
        JsonNode publisher = node.get("publisher");
        if (publisher != null && publisher.isObject()) {
            putText(normalized, "publisher", publisher.get("name"));
        }

        // Description
        String desc = text(node.get("desc"));
        if (desc != null) {
            normalized.put("description", desc);
        }

        // Page count
        putText(normalized, "page_count", node.get("page"));

        // Canonical URL
        putText(normalized, "canonical_url", node.get("resource_url"));

        // Creators
        applyCredits(normalized, node.get("credits"));

        normalized.put("attribution", "Data from Metron");

        ProviderConfidence confidence = new ProviderConfidence(score, reason);

        return new ProviderResult(
                key(),
                Map.of("json", rawBody),
                Map.of("json", normalized.toString()),
                assets,
                confidence
        );
    }

    private String text(JsonNode n) {
        if (n == null || n.isNull() || n.isMissingNode()) return null;
        String v = n.asText(null);
        if (v == null) return null;
        v = v.trim();
        return v.isEmpty() ? null : v;
    }

    private void applyCredits(ObjectNode target, JsonNode credits) {
        if (credits == null || !credits.isArray()) return;

        Set<String> writers = new LinkedHashSet<>();
        Set<String> artists = new LinkedHashSet<>();
        Set<String> coverArtists = new LinkedHashSet<>();

        for (JsonNode c : credits) {
            String name = text(c.get("creator"));
            if (name == null) continue;
            JsonNode roles = c.get("role");
            if (roles == null || !roles.isArray()) continue;
            for (JsonNode r : roles) {
                String roleName = text(r.get("name"));
                if (roleName == null) continue;
                String roleLower = roleName.toLowerCase();
                if (roleLower.contains("story") || roleLower.contains("script") || roleLower.contains("writer")) {
                    writers.add(name);
                }
                if (roleLower.contains("artist") || roleLower.contains("penciler") || roleLower.contains("inker")) {
                    artists.add(name);
                }
                if (roleLower.contains("cover")) {
                    coverArtists.add(name);
                }
            }
        }

        if (!writers.isEmpty()) target.put("writers", String.join(", ", writers));
        if (!artists.isEmpty()) target.put("artists", String.join(", ", artists));
        if (!coverArtists.isEmpty()) target.put("cover_artists", String.join(", ", coverArtists));
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
}
