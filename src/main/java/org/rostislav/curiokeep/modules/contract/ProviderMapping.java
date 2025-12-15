package org.rostislav.curiokeep.modules.contract;

public record ProviderMapping(
        String provider,
        String path,
        String transform
) {
}