package org.rostislav.curiokeep.providers;

import org.rostislav.curiokeep.items.entities.ItemIdentifierEntity;
import org.rostislav.curiokeep.providers.api.dto.ProviderStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

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
    private final ProviderCredentialService credentialService;

    private final Map<String, ProviderStatusEntry> cache = new ConcurrentHashMap<>();

    public ProviderStatusService(ProviderRegistry registry,
                                 ProviderKnowledgeBase knowledgeBase,
                                 RestClient restClient,
                                 ProviderCredentialService credentialService) {
        this.registry = registry;
        this.knowledgeBase = knowledgeBase;
        this.restClient = restClient;
        this.credentialService = credentialService;
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
                    (int) Math.max(1, retryAfter),
                    rateLimited.credentialsRequired(),
                    rateLimited.credentialsConfigured()
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
        String target = resolveHealthCheckTarget(key, profile);

        boolean available = descriptor != null;
        String message;
        List<ProviderCredentialField> credentialFields = descriptor.credentialFields();
        boolean credentialsRequired = !credentialFields.isEmpty();
        boolean credentialsConfigured = credentialService.hasCredentials(key, credentialFields);

        if (credentialsRequired && !credentialsConfigured) {
            message = "Credentials not configured for this provider";
            available = false;
        } else if (target == null || target.contains("{")) {
            // Avoid calling templated URLs or missing endpoints; treat as not configured.
            message = "Health check not configured for this provider";
            available = false;
        } else {
            String safeTarget = normalizeTarget(key, target);
            try {
                restClient.get()
                    .uri(safeTarget)
                    .retrieve()
                    .toBodilessEntity();
                message = "Successfully contacted provider";
                available = true;
            } catch (RestClientResponseException ex) {
                String resp = ex.getResponseBodyAsString();
                int bodyLen = resp == null ? 0 : resp.length();
                log.debug("Provider health check failed for {}: status={} bodyLength={}", key, ex.getStatusCode(), bodyLen);
                message = "Health check failed: HTTP " + ex.getStatusCode().value();
                available = false;
            } catch (RestClientException ex) {
                log.debug("Provider health check failed for {}: {}", key, ex.getMessage());
                message = "Health check failed: API unreachable";
                available = false;
            } catch (Exception ex) {
                log.debug("Provider health check failed for {}: {}", key, ex.getMessage());
                message = "Health check failed: internal error";
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
                , credentialsRequired
                , credentialsConfigured
        );
        return new ProviderStatusEntry(response, Instant.now());
    }

    private String normalizeTarget(String key, String target) {
        // Certain providers require query parameters; supply safe defaults to avoid 400s.
        if (key.equals("googlebooks") && !target.contains("?")) {
            return target + "?q=ping";
        }
        return target;
    }

    private String resolveHealthCheckTarget(String key, ProviderProfile profile) {
        if (key.equals("internetarchive")) {
            return "https://archive.org/robots.txt";
        }
        if (key.equals("coverartarchive")) {
            return "https://coverartarchive.org/";
        }
        if (key.equals("openproduct")) {
            return "https://world.openfoodfacts.org/robots.txt";
        }

        if (profile == null) return null;
        if (profile.apiUrl() != null) return profile.apiUrl();
        return profile.websiteUrl();
    }

    private ProviderDescriptor descriptorFor(String key) {
        return registry.get(key)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown provider: " + key))
                .descriptor();
    }

    private record ProviderStatusEntry(ProviderStatusResponse response, Instant checkedAt) {}
}
