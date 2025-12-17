import type { User, Collection, ModuleSummary, ModuleDetails, SetupStatus, ProviderLookupResult, CollectionModule } from "./types";

async function jsonFetch<T>(input: RequestInfo | URL, init?: RequestInit): Promise<T> {
    let res: Response;
    try {
        res = await fetch(input, {
            credentials: "include",
            headers: {
                "Content-Type": "application/json",
                ...(init?.headers || {}),
            },
            ...init,
        });
    } catch (err) {
        const message = (err as Error)?.message || "Network error";
        // Be explicit when the backend is down so the UI can show a clear error.
        if (message.includes("ECONNREFUSED") || message.includes("Failed to fetch")) {
            throw new Error("Cannot reach the backend API. Is the server running?");
        }
        throw new Error(message);
    }

    const contentType = res.headers.get("content-type") || "";
    const isJson = contentType.includes("application/json");

    if (!res.ok) {
        // Try to extract a meaningful error message from JSON or text bodies.
        if (isJson) {
            try {
                const data = (await res.json()) as { message?: string; error?: string };
                const msg = data.message || data.error;
                if (msg) throw new Error(msg);
            } catch {
                // fall through to text handling
            }
        }
        try {
            const text = await res.text();
            if (text) throw new Error(text);
        } catch {
            // ignore parsing errors
        }
        throw new Error(`Request failed: ${res.status} ${res.statusText}`);
    }

    if (res.status === 204) return undefined as T;
    if (!isJson) {
        // In practice we expect JSON, but handle text to avoid runtime errors.
        return (await res.text()) as T;
    }
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
