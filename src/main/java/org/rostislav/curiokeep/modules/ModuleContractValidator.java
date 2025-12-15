package org.rostislav.curiokeep.modules;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ModuleContractValidator {

    private final JsonSchema schema;

    public ModuleContractValidator() {
        try (InputStream in = new ClassPathResource("docs/spec/module-contract-v1.schema.json").getInputStream()) {
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
            this.schema = factory.getSchema(in);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load module contract JSON schema", e);
        }
    }

    public void validate(JsonNode moduleContractJson) {
        Set<ValidationMessage> errors = schema.validate(moduleContractJson);
        if (!errors.isEmpty()) {
            String msg = errors.stream()
                    .map(ValidationMessage::getMessage)
                    .sorted()
                    .collect(Collectors.joining("\n - ", "Module contract invalid:\n - ", ""));
            throw new IllegalStateException(msg);
        }
    }
}
