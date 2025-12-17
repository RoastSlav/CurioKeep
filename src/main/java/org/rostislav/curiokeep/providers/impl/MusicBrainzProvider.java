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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Component
public class MusicBrainzProvider implements MetadataProvider {

    private static final Logger log = LoggerFactory.getLogger(MusicBrainzProvider.class);
    private static final String USER_AGENT = "CurioKeep/1.0 (+https://github.com/RoastSlav/CurioKeep)";

    private final RestClient http;
    private final ObjectMapper objectMapper;

    public MusicBrainzProvider(RestClient http, ObjectMapper objectMapper) {
        this.http = http;
        this.objectMapper = objectMapper;
    }

    @Override
    public String key() {
        return "musicbrainz";
    }

    @Override
    public boolean supports(ItemIdentifierEntity.IdType idType) {
        return idType == ItemIdentifierEntity.IdType.UPC || idType == ItemIdentifierEntity.IdType.EAN;
    }

    @Override
    public Optional<ProviderResult> fetch(ItemIdentifierEntity.IdType idType, String idValue) {
        String barcode = normalizeBarcode(idValue);
        if (barcode == null) return Optional.empty();

        try {
            ResponseEntity<String> response = http.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("musicbrainz.org")
                            .path("/ws/2/release/")
                            .queryParam("query", "barcode:" + barcode)
                            .queryParam("fmt", "json")
                            .queryParam("limit", "1")
                            .queryParam("inc", "artist-credits+release-groups+labels")
                            .build())
                    .header("User-Agent", USER_AGENT)
                    .retrieve()
                    .toEntity(String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("musicbrainz lookup failed: status={} barcode={}", response.getStatusCode(), barcode);
                return Optional.empty();
            }

            String body = response.getBody();
            if (body == null || body.isBlank()) {
                log.warn("musicbrainz lookup empty body barcode={}", barcode);
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(body);
            JsonNode releases = root.path("releases");
            if (!releases.isArray() || releases.size() == 0) return Optional.empty();

            JsonNode release = releases.get(0);
            ProviderResult result = buildResult(idType, barcode, release);
            if (result == null) return Optional.empty();

            return Optional.of(result);
        } catch (Exception e) {
            log.warn("musicbrainz lookup failed barcode={} error={}", idValue, e.getMessage());
            return Optional.empty();
        }
    }

    private ProviderResult buildResult(ItemIdentifierEntity.IdType idType, String barcode, JsonNode release) {
        ObjectNode normalized = objectMapper.createObjectNode();

        putText(normalized, "title", release.get("title"));

        String artists = artists(release.path("artist-credit"));
        if (artists != null) {
            normalized.put("artists", artists);
            normalized.put("authors", artists);
        }

        putText(normalized, "publisher", firstLabel(release.path("label-info")));
        putText(normalized, "country", release.get("country"));

        Integer year = year(release.get("date"));
        if (year != null) normalized.put("published_year", year);

        if (idType == ItemIdentifierEntity.IdType.UPC) normalized.put("upc", barcode);
        if (idType == ItemIdentifierEntity.IdType.EAN) normalized.put("ean", barcode);

        String mbid = text(release.get("id"));
        if (mbid != null) {
            normalized.put("musicbrainz_id", mbid);
            normalized.put("canonical_url", "https://musicbrainz.org/release/" + mbid);
        }

        List<ProviderAsset> assets = fetchCoverArt(mbid);

        ProviderConfidence confidence = new ProviderConfidence(85, "MusicBrainz barcode match");

        return new ProviderResult(
                key(),
                Map.of("json", release.toString()),
                Map.of("json", normalized.toString()),
                assets,
                confidence
        );
    }

    private List<ProviderAsset> fetchCoverArt(String mbid) {
        if (mbid == null || mbid.isBlank()) return List.of();

        try {
            ResponseEntity<String> response = http.get()
                    .uri("https://coverartarchive.org/release/{mbid}", mbid)
                    .header("User-Agent", USER_AGENT)
                    .retrieve()
                    .toEntity(String.class);

            if (!response.getStatusCode().is2xxSuccessful()) return List.of();
            String body = response.getBody();
            if (body == null || body.isBlank()) return List.of();

            JsonNode root = objectMapper.readTree(body);
            JsonNode images = root.path("images");
            if (!images.isArray() || images.size() == 0) return List.of();

            JsonNode picked = pickImage(images);
            if (picked == null) return List.of();

            List<ProviderAsset> out = new ArrayList<>();
            String full = text(picked.get("image"));
            if (full != null) out.add(new ProviderAsset(AssetType.COVER, URI.create(full), null, null));

            JsonNode thumbnails = picked.path("thumbnails");
            String large = text(thumbnails.get("large"));
            if (large != null) out.add(new ProviderAsset(AssetType.THUMBNAIL, URI.create(large), null, null));

            return out;
        } catch (Exception e) {
            log.warn("coverart lookup failed mbid={} error={}", mbid, e.getMessage());
            return List.of();
        }
    }

    private JsonNode pickImage(JsonNode images) {
        for (JsonNode img : images) {
            if (img.path("front").asBoolean(false)) return img;
        }
        return images.size() == 0 ? null : images.get(0);
    }

    private String artists(JsonNode artistCredit) {
        if (!artistCredit.isArray() || artistCredit.size() == 0) return null;
        StringBuilder sb = new StringBuilder();
        String nextJoin = null;
        for (JsonNode a : artistCredit) {
            String name = text(a.get("name"));
            if (name == null) continue;
            if (sb.length() > 0) sb.append(nextJoin == null ? ", " : nextJoin);
            sb.append(name);
            nextJoin = joinPhrase(a);
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private String joinPhrase(JsonNode credit) {
        JsonNode joinNode = credit.get("joinphrase");
        if (joinNode == null || joinNode.isNull() || joinNode.isMissingNode()) return ", ";
        String join = joinNode.asText();
        if (join == null || join.isBlank()) return ", ";
        return join;
    }

    private String firstLabel(JsonNode labelInfo) {
        if (!labelInfo.isArray() || labelInfo.size() == 0) return null;
        for (JsonNode li : labelInfo) {
            JsonNode label = li.get("label");
            if (label != null) {
                String name = text(label.get("name"));
                if (name != null) return name;
            }
        }
        return null;
    }

    private Integer year(JsonNode dateNode) {
        String date = text(dateNode);
        if (date == null) return null;
        var matcher = java.util.regex.Pattern.compile("^(\\d{4})").matcher(date);
        if (matcher.find()) return Integer.parseInt(matcher.group(1));
        return null;
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
