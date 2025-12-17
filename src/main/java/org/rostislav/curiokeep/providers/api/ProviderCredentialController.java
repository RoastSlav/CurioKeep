package org.rostislav.curiokeep.providers.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.rostislav.curiokeep.providers.ProviderCredentialField;
import org.rostislav.curiokeep.providers.ProviderCredentialService;
import org.rostislav.curiokeep.providers.ProviderDescriptor;
import org.rostislav.curiokeep.providers.ProviderRegistry;
import org.rostislav.curiokeep.providers.api.dto.ProviderCredentialStatusResponse;
import org.rostislav.curiokeep.providers.api.dto.UpdateProviderCredentialsRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

@SecurityRequirement(name = "sessionAuth")
@RestController
@RequestMapping("/api/providers/{key}/credentials")
@PreAuthorize("hasAuthority('APP_ADMIN')")
public class ProviderCredentialController {

    private final ProviderRegistry registry;
    private final ProviderCredentialService credentialService;

    public ProviderCredentialController(ProviderRegistry registry, ProviderCredentialService credentialService) {
        this.registry = registry;
        this.credentialService = credentialService;
    }

    @Operation(summary = "Check whether credentials are stored")
    @GetMapping
    public ProviderCredentialStatusResponse status(@PathVariable String key) {
        ProviderDescriptor descriptor = descriptorFor(key);
        boolean configured = credentialService.hasCredentials(key, descriptor.credentialFields());
        return new ProviderCredentialStatusResponse(key, configured);
    }

    @Operation(summary = "Store credentials for a provider")
    @PostMapping
    public ProviderCredentialStatusResponse update(@PathVariable String key,
                                                  @RequestBody UpdateProviderCredentialsRequest req) {
        ProviderDescriptor descriptor = descriptorFor(key);
        List<ProviderCredentialField> fields = descriptor.credentialFields();
        if (fields.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provider does not accept stored credentials");
        }
        Map<String, String> payload = normalize(req, fields);
        credentialService.saveCredentials(key, payload);
        return new ProviderCredentialStatusResponse(key, true);
    }

    @Operation(summary = "Remove stored credentials")
    @DeleteMapping
    public ProviderCredentialStatusResponse delete(@PathVariable String key) {
        descriptorFor(key); // ensure provider exists
        credentialService.deleteCredentials(key);
        return new ProviderCredentialStatusResponse(key, false);
    }

    private ProviderDescriptor descriptorFor(String key) {
        return registry.get(key)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown provider: " + key))
                .descriptor();
    }

    private Map<String, String> normalize(UpdateProviderCredentialsRequest req, List<ProviderCredentialField> fields) {
        if (req == null || req.values() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Credential values are required");
        }
        Map<String, String> source = req.values();
        Set<String> allowed = fields.stream().map(ProviderCredentialField::name).collect(toSet());
        for (String provided : source.keySet()) {
            if (!allowed.contains(provided)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unexpected credential field: " + provided);
            }
        }
        LinkedHashMap<String, String> normalized = new LinkedHashMap<>();
        for (ProviderCredentialField field : fields) {
            String value = source.get(field.name());
            if (value == null || value.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field.label() + " is required");
            }
            normalized.put(field.name(), value.trim());
        }
        return normalized;
    }
}
