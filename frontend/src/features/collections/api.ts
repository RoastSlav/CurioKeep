import { apiFetch } from "../../api/client";
import {
  clearCached,
  getCached,
  setCached,
  DEFAULT_CACHE_TTL,
} from "../../api/cache";
import type {
  Collection,
  CollectionModule,
  CreateCollectionRequest,
} from "../../api/types";

export type UpdateCollectionRequest = {
  name?: string;
  description?: string | null;
};

const COLLECTIONS_CACHE_KEY = "collections:list";

export type ListCollectionsOptions = {
  forceRefresh?: boolean;
};

export async function listCollections(options?: ListCollectionsOptions) {
  if (!options?.forceRefresh) {
    const cached = getCached<Collection[]>(COLLECTIONS_CACHE_KEY);
    if (cached) return cached;
  }

  const collections = await apiFetch<Collection[]>("/collections");
  return setCached(COLLECTIONS_CACHE_KEY, collections, DEFAULT_CACHE_TTL, true);
}

export async function getCollection(id: string) {
  return apiFetch<Collection>(`/collections/${id}`);
}

export async function listCollectionModules(collectionId: string) {
  return apiFetch<CollectionModule[]>(`/collections/${collectionId}/modules`);
}

export async function createCollection(payload: CreateCollectionRequest) {
  const created = await apiFetch<Collection>("/collections", {
    method: "POST",
    body: payload,
  });
  clearCollectionsCache();
  return created;
}

export async function updateCollection(
  id: string,
  payload: UpdateCollectionRequest
) {
  const updated = await apiFetch<Collection>(`/collections/${id}`, {
    method: "PUT",
    body: payload,
  });
  clearCollectionsCache();
  return updated;
}

export async function deleteCollection(id: string) {
  await apiFetch<void>(`/collections/${id}`, { method: "DELETE" });
  clearCollectionsCache();
}

export function clearCollectionsCache() {
  clearCached(COLLECTIONS_CACHE_KEY);
}
