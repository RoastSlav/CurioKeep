import type { User, Collection, ModuleSummary, ModuleDetails, SetupStatus, ProviderLookupResult, CollectionModule, AdminUser, InviteValidation } from "./types";

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
    const raw = await jsonFetch<any>("/api/auth/me", { method: "GET" });
    return {
        id: raw.id,
        email: raw.email,
        displayName: raw.displayName,
        admin: raw.admin ?? raw.isAdmin ?? false,
    } satisfies User;
}

export async function getHealth(): Promise<{ status: string }> {
    return jsonFetch("/api/health", { method: "GET" });
}

export async function listModules(): Promise<ModuleSummary[]> {
    const mods = await jsonFetch<any[]>("/api/modules", { method: "GET" });
    return mods.map((m) => ({
        moduleKey: m.moduleKey ?? m.key,
        key: m.moduleKey ?? m.key,
        name: m.name,
        version: m.version,
        description: m.description,
    } satisfies ModuleSummary));
}

export async function getModule(key: string): Promise<ModuleDetails> {
    const data = await jsonFetch<any>(`/api/modules/${encodeURIComponent(key)}`, { method: "GET" });
    const contract = data.contract || {};
    return {
        moduleKey: data.moduleKey ?? data.key,
        key: data.moduleKey ?? data.key,
        name: data.name,
        version: data.version,
        description: contract.description ?? data.description,
        states: contract.states,
        providers: contract.providers,
        fields: contract.fields,
    } satisfies ModuleDetails;
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
    const data = await jsonFetch<any[]>(`/api/collections/${collectionId}/modules`, { method: "GET" });
    return data.map((m) => ({
        moduleKey: m.moduleKey,
        moduleName: m.name ?? m.moduleName,
        version: m.version,
    } satisfies CollectionModule));
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

// Admin invites
export async function createInvite(email: string): Promise<string> {
    const res = await jsonFetch<{ token: string }>("/api/admin/invites", {
        method: "POST",
        body: JSON.stringify({ email }),
    });
    return res.token;
}

export async function validateInvite(token: string): Promise<InviteValidation> {
    return jsonFetch<InviteValidation>(`/api/invites/${encodeURIComponent(token)}/validate`, { method: "GET" });
}

export async function acceptInvite(token: string, password: string, displayName: string): Promise<void> {
    await jsonFetch("/api/invites/accept", {
        method: "POST",
        body: JSON.stringify({ token, password, displayName }),
    });
}

// Admin users (API not yet available server-side; wired to expected endpoints)
export async function listUsers(): Promise<AdminUser[]> {
    return jsonFetch<AdminUser[]>("/api/admin/users", { method: "GET" });
}

export async function setUserStatus(userId: string, status: "ACTIVE" | "DISABLED"): Promise<void> {
    await jsonFetch(`/api/admin/users/${encodeURIComponent(userId)}/status`, {
        method: "POST",
        body: JSON.stringify({ status }),
    });
}

export async function setUserAdmin(userId: string, admin: boolean): Promise<void> {
    await jsonFetch(`/api/admin/users/${encodeURIComponent(userId)}/admin`, {
        method: "POST",
        body: JSON.stringify({ admin }),
    });
}

export async function resetUserPassword(userId: string): Promise<void> {
    await jsonFetch(`/api/admin/users/${encodeURIComponent(userId)}/reset-password`, { method: "POST" });
}

export async function deleteUser(userId: string): Promise<void> {
    await jsonFetch(`/api/admin/users/${encodeURIComponent(userId)}`, { method: "DELETE" });
}
