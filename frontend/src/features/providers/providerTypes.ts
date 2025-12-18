import type { ItemIdentifier } from "../items/itemTypes";

export type ProviderAsset = {
    type: string;
    url: string;
    label?: string;
};

export type ProviderLookupRequest = {
    moduleId: string;
    identifiers: ItemIdentifier[];
    providers?: string[];
};

export type ProviderLookupResult = {
    providerKey: string;
    merged?: Record<string, any>;
    fieldValues?: Record<string, any>;
    assets?: ProviderAsset[];
    error?: string;
    raw?: unknown;
};

export type ProviderLookupResponse = {
    merged: Record<string, any>;
    fieldValues: Record<string, any>;
    assets?: ProviderAsset[];
    providerResults?: ProviderLookupResult[];
};
