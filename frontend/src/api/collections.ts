import { apiFetch } from "./client";
import type { Collection, CreateCollectionRequest } from "./types";

export async function listCollections() {
    return apiFetch<Collection[]>("/collections");
}

export async function createCollection(payload: CreateCollectionRequest) {
    return apiFetch<Collection>("/collections", { method: "POST", body: payload });
}
