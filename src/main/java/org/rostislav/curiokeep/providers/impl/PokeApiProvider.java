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
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class PokeApiProvider implements MetadataProvider {

    private static final Logger log = LoggerFactory.getLogger(PokeApiProvider.class);
    private static final String USER_AGENT = "CurioKeep/1.0 (+https://github.com/RoastSlav/CurioKeep)";

    private final RestClient http;
    private final ObjectMapper objectMapper;

    public PokeApiProvider(RestClient http, ObjectMapper objectMapper) {
        this.http = http;
        this.objectMapper = objectMapper;
    }

    @Override
    public String key() {
        return "pokeapi";
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
                    .uri("https://pokeapi.co/api/v2/pokemon/" + id)
                    .header("User-Agent", USER_AGENT)
                    .retrieve()
                    .toEntity(String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("pokeapi lookup failed status={} id={}", response.getStatusCode(), id);
                return Optional.empty();
            }

            String body = response.getBody();
            if (body == null || body.isBlank()) return Optional.empty();

            JsonNode root = objectMapper.readTree(body);
            ObjectNode normalized = objectMapper.createObjectNode();

            putText(normalized, "title", root.get("name"));
            normalized.put("pokedex_number", root.path("id").asInt());
            normalized.put("pokeapi_id", id);
            normalized.put("canonical_url", "https://pokeapi.co/api/v2/pokemon/" + id);

            ArrayNode types = objectMapper.createArrayNode();
            JsonNode typeNodes = root.get("types");
            if (typeNodes != null && typeNodes.isArray()) {
                for (JsonNode t : typeNodes) {
                    String name = text(t.path("type").get("name"));
                    if (name != null) types.add(name);
                }
            }
            if (!types.isEmpty()) normalized.set("types", types);

            JsonNode abilities = root.get("abilities");
            ArrayNode abilityNames = objectMapper.createArrayNode();
            if (abilities != null && abilities.isArray()) {
                for (JsonNode a : abilities) {
                    String name = text(a.path("ability").get("name"));
                    if (name != null) abilityNames.add(name);
                }
            }
            if (!abilityNames.isEmpty()) normalized.set("abilities", abilityNames);

            if (root.has("weight")) normalized.put("weight", root.get("weight").asInt());

            List<ProviderAsset> assets = new ArrayList<>();
            JsonNode sprites = root.get("sprites");
            if (sprites != null && sprites.isObject()) {
                String front = text(sprites.get("front_default"));
                if (front != null) assets.add(new ProviderAsset(AssetType.THUMBNAIL, URI.create(front), null, null));
                String back = text(sprites.get("back_default"));
                if (back != null) assets.add(new ProviderAsset(AssetType.THUMBNAIL, URI.create(back), null, null));
            }

            ProviderConfidence confidence = new ProviderConfidence(65, "PokéAPI match");

            return Optional.of(new ProviderResult(
                    key(),
                    Map.of("json", body),
                    Map.of("json", normalized.toString()),
                    assets,
                    confidence
            ));
        } catch (Exception e) {
            log.warn("pokeapi lookup failed id={} error={}", idValue, e.getMessage());
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
        if (!v.matches("[a-zA-Z0-9-]+")) return null;
        return v;
    }
}
