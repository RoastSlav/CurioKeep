import { apiFetch } from "../../api/client";
import type { Collection, CollectionModule, CreateCollectionRequest } from "../../api/types";

export type UpdateCollectionRequest = {
    name?: string;
    description?: string | null;
};

export async function listCollections() {
    return apiFetch<Collection[]>("/collections");
}

export async function getCollection(id: string) {
    return apiFetch<Collection>(`/collections/${id}`);
}

export async function listCollectionModules(collectionId: string) {
    return apiFetch<CollectionModule[]>(`/collections/${collectionId}/modules`);
}

export async function createCollection(payload: CreateCollectionRequest) {
    return apiFetch<Collection>("/collections", { method: "POST", body: payload });
}

export async function updateCollection(id: string, payload: UpdateCollectionRequest) {
    return apiFetch<Collection>(`/collections/${id}`, { method: "PUT", body: payload });
}

export async function deleteCollection(id: string) {
    await apiFetch<void>(`/collections/${id}`, { method: "DELETE" });
}
