import type { User, Collection, ModuleSummary, ModuleDetails, SetupStatus, ProviderLookupResult, CollectionModule } from "./types";

async function jsonFetch<T>(input: RequestInfo | URL, init?: RequestInit): Promise<T> {
    const res = await fetch(input, {
        credentials: "include",
        headers: {
            "Content-Type": "application/json",
            ...(init?.headers || {}),
        },
        ...init,
    });
    if (!res.ok) {
        const text = await res.text();
        throw new Error(text || `HTTP ${res.status}`);
    }
    if (res.status === 204) return undefined as T;
    return res.json() as Promise<T>;
}

export async function getSetupStatus(): Promise<SetupStatus> {
    return jsonFetch<SetupStatus>("/api/setup/status", { method: "GET" });
}

export async function createAdmin(email: string, displayName: string, password: string): Promise<void> {
    await jsonFetch("/api/setup/admin", { method: "POST", body: JSON.stringify({ email, displayName, password }) });
}

export async function login(email: string, password: string): Promise<void> {
    await jsonFetch("/api/auth/login", { method: "POST", body: JSON.stringify({ email, password }) });
}

export async function logout(): Promise<void> {
    await jsonFetch("/api/auth/logout", { method: "POST" });
}

export async function getMe(): Promise<User> {
    return jsonFetch<User>("/api/auth/me", { method: "GET" });
}

export async function getHealth(): Promise<{ status: string }> {
    return jsonFetch("/api/health", { method: "GET" });
}

export async function listModules(): Promise<ModuleSummary[]> {
    return jsonFetch<ModuleSummary[]>("/api/modules", { method: "GET" });
}

export async function getModule(key: string): Promise<ModuleDetails> {
    return jsonFetch<ModuleDetails>(`/api/modules/${encodeURIComponent(key)}`, { method: "GET" });
}

export async function listCollections(): Promise<Collection[]> {
    return jsonFetch<Collection[]>("/api/collections", { method: "GET" });
}

export async function createCollection(name: string, description: string): Promise<Collection> {
    return jsonFetch<Collection>("/api/collections", {
        method: "POST",
        body: JSON.stringify({ name, description }),
    });
}

export async function listCollectionModules(collectionId: string): Promise<CollectionModule[]> {
    return jsonFetch<CollectionModule[]>(`/api/collections/${collectionId}/modules`, { method: "GET" });
}

export async function enableCollectionModule(collectionId: string, moduleKey: string): Promise<void> {
    await jsonFetch(`/api/collections/${collectionId}/modules/${encodeURIComponent(moduleKey)}`, { method: "POST" });
}

export async function disableCollectionModule(collectionId: string, moduleKey: string): Promise<void> {
    await jsonFetch(`/api/collections/${collectionId}/modules/${encodeURIComponent(moduleKey)}`, { method: "DELETE" });
}

export async function lookupProviders(payload: ProviderLookupResult["request"]): Promise<ProviderLookupResult> {
    return jsonFetch<ProviderLookupResult>("/api/providers/lookup", { method: "POST", body: JSON.stringify(payload) });
}

export type { User };
