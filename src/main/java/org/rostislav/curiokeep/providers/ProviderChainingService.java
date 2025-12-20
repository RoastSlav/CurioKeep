package org.rostislav.curiokeep.providers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Encapsulates post-fetch chaining rules between providers (e.g., hydrate ComicVine when Metron references it).
 */
public class ProviderChainingService {

    private static final Logger log = LoggerFactory.getLogger(ProviderChainingService.class);

    private final ObjectMapper objectMapper;
    private final ProviderRegistry registry;

    public ProviderChainingService(ObjectMapper objectMapper, ProviderRegistry registry) {
        this.objectMapper = objectMapper;
        this.registry = registry;
    }

    /**
     * Apply chaining rules for a single provider result and append any derived results to the provided list.
     */
    public void applyChains(ModuleProviderSpec spec,
                            ProviderResult pr,
                            boolean comicvineEnabled,
                            List<ProviderResult> results) {

        // If Metron returned a ComicVine reference and ComicVine is enabled, hydrate via ComicVine
        if (comicvineEnabled && "metron".equals(spec.key())) {
            String cvId = extractComicvineId(pr);
            if (cvId != null) {
                registry.get("comicvine").ifPresent(cv -> {
                    try {
                        cv.fetch(org.rostislav.curiokeep.items.entities.ItemIdentifierEntity.IdType.CUSTOM, cvId)
                                .ifPresent(results::add);
                    } catch (Exception ex) {
                        log.warn("ComicVine hydrate failed for {}: {}", cvId, ex.getMessage());
                    }
                });
            }
        }
    }

    private String extractComicvineId(ProviderResult r) {
        JsonNode n = safeNormalizedNode(r);
        String cv = asText(n.at("/comicvine_id"));
        if (cv == null) {
            cv = asText(n.at("/cv_id"));
        }
        if (cv == null) return null;
        if (cv.startsWith("4000-")) return cv;
        if (cv.matches("\\d+")) return "4000-" + cv;
        return null;
    }

    private JsonNode safeNormalizedNode(ProviderResult r) {
        Object nf = r.normalizedFields();
        try {
            if (nf instanceof java.util.Map<?, ?> m) {
                if (m.size() == 1 && m.containsKey("json") && m.get("json") instanceof String s) {
                    return objectMapper.readTree(s);
                }
                return objectMapper.valueToTree(m);
            }
            if (nf instanceof String s) {
                return objectMapper.readTree(s);
            }
            return objectMapper.valueToTree(nf);
        } catch (JacksonException ex) {
            log.warn("Failed to parse normalizedFields from provider {}: {}", r.providerKey(), ex.getMessage());
            return objectMapper.createObjectNode();
        }
    }

    private String asText(JsonNode n) {
        if (n == null || n.isMissingNode() || n.isNull()) return null;
        String v = n.asText(null);
        if (v == null) return null;
        v = v.trim();
        return v.isEmpty() ? null : v;
    }
}