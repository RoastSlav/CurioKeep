import { apiFetch } from "../../api/client";
import type {
    ProviderLookupIdentifier,
    ProviderLookupRequest,
    ProviderLookupResponse,
    ProviderLookupResult,
} from "./providerTypes";

type RawLookupResponse = {
    results: ProviderLookupResult[];
    best?: ProviderLookupResult | null;
    mergedAttributes: Record<string, any>;
    assets?: ProviderLookupResponse["assets"];
};

function normalizeIdentifiers(ids: ProviderLookupRequest["identifiers"]): ProviderLookupIdentifier[] {
    return ids.map((id) => {
        if ("idType" in id && "idValue" in id) {
            return { idType: id.idType, idValue: id.idValue };
        }
        return { idType: id.type, idValue: id.value };
    });
}

export async function lookupProviders(payload: ProviderLookupRequest): Promise<ProviderLookupResponse> {
    const request = {
        moduleId: payload.moduleId,
        identifiers: normalizeIdentifiers(payload.identifiers),
    };

    const raw = await apiFetch<RawLookupResponse>("/providers/lookup", {
        method: "POST",
        body: request,
    });

    return {
        ...raw,
        providerResults: raw.results,
        merged: raw.mergedAttributes,
        fieldValues: raw.mergedAttributes,
    };
}
