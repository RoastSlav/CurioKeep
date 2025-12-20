package org.rostislav.curiokeep.providers;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

@Schema(description = "Definition of a credential field required by a metadata provider")
public record ProviderCredentialField(
        @Schema(description = "Internal name used to store this value") String name,
        @Schema(description = "Label presented to administrators") String label,
        @Schema(description = "Short description of what to enter") String description,
        @Schema(description = "True when the value is a secret and should be masked") boolean secret,
        @Schema(description = "True when the field must be provided") boolean required
) {
    public ProviderCredentialField {
        Objects.requireNonNull(name, "name is required");
        label = (label == null || label.isBlank()) ? humanize(name) : label;
        description = description == null ? "" : description;
        // required is passed through; defaults applied by factory methods
    }

    public static ProviderCredentialField text(String name, String label, String description) {
        return new ProviderCredentialField(name, label, description, false, true);
    }

    public static ProviderCredentialField text(String name, String label, String description, boolean required) {
        return new ProviderCredentialField(name, label, description, false, required);
    }

    public static ProviderCredentialField secret(String name, String label, String description) {
        return new ProviderCredentialField(name, label, description, true, true);
    }

    public static ProviderCredentialField secret(String name, String label, String description, boolean required) {
        return new ProviderCredentialField(name, label, description, true, required);
    }

    private static String humanize(String input) {
        if (input == null || input.isBlank()) return "Field";
        String cleaned = input.replace('_', ' ').replace('-', ' ');
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String part : cleaned.split("\\s+")) {
            if (part.isEmpty()) continue;
            if (!first) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) sb.append(part.substring(1));
            first = false;
        }
        return sb.toString();
    }
}
