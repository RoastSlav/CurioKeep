package org.rostislav.curiokeep.providers;

import org.rostislav.curiokeep.items.entities.ItemIdentifierEntity;

import java.util.List;
import java.util.Optional;

public interface MetadataProvider {

    String key();

    boolean supports(ItemIdentifierEntity.IdType idType);

    Optional<ProviderResult> fetch(ItemIdentifierEntity.IdType idType, String idValue);

    default ProviderDescriptor descriptor() {
        return ProviderDescriptor.basic(this);
    }

    default List<ProviderCredentialField> credentialFields() {
        return List.of();
    }
}
