package org.rostislav.curiokeep.modules;

import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.stream.Collectors;

@Component
public class ModuleContractValidator {
    private final Schema schema;
    private final ObjectMapper objectMapper;

    public ModuleContractValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;

        try (InputStream in = new ClassPathResource("docs/spec/module-contract-v1.schema.json").getInputStream()) {
            JSONObject raw = new JSONObject(new JSONTokener(in));
            this.schema = SchemaLoader.builder()
                    .schemaJson(raw)
                    .draftV7Support() // schema file is V202012 compatible
                    .build()
                    .load()
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load module contract JSON schema", e);
        }
    }

    public void validate(JsonNode moduleContractJson) {
        try {
            JSONObject data = new JSONObject(objectMapper.writeValueAsString(moduleContractJson));
            schema.validate(data);
        } catch (org.everit.json.schema.ValidationException ve) {
            String msg = ve.getAllMessages().stream()
                    .sorted()
                    .collect(Collectors.joining("\n - ", "Module contract invalid:\n - ", ""));
            throw new IllegalStateException(msg, ve);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to validate module contract JSON", e);
        }
    }
}
