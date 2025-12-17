package org.rostislav.curiokeep.providers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;
import tools.jackson.core.JsonProcessingException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
public class ProviderCredentialService implements ProviderCredentialLookup {

    private static final Logger log = LoggerFactory.getLogger(ProviderCredentialService.class);
    private static final TypeReference<LinkedHashMap<String, String>> MAP_TYPE = new TypeReference<>() {
    };

    private final ProviderCredentialRepository repository;
    private final ObjectMapper objectMapper;
    private final TextEncryptor encryptor;
    private final ConcurrentMap<String, ProviderCredential> cache = new ConcurrentHashMap<>();

    public ProviderCredentialService(ProviderCredentialRepository repository,
                                     ObjectMapper objectMapper,
                                     @Value("${curiokeep.providers.credentials.encryption.password:changeme}") String password,
                                     @Value("${curiokeep.providers.credentials.encryption.salt:changeme-salt}") String salt) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.encryptor = Encryptors.text(password, salt);
        loadExistingCredentials();
    }

    private void loadExistingCredentials() {
        repository.findAll().forEach(entity -> decode(entity).ifPresent(credential -> cache.put(entity.getProviderKey(), credential)));
    }

    @Override
    public Optional<ProviderCredential> getCredentials(String providerKey) {
        ProviderCredential cached = cache.get(providerKey);
        if (cached != null) {
            return Optional.of(cached);
        }
        return repository.findById(providerKey)
                .flatMap(this::decode)
                .map(credential -> {
                    cache.put(providerKey, credential);
                    return credential;
                });
    }

    public boolean hasCredentials(String providerKey, List<ProviderCredentialField> requiredFields) {
        if (requiredFields == null || requiredFields.isEmpty()) {
            return true;
        }
        return getCredentials(providerKey).map(credential -> {
            Map<String, String> values = credential.values();
            return requiredFields.stream().allMatch(field -> {
                String v = values.get(field.name());
                return v != null && !v.isBlank();
            });
        }).orElse(false);
    }

    public ProviderCredential saveCredentials(String providerKey, Map<String, String> values) {
        Map<String, String> normalized = normalize(values);
        String payload;
        try {
            payload = objectMapper.writeValueAsString(normalized);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize provider credentials", e);
        }
        String encrypted = encryptor.encrypt(payload);
        ProviderCredentialEntity entity = repository.findById(providerKey)
                .orElse(new ProviderCredentialEntity(providerKey, encrypted));
        entity.setEncryptedPayload(encrypted);
        ProviderCredentialEntity saved = repository.save(entity);
        ProviderCredential credential = new ProviderCredential(saved.getProviderKey(), normalized, saved.getUpdatedAt());
        cache.put(providerKey, credential);
        return credential;
    }

    public void deleteCredentials(String providerKey) {
        repository.deleteById(providerKey);
        cache.remove(providerKey);
    }

    private Optional<ProviderCredential> decode(ProviderCredentialEntity entity) {
        try {
            String json = encryptor.decrypt(entity.getEncryptedPayload());
            LinkedHashMap<String, String> values = objectMapper.readValue(json, MAP_TYPE);
            Map<String, String> normalized = values.entrySet().stream()
                    .filter(entry -> entry.getKey() != null)
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue(), (a, b) -> b, LinkedHashMap::new));
            return Optional.of(new ProviderCredential(entity.getProviderKey(), Map.copyOf(normalized), entity.getUpdatedAt()));
        } catch (Exception e) {
            log.warn("Failed to decrypt provider credentials for {}: {}", entity.getProviderKey(), e.getMessage());
            return Optional.empty();
        }
    }

    private Map<String, String> normalize(Map<String, String> values) {
        return values.entrySet().stream()
                .filter(entry -> entry.getKey() != null && !entry.getKey().isBlank())
                .collect(Collectors.toMap(
                        entry -> entry.getKey().trim(),
                        entry -> entry.getValue() == null ? "" : entry.getValue().trim(),
                        (a, b) -> b,
                        LinkedHashMap::new
                ));
    }
}
