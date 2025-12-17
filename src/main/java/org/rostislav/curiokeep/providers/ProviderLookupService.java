package org.rostislav.curiokeep.providers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.rostislav.curiokeep.items.entities.ItemIdentifierEntity;
import org.rostislav.curiokeep.modules.entities.ModuleDefinitionEntity;
import org.rostislav.curiokeep.modules.entities.ModuleFieldEntity;
import org.springframework.stereotype.Service;
import org.rostislav.curiokeep.providers.api.dto.LookupResponse;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class ProviderLookupService {

    private static final Logger log = LoggerFactory.getLogger(ProviderLookupService.class);

    private final ProviderRegistry registry;
    private final ProviderFieldMapper mapper;
    private final ObjectMapper objectMapper;

    public ProviderLookupService(ProviderRegistry registry, ProviderFieldMapper mapper, ObjectMapper objectMapper) {
        this.registry = registry;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    public LookupResponse lookup(ModuleDefinitionEntity module, List<ItemIdentifierEntity> identifiers) {

        List<ModuleProviderSpec> providerSpecs = ModuleProviderSpec.fromModule(module, objectMapper)
                .stream()
                .filter(ModuleProviderSpec::enabled)
                .toList();

        List<ProviderResult> results = new ArrayList<>();

        for (ModuleProviderSpec spec : providerSpecs) {
            registry.get(spec.key()).ifPresent(provider -> {
                for (ItemIdentifierEntity id : identifiers) {
                    if (!provider.supports(id.getIdType())) continue;
                    try {
                        provider.fetch(id.getIdType(), id.getIdValue()).ifPresent(results::add);
                    } catch (Exception ex) {
                        log.warn("Provider {} failed for {}: {}", spec.key(), id.getIdValue(), ex.getMessage());
                    }
                }
            });
        }

        // Merge mapped attributes: higher priority should win -> apply in priority order and only fill missing
        Map<String, Object> merged = new LinkedHashMap<>();
        List<ProviderAsset> assets = new ArrayList<>();

        // if you have module.getFields() as a collection of ModuleFieldEntity, use it:
        Iterable<ModuleFieldEntity> fields = module.getFields();

        for (ProviderResult r : results) {
            JsonNode normNode = safeNormalizedNode(r);

            Map<String, Object> mapped = Collections.emptyMap();
            try {
                mapped = mapper.mapFields(normNode, fields, r.providerKey());
            } catch (Exception ex) {
                log.warn("Field mapping failed for provider {}: {}", r.providerKey(), ex.getMessage());
            }

            if (mapped != null) {
                for (var e : mapped.entrySet()) merged.putIfAbsent(e.getKey(), e.getValue());
            }

            if (r.assets() != null) {
                assets.addAll(r.assets());
            }
        }

        ProviderResult best = results.stream()
                .max(Comparator.comparingInt(x -> x.confidence() == null ? 0 : x.confidence().score()))
                .orElse(null);

        List<ProviderAsset> uniqueAssets = dedupeAssets(assets);

        return new LookupResponse(results, best, merged, uniqueAssets);
    }

    private JsonNode safeNormalizedNode(ProviderResult r) {
        Object nf = r.normalizedFields();
        try {
            if (nf instanceof Map<?, ?> m) {
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

    private List<ProviderAsset> dedupeAssets(List<ProviderAsset> assets) {
        if (assets == null || assets.isEmpty()) {
            return List.of();
        }
        try {
            return new ArrayList<>(new LinkedHashSet<>(assets));
        } catch (Exception ignored) {
            Set<String> seen = new HashSet<>();
            List<ProviderAsset> out = new ArrayList<>();
            for (ProviderAsset a : assets) {
                String key = a == null ? "null" : String.valueOf(a.url());
                if (seen.add(key)) {
                    out.add(a);
                }
            }
            return out;
        }
    }

    
}