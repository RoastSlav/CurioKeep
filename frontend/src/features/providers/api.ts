import { apiFetch } from "../../api/client";
import type { ProviderLookupRequest, ProviderLookupResponse } from "./providerTypes";

export async function lookupProviders(payload: ProviderLookupRequest) {
    return apiFetch<ProviderLookupResponse>("/providers/lookup", {
        method: "POST",
        body: payload,
    });
}
