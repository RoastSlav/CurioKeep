package org.rostislav.curiokeep.providers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

/** Reads a provider result's normalized fields as JSON, whichever shape the provider stored them in. */
final class NormalizedFields {

    private static final Logger log = LoggerFactory.getLogger(NormalizedFields.class);

    private NormalizedFields() {
    }

    /** An empty object when the fields cannot be read, so callers can look keys up without checking. */
    static JsonNode of(ObjectMapper objectMapper, ProviderResult result) {
        Object fields = result.normalizedFields();
        try {
            if (fields instanceof Map<?, ?> map) {
                if (map.size() == 1 && map.containsKey("json") && map.get("json") instanceof String json) {
                    return objectMapper.readTree(json);
                }
                return objectMapper.valueToTree(map);
            }
            if (fields instanceof String json) {
                return objectMapper.readTree(json);
            }
            return objectMapper.valueToTree(fields);
        } catch (JacksonException ex) {
            log.warn("Failed to parse normalizedFields from provider {}: {}", result.providerKey(), ex.getMessage());
            return objectMapper.createObjectNode();
        }
    }
}
