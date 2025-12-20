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
    private final ProviderChainingService chainingService;

    public ProviderLookupService(ProviderRegistry registry, ProviderFieldMapper mapper, ObjectMapper objectMapper) {
        this.registry = registry;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.chainingService = new ProviderChainingService(objectMapper, registry);
    }

    public LookupResponse lookup(ModuleDefinitionEntity module, List<ItemIdentifierEntity> identifiers) {
        return lookup(module, identifiers, List.of(), null);
    }

    public LookupResponse lookup(ModuleDefinitionEntity module, List<ItemIdentifierEntity> identifiers, List<String> providerFilter, String query) {

        List<ModuleProviderSpec> providerSpecs = ModuleProviderSpec.fromModule(module, objectMapper)
                .stream()
                .filter(ModuleProviderSpec::enabled)
                .filter(p -> providerFilter == null || providerFilter.isEmpty() || providerFilter.contains(p.key()))
                .toList();

        Map<String, Integer> priorityByProvider = new LinkedHashMap<>();
        for (ModuleProviderSpec spec : providerSpecs) {
            priorityByProvider.put(spec.key(), spec.priority());
        }

        List<ProviderResult> results = new ArrayList<>();

        boolean comicvineEnabled = providerSpecs.stream()
            .anyMatch(p -> "comicvine".equals(p.key()) && p.enabled());

        for (ModuleProviderSpec spec : providerSpecs) {
            registry.get(spec.key()).ifPresent(provider -> {
                for (ItemIdentifierEntity id : identifiers) {
                    if (!provider.supports(id.getIdType())) continue;
                    try {
                        provider.fetch(id.getIdType(), id.getIdValue()).ifPresent(pr -> {
                            results.add(pr);
                            chainingService.applyChains(spec, pr, comicvineEnabled, results);
                        });
                    } catch (Exception ex) {
                        log.warn("Provider {} failed for {}: {}", spec.key(), id.getIdValue(), ex.getMessage());
                    }
                }
            });
        }

        // Merge mapped attributes: higher priority should win -> apply in priority order and only fill missing
        Map<String, Object> merged = new LinkedHashMap<>();
        List<ProviderAsset> assets = new ArrayList<>();

        Iterable<ModuleFieldEntity> fields = module.getFields();

        record Candidate(ProviderResult result, int priority, int score) {}

        Comparator<Candidate> candidateComparator = Comparator
            .comparingInt(Candidate::priority).reversed()
            .thenComparing(Comparator.comparingInt(Candidate::score).reversed());

        List<Candidate> candidates = results.stream()
                .map(r -> new Candidate(
                        r,
                        priorityByProvider.getOrDefault(r.providerKey(), Integer.MAX_VALUE),
                        Optional.ofNullable(r.confidence()).map(c -> Optional.ofNullable(c.score()).orElse(0)).orElse(0)
                ))
                .sorted(candidateComparator)
                .toList();

        Map<String, Map<String, Object>> mappedCache = new LinkedHashMap<>();

        // Build map of providerKey -> best candidate for that provider (highest score / priority)
        Map<String, Candidate> candidateByProvider = new LinkedHashMap<>();
        for (Candidate c : candidates) {
            Candidate existing = candidateByProvider.get(c.result().providerKey());
            if (existing == null || candidateComparator.compare(c, existing) < 0) {
                // candidateComparator is reversed (higher priority/score first), so < 0 means better
                candidateByProvider.put(c.result().providerKey(), c);
            }
        }

        // Merge attributes using module-declared provider order: earlier providers fill
        // attributes first; later providers only fill missing values. This respects
        // the module author's preferred attribute sourcing while best-provider
        // selection is still based on priority/score.
        for (ModuleFieldEntity field : fields) {
            String key = field.getFieldKey();
            for (ModuleProviderSpec spec : providerSpecs) {
                Candidate candidate = candidateByProvider.get(spec.key());
                if (candidate == null) continue;
                Map<String, Object> mapped = mappedCache.computeIfAbsent(candidate.result().providerKey(), providerKey -> {
                    JsonNode normNode = safeNormalizedNode(candidate.result());
                    try {
                        return mapper.mapFields(normNode, fields, candidate.result().providerKey());
                    } catch (Exception ex) {
                        log.warn("Field mapping failed for provider {}: {}", candidate.result().providerKey(), ex.getMessage());
                        return Collections.emptyMap();
                    }
                });
                if (mapped.containsKey(key)) {
                    merged.put(key, mapped.get(key));
                    break;
                }
            }
        }

        // Collect assets in module-declared provider order (so module provider ordering
        // determines preferred asset ordering) but using the candidate results for each
        // provider. This ensures assets are ordered as module authors expect while
        // best provider selection still uses priority/score.
        for (ModuleProviderSpec spec : providerSpecs) {
            for (Candidate candidate : candidates) {
                if (candidate.result().providerKey().equals(spec.key())) {
                    if (candidate.result().assets() != null) {
                        assets.addAll(candidate.result().assets());
                    }
                }
            }
        }

        ProviderResult best = candidates.stream()
                .sorted(candidateComparator)
                .map(Candidate::result)
                .findFirst()
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