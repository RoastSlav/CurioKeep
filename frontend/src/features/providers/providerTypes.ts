import type { IdentifierType, ItemIdentifier } from "../items/itemTypes";

export type ProviderAsset = {
    url: string;
    type?: string;
    label?: string;
    providerKey?: string;
};

export type ProviderLookupIdentifier = {
    idType: IdentifierType;
    idValue: string;
};

export type ProviderLookupRequest = {
    moduleId: string;
    identifiers: Array<ProviderLookupIdentifier | ItemIdentifier>;
    providers?: string[];
};

export type ProviderLookupResult = {
    providerKey: string;
    rawData?: Record<string, any>;
    normalizedFields?: Record<string, any> | string | null;
    assets?: ProviderAsset[];
    confidence?: { score?: number; reason?: string };
    error?: string;
};

export type ProviderLookupResponse = {
    results: ProviderLookupResult[];
    best?: ProviderLookupResult | null;
    mergedAttributes: Record<string, any>;
    assets?: ProviderAsset[];
    // Legacy aliases used by UI components
    providerResults?: ProviderLookupResult[];
    merged?: Record<string, any>;
    fieldValues?: Record<string, any>;
};
