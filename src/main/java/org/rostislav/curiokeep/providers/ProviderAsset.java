package org.rostislav.curiokeep.providers;

import java.net.URI;

public record ProviderAsset(
        AssetType type,
        URI url,
        Integer width,
        Integer height
) {
}
