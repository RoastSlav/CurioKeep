import { apiFetch } from "../../../api/client";
import type { CollectionModule, ModuleSummary } from "../../../api/types";

export async function listAvailableModules() {
    return apiFetch<ModuleSummary[]>("/modules");
}

export async function listEnabledModules(collectionId: string) {
    return apiFetch<CollectionModule[]>(`/collections/${collectionId}/modules`);
}

export async function enableCollectionModule(collectionId: string, moduleKey: string) {
    return apiFetch<{ enabled: boolean }>(`/collections/${collectionId}/modules/${moduleKey}`, { method: "POST" });
}

export async function disableCollectionModule(collectionId: string, moduleKey: string) {
    return apiFetch<{ enabled: boolean }>(`/collections/${collectionId}/modules/${moduleKey}`, { method: "DELETE" });
}
