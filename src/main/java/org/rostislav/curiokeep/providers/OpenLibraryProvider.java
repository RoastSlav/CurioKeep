package org.rostislav.curiokeep.providers;

import org.rostislav.curiokeep.items.entities.ItemIdentifierEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.util.*;

@Component
public class OpenLibraryProvider implements MetadataProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenLibraryProvider.class);

    private final RestClient http;
    private final ObjectMapper objectMapper;

    public OpenLibraryProvider(RestClient http, ObjectMapper objectMapper) {
        this.http = http;
        this.objectMapper = objectMapper;
    }

    private static String normalizeIsbn(String in) {
        if (in == null) return null;
        String v = in.replaceAll("[^0-9Xx]", "");
        if (v.length() == 10 || v.length() == 13) return v.toUpperCase(Locale.ROOT);
        return null;
    }

    private static void putText(ObjectNode out, String key, JsonNode n) {
        if (n != null && n.isTextual() && !n.asText().isBlank()) out.put(key, n.asText().trim());
    }

    private static void putPublishedYear(ObjectNode out, JsonNode publishDate) {
        if (publishDate == null || !publishDate.isTextual()) return;
        String s = publishDate.asText();
        // extract first 4-digit year
        var m = java.util.regex.Pattern.compile("(\\d{4})").matcher(s);
        if (m.find()) out.put("published_year", Integer.parseInt(m.group(1)));
    }

    private static Map<String, Object> jsonToMap(JsonNode node) {
        // simplest safe thing: store raw JSON string; you can improve later
        return Map.of("json", node == null ? null : node.toString());
    }

    @Override
    public String key() {
        return "openlibrary";
    }

    @Override
    public boolean supports(ItemIdentifierEntity.IdType idType) {
        return idType == ItemIdentifierEntity.IdType.ISBN10 || idType == ItemIdentifierEntity.IdType.ISBN13;
    }

    @Override
    public Optional<ProviderResult> fetch(ItemIdentifierEntity.IdType idType, String idValue) {
        String isbn = normalizeIsbn(idValue);
        if (isbn == null) return Optional.empty();

        try {
            ResponseEntity<String> response = http.get()
                    .uri("https://openlibrary.org/isbn/{isbn}.json", isbn)
                    .retrieve()
                    .toEntity(String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("openlibrary lookup failed: status={} isbn={}", response.getStatusCode(), isbn);
                return Optional.empty();
            }

            String raw = response.getBody();
            if (raw == null || raw.isBlank()) {
                log.warn("openlibrary lookup empty body isbn={}", isbn);
                return Optional.empty();
            }

            JsonNode rawNode = objectMapper.readTree(raw);

            if (rawNode.isMissingNode() || rawNode.isNull()) return Optional.empty();

            ObjectNode normalized = JsonNodeFactory.instance.objectNode();

            // title
            putText(normalized, "title", rawNode.get("title"));
            putText(normalized, "subtitle", rawNode.get("subtitle"));

            // publish
            putText(normalized, "publisher", rawNode.get("publishers") != null && rawNode.get("publishers").isArray() && rawNode.get("publishers").size() > 0
                    ? rawNode.get("publishers").get(0)
                    : rawNode.get("publishers"));
            putPublishedYear(normalized, rawNode.get("publish_date"));

            // pages
            if (rawNode.hasNonNull("number_of_pages")) normalized.put("pages", rawNode.get("number_of_pages").asInt());

            // language (OpenLibrary returns language keys sometimes)
            // keep it simple: if "languages" is array, take first key (like "/languages/eng")
            if (rawNode.has("languages") && rawNode.get("languages").isArray() && rawNode.get("languages").size() > 0) {
                JsonNode langKey = rawNode.get("languages").get(0).get("key");
                if (langKey != null && langKey.isTextual()) {
                    String v = langKey.asText();
                    normalized.put("language", v.substring(v.lastIndexOf('/') + 1));
                }
            }

            // identifiers
            if (idType == ItemIdentifierEntity.IdType.ISBN10) normalized.put("isbn10", isbn);
            if (idType == ItemIdentifierEntity.IdType.ISBN13) normalized.put("isbn13", isbn);

            // authors: OpenLibrary often requires a second call to /authors/{id}.json
            // Keep v1 simple: return empty if not resolved (you can add the extra call later)
            // You *can* still attempt it if you want:
            List<String> authors = tryResolveAuthors(rawNode);
            if (!authors.isEmpty()) normalized.put("authors", String.join(", ", authors));

            List<ProviderAsset> assets = List.of(
                    new ProviderAsset(AssetType.COVER, URI.create("https://covers.openlibrary.org/b/isbn/" + isbn + "-L.jpg"), null, null),
                    new ProviderAsset(AssetType.THUMBNAIL, URI.create("https://covers.openlibrary.org/b/isbn/" + isbn + "-M.jpg"), null, null)
            );

            ProviderConfidence conf = new ProviderConfidence(80, "OpenLibrary ISBN match");

            return Optional.of(new ProviderResult(
                    key(),
                    jsonToMap(rawNode),
                    jsonToMap(normalized),
                    assets,
                    conf
            ));

        } catch (JacksonException e) {
            log.warn("openlibrary lookup invalid JSON isbn={} error={}", isbn, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("openlibrary lookup failed isbn={} error={}", isbn, e.getMessage());
            return Optional.empty();
        }
    }

    private List<String> tryResolveAuthors(JsonNode raw) {
        if (!raw.has("authors") || !raw.get("authors").isArray()) return List.of();
        List<String> result = new ArrayList<>();
        for (JsonNode a : raw.get("authors")) {
            JsonNode keyNode = a.get("key");
            if (keyNode == null || !keyNode.isTextual()) continue;
            String key = keyNode.asText(); // "/authors/OL..."
            try {
                ResponseEntity<String> response = http.get()
                        .uri("https://openlibrary.org{key}.json", key)
                        .retrieve()
                        .toEntity(String.class);

                if (!response.getStatusCode().is2xxSuccessful()) {
                    continue;
                }

                String body = response.getBody();
                if (body == null || body.isBlank()) continue;

                JsonNode authorJson = objectMapper.readTree(body);

                if (authorJson.hasNonNull("name")) {
                    result.add(authorJson.get("name").asText());
                }
            } catch (Exception ignored) {
            }
        }
        return result;
    }
}