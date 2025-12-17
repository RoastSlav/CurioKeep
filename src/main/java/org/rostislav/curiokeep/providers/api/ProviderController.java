package org.rostislav.curiokeep.providers.api;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.rostislav.curiokeep.items.entities.ItemIdentifierEntity;
import org.rostislav.curiokeep.modules.ModuleService;
import org.rostislav.curiokeep.modules.entities.ModuleDefinitionEntity;
import org.rostislav.curiokeep.providers.MetadataProvider;
import org.rostislav.curiokeep.providers.ProviderDescriptor;
import org.rostislav.curiokeep.providers.ProviderKnowledgeBase;
import org.rostislav.curiokeep.providers.ProviderProfile;
import org.rostislav.curiokeep.providers.ProviderLookupService;
import org.rostislav.curiokeep.providers.ProviderRegistry;
import org.rostislav.curiokeep.providers.api.dto.LookupResponse;
import org.rostislav.curiokeep.providers.api.dto.ProviderInfoResponse;
import org.rostislav.curiokeep.providers.api.dto.ProviderStatusResponse;
import org.rostislav.curiokeep.providers.api.dto.ProviderLookupRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/providers")
public class ProviderController {

    private final ModuleService modules;
    private final ProviderRegistry registry;
    private final ProviderLookupService lookup;
    private final ProviderKnowledgeBase knowledgeBase;

    public ProviderController(ModuleService modules, ProviderRegistry registry, ProviderLookupService lookup, ProviderKnowledgeBase knowledgeBase) {
        this.modules = modules;
        this.registry = registry;
        this.lookup = lookup;
        this.knowledgeBase = knowledgeBase;
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
        MetadataProvider provider = registry.get(key)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown provider: " + key));
        ProviderDescriptor descriptor = provider.descriptor();
        return new ProviderStatusResponse(descriptor.key(), true, "Available", descriptor.supportedIdTypes());
    }

    @PostMapping("/lookup")
    @Operation(summary = "Lookup metadata from external providers (no saving)")
    public LookupResponse lookup(@RequestBody @Valid ProviderLookupRequest req) {
        ModuleDefinitionEntity module = modules.getById(req.moduleId());
        List<ItemIdentifierEntity> ids = req.identifiers().stream().map(d -> {
            ItemIdentifierEntity e = new ItemIdentifierEntity();
            e.setIdType(d.idType());
            e.setIdValue(d.idValue());
            e.setItemId(null);
            return e;
        }).toList();

        return lookup.lookup(module, ids);
    }

    private ProviderInfoResponse toInfo(MetadataProvider provider) {
        ProviderDescriptor descriptor = provider.descriptor();
        ProviderProfile profile = knowledgeBase.profileFor(descriptor.key());
        return new ProviderInfoResponse(
            descriptor.key(),
            profile != null && profile.displayName() != null ? profile.displayName() : descriptor.displayName(),
            profile != null && profile.summary() != null ? profile.summary() : descriptor.description(),
            descriptor.supportedIdTypes(),
            descriptor.priority(),
            profile == null ? null : profile.websiteUrl(),
            profile == null ? null : profile.apiUrl(),
            profile == null ? null : profile.dataReturned(),
            profile == null ? List.of() : profile.highlights()
        );
    }
}