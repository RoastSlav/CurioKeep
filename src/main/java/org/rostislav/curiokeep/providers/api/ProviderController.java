package org.rostislav.curiokeep.providers.api;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.rostislav.curiokeep.items.entities.ItemIdentifierEntity;
import org.rostislav.curiokeep.modules.ModuleService;
import org.rostislav.curiokeep.modules.entities.ModuleDefinitionEntity;
import org.rostislav.curiokeep.providers.ProviderLookupService;
import org.rostislav.curiokeep.providers.api.dto.LookupResponse;
import org.rostislav.curiokeep.providers.api.dto.ProviderLookupRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/providers")
public class ProviderController {

    private final ModuleService modules;
    private final ProviderLookupService lookup;

    public ProviderController(ModuleService modules, ProviderLookupService lookup) {
        this.modules = modules;
        this.lookup = lookup;
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
}