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
public class GoogleBooksProvider implements MetadataProvider {

    private static final Logger log = LoggerFactory.getLogger(GoogleBooksProvider.class);

    private final RestClient http;
    private final ObjectMapper objectMapper;

    public GoogleBooksProvider(RestClient http, ObjectMapper objectMapper) {
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

    private static String text(JsonNode n) {
        if (n == null || n.isNull() || n.isMissingNode()) return null;
        return n.asText(null);
    }

    private static void putPublishedYear(ObjectNode out, JsonNode publishedDate) {
        if (publishedDate == null || !publishedDate.isTextual()) return;
        String s = publishedDate.asText();
        var m = java.util.regex.Pattern.compile("(\\d{4})").matcher(s);
        if (m.find()) out.put("published_year", Integer.parseInt(m.group(1)));
    }

    @Override
    public String key() {
        return "googlebooks";
    }

    @Override
    public boolean supports(ItemIdentifierEntity.IdType idType) {
        return idType == ItemIdentifierEntity.IdType.ISBN10 || idType == ItemIdentifierEntity.IdType.ISBN13;
    }

    @Override
    public Optional<ProviderResult> fetch(ItemIdentifierEntity.IdType idType, String idValue) {
        String isbn = normalizeIsbn(idValue);
        if (isbn == null) return Optional.empty();

        ResponseEntity<String> response = http.get()
                .uri("https://www.googleapis.com/books/v1/volumes?q=isbn:{isbn}", isbn)
                .retrieve()
                .toEntity(String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            log.warn("googlebooks lookup failed: status={} isbn={}", response.getStatusCode(), isbn);
            return Optional.empty();
        }

        String body = response.getBody();
        if (body == null || body.isBlank()) {
            log.warn("googlebooks lookup empty body isbn={}", isbn);
            return Optional.empty();
        }

        JsonNode rootNode;
        try {
            rootNode = objectMapper.readTree(body);
        } catch (JacksonException e) {
            log.warn("googlebooks lookup invalid JSON isbn={} error={}", isbn, e.getMessage());
            return Optional.empty();
        }

        if (!rootNode.has("items") || !rootNode.get("items").isArray() || rootNode.get("items").isEmpty()) {
            return Optional.empty();
        }

        JsonNode item0 = rootNode.get("items").get(0);
        JsonNode info = item0.get("volumeInfo");
        if (info == null || info.isNull()) return Optional.empty();

        ObjectNode normalized = JsonNodeFactory.instance.objectNode();

        putText(normalized, "title", info.get("title"));
        putText(normalized, "subtitle", info.get("subtitle"));

        // authors array -> join comma
        if (info.has("authors") && info.get("authors").isArray()) {
            List<String> authors = new ArrayList<>();
            for (JsonNode a : info.get("authors")) if (a.isTextual()) authors.add(a.asText());
            if (!authors.isEmpty()) normalized.put("authors", String.join(", ", authors));
        }

        putText(normalized, "publisher", info.get("publisher"));
        putPublishedYear(normalized, info.get("publishedDate"));
        if (info.hasNonNull("pageCount")) normalized.put("pages", info.get("pageCount").asInt());

        // language like "en"
        putText(normalized, "language", info.get("language"));

        // identifiers (industryIdentifiers)
        if (info.has("industryIdentifiers") && info.get("industryIdentifiers").isArray()) {
            for (JsonNode ii : info.get("industryIdentifiers")) {
                String type = text(ii.get("type"));
                String val = text(ii.get("identifier"));
                if (type == null || val == null) continue;
                if ("ISBN_10".equals(type)) normalized.put("isbn10", val);
                if ("ISBN_13".equals(type)) normalized.put("isbn13", val);
            }
        } else {
            // fallback
            if (idType == ItemIdentifierEntity.IdType.ISBN10) normalized.put("isbn10", isbn);
            if (idType == ItemIdentifierEntity.IdType.ISBN13) normalized.put("isbn13", isbn);
        }

        // image links
        List<ProviderAsset> assets = new ArrayList<>();
        JsonNode imageLinks = info.get("imageLinks");
        if (imageLinks != null && imageLinks.isObject()) {
            String thumb = text(imageLinks.get("thumbnail"));
            String small = text(imageLinks.get("smallThumbnail"));

            if (thumb != null) assets.add(new ProviderAsset(AssetType.COVER, URI.create(thumb), null, null));
            if (small != null) assets.add(new ProviderAsset(AssetType.THUMBNAIL, URI.create(small), null, null));
        }

        ProviderConfidence conf = new ProviderConfidence(90, "GoogleBooks ISBN match");

        return Optional.of(new ProviderResult(
                key(),
                Map.of("json", item0.toString()),
                Map.of("json", normalized.toString()),
                assets,
                conf
        ));
    }
}