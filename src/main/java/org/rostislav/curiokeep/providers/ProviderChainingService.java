package org.rostislav.curiokeep.providers;

import org.rostislav.curiokeep.items.entities.ItemIdentifierEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Follows the chains a module declares on its providers: once a provider has returned a result, one of that result's normalized
 * fields is looked up as an identifier in another provider of the module, and that result is followed in turn.
 * <p>
 * Work per lookup is bounded: a provider is asked for a given identifier at most once, and chains are followed at most
 * {@value #MAX_DEPTH} hops deep. A hop that fails is logged and skipped; it never fails the lookup.
 */
public class ProviderChainingService {

    static final int MAX_DEPTH = 3;
    private static final Logger log = LoggerFactory.getLogger(ProviderChainingService.class);

    private final ObjectMapper objectMapper;
    private final ProviderRegistry registry;

    public ProviderChainingService(ObjectMapper objectMapper, ProviderRegistry registry) {
        this.objectMapper = objectMapper;
        this.registry = registry;
    }

    /** The key under which a provider's lookup of one identifier is recorded in {@code asked}. */
    static String askedKey(String providerKey, ItemIdentifierEntity.IdType idType, String idValue) {
        return providerKey + "/" + idType + "/" + idValue;
    }

    /**
     * Follows the chains of {@code spec} for {@code result} and appends every derived result to {@code results}.
     *
     * @param active the module's enabled providers, by key, that a chain may target
     * @param asked  the lookups already made in this request, updated with those made here
     */
    public void follow(ModuleProviderSpec spec, ProviderResult result, Map<String, ModuleProviderSpec> active,
                       Set<String> asked, List<ProviderResult> results) {
        follow(spec, result, active, asked, results, 1);
    }

    private void follow(ModuleProviderSpec spec, ProviderResult result, Map<String, ModuleProviderSpec> active,
                        Set<String> asked, List<ProviderResult> results, int depth) {
        if (depth > MAX_DEPTH || spec.chains().isEmpty()) return;
        JsonNode normalized = NormalizedFields.of(objectMapper, result);

        for (ModuleProviderSpec.Chain chain : spec.chains()) {
            ModuleProviderSpec target = active.get(chain.to());
            String idValue = text(normalized.get(chain.from()));
            if (target == null || idValue == null) continue;
            Optional<MetadataProvider> provider = registry.get(target.key()).filter(p -> p.supports(chain.idType()));
            if (provider.isEmpty() || !asked.add(askedKey(target.key(), chain.idType(), idValue))) continue;
            try {
                provider.get().fetch(chain.idType(), idValue).ifPresent(chained -> {
                    results.add(chained);
                    follow(target, chained, active, asked, results, depth + 1);
                });
            } catch (Exception ex) {
                log.warn("Chained lookup from {} to {} failed: {}", spec.key(), target.key(), ex.getMessage());
            }
        }
    }

    private static String text(JsonNode value) {
        if (value == null || !value.isValueNode() || value.isNull()) return null;
        String text = value.asString(null);
        return text == null || text.isBlank() ? null : text.trim();
    }
}
