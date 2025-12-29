package org.rostislav.curiokeep.providers;

import org.rostislav.curiokeep.items.entities.ItemIdentifierEntity;

import java.util.Optional;

record StubMetadataProvider(ProviderResult result) implements MetadataProvider {

    @Override
    public String key() {
        return result.providerKey();
    }

    @Override
    public boolean supports(ItemIdentifierEntity.IdType idType) {
        return true;
    }

    @Override
    public Optional<ProviderResult> fetch(ItemIdentifierEntity.IdType idType, String idValue) {
        return Optional.of(result);
    }
}
