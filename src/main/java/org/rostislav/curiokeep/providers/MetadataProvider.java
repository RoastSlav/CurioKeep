package org.rostislav.curiokeep.providers;

import org.rostislav.curiokeep.items.entities.ItemIdentifierEntity;

import java.util.Optional;

public interface MetadataProvider {

    String key();

    boolean supports(ItemIdentifierEntity.IdType idType);

    Optional<ProviderResult> fetch(ItemIdentifierEntity.IdType idType, String idValue);
}
