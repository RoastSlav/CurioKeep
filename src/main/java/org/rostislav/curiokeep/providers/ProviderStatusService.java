package org.rostislav.curiokeep.providers;

import org.springframework.stereotype.Service;
import org.rostislav.curiokeep.items.entities.ItemIdentifierEntity;
import org.rostislav.curiokeep.providers.api.dto.ProviderStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.rostislav.curiokeep.providers.ProviderDescriptor;
import org.rostislav.curiokeep.providers.ProviderProfile;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProviderStatusService {
    private static final Duration RATE_LIMIT = Duration.ofSeconds(30);

    private static final Logger log = LoggerFactory.getLogger(ProviderStatusService.class);

    private final ProviderRegistry registry;
    private final ProviderKnowledgeBase knowledgeBase;
    private final RestClient restClient;

    private final Map<String, ProviderStatusEntry> cache = new ConcurrentHashMap<>();

    public ProviderStatusService(ProviderRegistry registry,
                                 ProviderKnowledgeBase knowledgeBase,
                                 RestClient restClient) {
        this.registry = registry;
        this.knowledgeBase = knowledgeBase;
        this.restClient = restClient;
    }

    public ProviderStatusResponse getStatus(String key) {
        ProviderDescriptor descriptor = descriptorFor(key);
        return cache.computeIfAbsent(key, ignored -> buildStatusEntry(key, descriptor)).response();
    }

    public ProviderStatusResponse checkStatus(String key) {
        Instant now = Instant.now();
        ProviderStatusEntry existing = cache.get(key);
        if (existing != null) {
            Duration since = Duration.between(existing.checkedAt(), now);
            if (since.compareTo(RATE_LIMIT) < 0) {
                long retryAfter = RATE_LIMIT.minus(since).getSeconds();
                ProviderStatusResponse rateLimited = existing.response();
                return new ProviderStatusResponse(
                        rateLimited.key(),
                        rateLimited.available(),
                        "Rate limited – try again in " + retryAfter + "s",
                        rateLimited.supportedIdTypes(),
                        true,
                        (int) Math.max(1, retryAfter)
                );
            }
        }

        ProviderDescriptor descriptor = descriptorFor(key);
        ProviderStatusEntry entry = buildStatusEntry(key, descriptor);
        cache.put(key, entry);
        return entry.response();
    }

    private ProviderStatusEntry buildStatusEntry(String key, ProviderDescriptor descriptor) {
        List<ItemIdentifierEntity.IdType> ids = descriptor.supportedIdTypes();
        ProviderProfile profile = knowledgeBase.profileFor(key);
        String target = profile != null ? (profile.apiUrl() != null ? profile.apiUrl() : profile.websiteUrl()) : null;
        boolean available = descriptor != null;
        String message;
        if (target == null) {
            message = "No health endpoint configured";
        } else {
            try {
                restClient.get().uri(target).retrieve().toBodilessEntity();
                message = "Successfully contacted " + target;
                available = true;
            } catch (Exception ex) {
                log.debug("Provider health check failed for {}: {}", key, ex.getMessage());
                message = "Health call failed: " + ex.getMessage();
                available = false;
            }
        }
        ProviderStatusResponse response = new ProviderStatusResponse(
                key,
                available,
                message,
                ids == null ? Collections.emptyList() : ids,
                false,
                null
        );
        return new ProviderStatusEntry(response, Instant.now());
    }

    private ProviderDescriptor descriptorFor(String key) {
        return registry.get(key)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown provider: " + key))
                .descriptor();
    }

    private record ProviderStatusEntry(ProviderStatusResponse response, Instant checkedAt) {}
}
