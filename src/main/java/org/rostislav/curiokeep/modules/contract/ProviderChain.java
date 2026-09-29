package org.rostislav.curiokeep.modules.contract;

/**
 * After the provider that declares it returns a result, looks up that result's normalized field {@code from} as an identifier of
 * type {@code idType} in the module's provider {@code to}.
 */
public record ProviderChain(String from, String to, IdentifierType idType) {
}
