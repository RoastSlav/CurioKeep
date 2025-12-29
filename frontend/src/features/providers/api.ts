import { apiFetch } from "../../api/client";
import {
  clearCached,
  getCached,
  setCached,
  DEFAULT_CACHE_TTL,
} from "../../api/cache";
import type {
  Provider,
  CredentialStatus,
  UpdateCredentialsRequest,
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

function normalizeIdentifiers(
  ids: ProviderLookupRequest["identifiers"]
): ProviderLookupIdentifier[] {
  return ids.map((id) => {
    if ("idType" in id && "idValue" in id) {
      return { idType: id.idType, idValue: id.idValue };
    }
    return { idType: id.type, idValue: id.value };
  });
}

export async function lookupProviders(
  payload: ProviderLookupRequest
): Promise<ProviderLookupResponse> {
  const request = {
    moduleId: payload.moduleId,
    identifiers: normalizeIdentifiers(payload.identifiers),
    providers: payload.providers,
    query: payload.query,
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
const PROVIDERS_CACHE_KEY = "providers:list";

export async function listProviders({ forceRefresh = false } = {}): Promise<
  Provider[]
> {
  if (!forceRefresh) {
    const cached = getCached<Provider[]>(PROVIDERS_CACHE_KEY);
    if (cached) return cached;
  }

  const providers = await apiFetch<Provider[]>("/providers");
  setCached(PROVIDERS_CACHE_KEY, providers, DEFAULT_CACHE_TTL, true);
  return providers;
}

export async function getCredentialStatus(
  providerKey: string
): Promise<CredentialStatus> {
  return apiFetch<CredentialStatus>(
    `/providers/${encodeURIComponent(providerKey)}/credentials`
  );
}

export async function updateCredentials(
  providerKey: string,
  credentials: UpdateCredentialsRequest
): Promise<CredentialStatus> {
  const res = await apiFetch<CredentialStatus>(
    `/providers/${encodeURIComponent(providerKey)}/credentials`,
    {
      method: "POST",
      body: credentials,
    }
  );
  clearCached(PROVIDERS_CACHE_KEY);
  return res;
}

export async function deleteCredentials(providerKey: string): Promise<void> {
  await apiFetch<void>(
    `/providers/${encodeURIComponent(providerKey)}/credentials`,
    {
      method: "DELETE",
    }
  );
  clearCached(PROVIDERS_CACHE_KEY);
}

export async function testProviderConnection(providerKey: string): Promise<{
  key: string;
  available: boolean;
  message: string | null;
  supportedIdTypes: string[];
  rateLimited: boolean;
  retryAfterSeconds: number | null;
  credentialsRequired: boolean;
  credentialsConfigured: boolean;
}> {
  return apiFetch(
    `/providers/${encodeURIComponent(providerKey)}/status/check`,
    {
      method: "POST",
    }
  );
}

export function clearProvidersCache() {
  clearCached(PROVIDERS_CACHE_KEY);
}
