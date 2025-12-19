package org.rostislav.curiokeep.providers.impl;

import org.rostislav.curiokeep.items.entities.ItemIdentifierEntity;
import org.rostislav.curiokeep.providers.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class BoardGameGeekProvider implements MetadataProvider {

    private static final Logger log = LoggerFactory.getLogger(BoardGameGeekProvider.class);
    private static final String USER_AGENT = "CurioKeep/1.0 (+https://github.com/RoastSlav/CurioKeep)";
    private static final List<ProviderCredentialField> CREDENTIAL_FIELDS = List.of(
            ProviderCredentialField.secret("token", "Bearer token", "BoardGameGeek API bearer token")
    );

    private final RestClient http;
    private final ObjectMapper objectMapper;
    private final ProviderCredentialLookup credentialLookup;

    public BoardGameGeekProvider(RestClient http, ObjectMapper objectMapper, ProviderCredentialLookup credentialLookup) {
        this.http = http;
        this.objectMapper = objectMapper;
        this.credentialLookup = credentialLookup;
    }

    @Override
    public String key() {
        return "boardgamegeek";
    }

    @Override
    public List<ProviderCredentialField> credentialFields() {
        return CREDENTIAL_FIELDS;
    }

    @Override
    public boolean supports(ItemIdentifierEntity.IdType idType) {
        return idType == ItemIdentifierEntity.IdType.CUSTOM;
    }

    @Override
    public Optional<ProviderResult> fetch(ItemIdentifierEntity.IdType idType, String idValue) {
        if (idValue == null || idValue.isBlank()) return Optional.empty();
        String trimmed = idValue.trim();
        if (!trimmed.matches("\\d+")) return Optional.empty();

        Optional<ProviderCredential> stored = credentialLookup.getCredentials(key());
        if (stored.isEmpty()) {
            log.debug("boardgamegeek bearer token not configured; skipping lookup id={}", trimmed);
            return Optional.empty();
        }
        String token = stored.get().values().get("token");
        if (token == null || token.isBlank()) {
            log.debug("boardgamegeek bearer token empty; skipping lookup id={}", trimmed);
            return Optional.empty();
        }
        String bearerToken = token.trim();
        if (bearerToken.isEmpty()) {
            log.debug("boardgamegeek bearer token empty after trim; skipping lookup id={}", trimmed);
            return Optional.empty();
        }

        try {
            ResponseEntity<String> response = http.get()
                    .uri("https://boardgamegeek.com/xmlapi2/thing?id={id}&stats=1", trimmed)
                    .header("User-Agent", USER_AGENT)
                    .header("Authorization", "Bearer " + bearerToken)
                    .retrieve()
                    .toEntity(String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("bgg lookup failed: status={} id={}", response.getStatusCode(), trimmed);
                return Optional.empty();
            }

            String body = response.getBody();
            if (body == null || body.isBlank()) return Optional.empty();

            Element item = extractItem(body);
            if (item == null) return Optional.empty();

            ObjectNode normalized = objectMapper.createObjectNode();

            String title = primaryName(item);
            if (title != null) normalized.put("title", title);

            Integer year = attrInt(item, "yearpublished", "value");
            if (year != null) normalized.put("published_year", year);

            Integer minPlayers = attrInt(item, "minplayers", "value");
            if (minPlayers != null) normalized.put("min_players", minPlayers);

            Integer maxPlayers = attrInt(item, "maxplayers", "value");
            if (maxPlayers != null) normalized.put("max_players", maxPlayers);

            Integer playTime = attrInt(item, "playingtime", "value");
            if (playTime != null) normalized.put("playing_time", playTime);

            Integer minAge = attrInt(item, "minage", "value");
            if (minAge != null) normalized.put("min_age", minAge);

            String publisher = firstLink(item, "boardgamepublisher");
            if (publisher != null) normalized.put("publisher", publisher);

            normalized.put("bgg_id", trimmed);
            normalized.put("canonical_url", "https://boardgamegeek.com/boardgame/" + trimmed);

            List<ProviderAsset> assets = new ArrayList<>();
            String image = textContent(item, "image");
            if (image != null) assets.add(new ProviderAsset(AssetType.COVER, URI.create(image), null, null));
            String thumb = textContent(item, "thumbnail");
            if (thumb != null) assets.add(new ProviderAsset(AssetType.THUMBNAIL, URI.create(thumb), null, null));

            ProviderConfidence confidence = new ProviderConfidence(70, "BoardGameGeek thing lookup");

            return Optional.of(new ProviderResult(
                    key(),
                    Map.of("xml", body),
                    Map.of("json", normalized.toString()),
                    assets,
                    confidence
            ));
        } catch (Exception e) {
            log.warn("bgg lookup failed id={} error={}", idValue, e.getMessage());
            return Optional.empty();
        }
    }

    private Element extractItem(String xml) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            Document doc = dbf.newDocumentBuilder().parse(new org.xml.sax.InputSource(new StringReader(xml)));
            NodeList items = doc.getElementsByTagName("item");
            if (items.getLength() == 0) return null;
            return (Element) items.item(0);
        } catch (Exception e) {
            log.warn("bgg parse failed: {}", e.getMessage());
            return null;
        }
    }

    private String primaryName(Element item) {
        NodeList names = item.getElementsByTagName("name");
        for (int i = 0; i < names.getLength(); i++) {
            Element el = (Element) names.item(i);
            String type = el.getAttribute("type");
            if ("primary".equalsIgnoreCase(type) || type.isBlank()) {
                String v = el.getAttribute("value");
                if (v != null && !v.isBlank()) return v;
            }
        }
        return null;
    }

    private Integer attrInt(Element parent, String tag, String attr) {
        NodeList list = parent.getElementsByTagName(tag);
        if (list.getLength() == 0) return null;
        Element el = (Element) list.item(0);
        String v = el.getAttribute(attr);
        try {
            return (v == null || v.isBlank()) ? null : Integer.parseInt(v);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String firstLink(Element parent, String type) {
        NodeList links = parent.getElementsByTagName("link");
        for (int i = 0; i < links.getLength(); i++) {
            Element link = (Element) links.item(i);
            if (type.equals(link.getAttribute("type"))) {
                String v = link.getAttribute("value");
                if (v != null && !v.isBlank()) return v;
            }
        }
        return null;
    }

    private String textContent(Element parent, String tag) {
        NodeList list = parent.getElementsByTagName(tag);
        if (list.getLength() == 0) return null;
        String v = list.item(0).getTextContent();
        if (v == null) return null;
        v = v.trim();
        return v.isEmpty() ? null : v;
    }
}
