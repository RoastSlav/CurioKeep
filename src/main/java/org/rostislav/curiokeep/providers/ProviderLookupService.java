package org.rostislav.curiokeep.providers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.rostislav.curiokeep.items.entities.ItemIdentifierEntity;
import org.rostislav.curiokeep.modules.entities.ModuleDefinitionEntity;
import org.rostislav.curiokeep.modules.entities.ModuleFieldEntity;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Service
public class ProviderLookupService {

    private final ProviderRegistry registry;
    private final ProviderFieldMapper mapper;
    private final ObjectMapper objectMapper;

    public ProviderLookupService(ProviderRegistry registry, ProviderFieldMapper mapper) {
        this.registry = registry;
        this.mapper = mapper;
        this.objectMapper = JsonMapper.builder()
                .defaultPropertyInclusion(JsonInclude.Value.construct(NON_NULL, NON_NULL))
                .findAndAddModules()
                .build();
    }

    public LookupResponse lookup(ModuleDefinitionEntity module, List<ItemIdentifierEntity> identifiers) {

        List<ModuleProviderSpec> providerSpecs = ModuleProviderSpec.fromModule(module)
                .stream()
                .filter(ModuleProviderSpec::enabled)
                .toList();

        List<ProviderResult> results = new ArrayList<>();

        for (ModuleProviderSpec spec : providerSpecs) {
            registry.get(spec.key()).ifPresent(provider -> {
                for (ItemIdentifierEntity id : identifiers) {
                    if (!provider.supports(id.getIdType())) continue;
                    provider.fetch(id.getIdType(), id.getIdValue()).ifPresent(results::add);
                }
            });
        }

        // Merge mapped attributes: higher priority should win -> apply in priority order and only fill missing
        Map<String, Object> merged = new LinkedHashMap<>();
        List<ProviderAsset> assets = new ArrayList<>();

        // if you have module.getFields() as a collection of ModuleFieldEntity, use it:
        Iterable<ModuleFieldEntity> fields = module.getFields();

        for (ProviderResult r : results) {
            // If you want to map via providerMappings from module fields:
            Map<String, Object> mapped = mapper.mapFields(objectMapper.valueToTree(r.rawData()), fields, r.providerKey());
            for (var e : mapped.entrySet()) merged.putIfAbsent(e.getKey(), e.getValue());

            assets.addAll(r.assets());
        }

        ProviderResult best = results.stream()
                .max(Comparator.comparingInt(x -> x.confidence() == null ? 0 : x.confidence().score()))
                .orElse(null);

        return new LookupResponse(results, best, merged, assets);
    }

    public record LookupResponse(
            List<ProviderResult> results,
            ProviderResult best,
            Map<String, Object> mergedAttributes,
            List<ProviderAsset> assets
    ) {
    }
}