package org.rostislav.curiokeep.providers.api;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.rostislav.curiokeep.items.entities.ItemIdentifierEntity;
import org.rostislav.curiokeep.modules.ModuleService;
import org.rostislav.curiokeep.modules.entities.ModuleDefinitionEntity;
import org.rostislav.curiokeep.providers.MetadataProvider;
import org.rostislav.curiokeep.providers.ProviderDescriptor;
import org.rostislav.curiokeep.providers.ProviderKnowledgeBase;
import org.rostislav.curiokeep.providers.ProviderLookupService;
import org.rostislav.curiokeep.providers.ProviderCredentialField;
import org.rostislav.curiokeep.providers.ProviderCredentialService;
import org.rostislav.curiokeep.providers.ProviderProfile;
import org.rostislav.curiokeep.providers.ProviderRegistry;
import org.rostislav.curiokeep.providers.ProviderStatusService;
import org.rostislav.curiokeep.providers.api.dto.LookupResponse;
import org.rostislav.curiokeep.providers.api.dto.ProviderInfoResponse;
import org.rostislav.curiokeep.providers.api.dto.ProviderStatusResponse;
import org.rostislav.curiokeep.providers.api.dto.ProviderLookupRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/providers")
public class ProviderController {

    private final ModuleService modules;
    private final ProviderRegistry registry;
    private final ProviderLookupService lookup;
    private final ProviderKnowledgeBase knowledgeBase;
    private final ProviderStatusService statusService;
    private final ProviderCredentialService credentialService;

    public ProviderController(ModuleService modules,
                              ProviderRegistry registry,
                              ProviderLookupService lookup,
                              ProviderKnowledgeBase knowledgeBase,
                              ProviderStatusService statusService,
                              ProviderCredentialService credentialService) {
        this.modules = modules;
        this.registry = registry;
        this.lookup = lookup;
        this.knowledgeBase = knowledgeBase;
        this.statusService = statusService;
        this.credentialService = credentialService;
    }

    @GetMapping
    @Operation(summary = "List available metadata providers")
    public List<ProviderInfoResponse> listProviders() {
        return registry.all().stream()
            .map(this::toInfo)
            .sorted(Comparator.comparing(ProviderInfoResponse::displayName))
            .toList();
    }

    @GetMapping("/{key}/status")
    @Operation(summary = "Get provider readiness/status")
    public ProviderStatusResponse status(@PathVariable String key) {
        return statusService.getStatus(key);
    }

    @PostMapping("/{key}/status/check")
    @Operation(summary = "Trigger a live status check (rate limited)")
    public ProviderStatusResponse checkStatus(@PathVariable String key) {
        return statusService.checkStatus(key);
    }

    @PostMapping("/lookup")
    @Operation(summary = "Lookup metadata from external providers (no saving)")
    public LookupResponse lookup(@RequestBody @Valid ProviderLookupRequest req) {
        ModuleDefinitionEntity module = modules.getById(req.moduleId());
        List<ItemIdentifierEntity> ids = Optional.ofNullable(req.identifiers()).orElse(List.of()).stream().map(d -> {
            ItemIdentifierEntity e = new ItemIdentifierEntity();
            e.setIdType(d.idType());
            e.setIdValue(d.idValue());
            e.setItemId(null);
            return e;
        }).toList();

        List<String> providers = Optional.ofNullable(req.providers()).orElse(List.of());
        String query = Optional.ofNullable(req.query()).map(String::trim).filter(s -> !s.isEmpty()).orElse(null);

        // If no identifiers but a query is provided, use it as a CUSTOM identifier for free-text lookups
        if ((ids == null || ids.isEmpty()) && query != null) {
            ItemIdentifierEntity qId = new ItemIdentifierEntity();
            qId.setIdType(ItemIdentifierEntity.IdType.CUSTOM);
            qId.setIdValue(query);
            qId.setItemId(null);
            ids = List.of(qId);
        }

        return lookup.lookup(module, ids, providers, query);
    }

    private ProviderInfoResponse toInfo(MetadataProvider provider) {
        ProviderDescriptor descriptor = provider.descriptor();
        ProviderProfile profile = knowledgeBase.profileFor(descriptor.key());
        List<ProviderCredentialField> credentialFields = descriptor.credentialFields();
        boolean configured = credentialService.hasCredentials(descriptor.key(), credentialFields);
        return new ProviderInfoResponse(
            descriptor.key(),
            profile != null && profile.displayName() != null ? profile.displayName() : descriptor.displayName(),
            profile != null && profile.summary() != null ? profile.summary() : descriptor.description(),
            descriptor.supportedIdTypes(),
            descriptor.priority(),
            profile == null ? null : profile.websiteUrl(),
            profile == null ? null : profile.apiUrl(),
            profile == null ? null : profile.dataReturned(),
            profile == null ? List.of() : profile.highlights(),
            credentialFields,
            configured
        );
    }
}