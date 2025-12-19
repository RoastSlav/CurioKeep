import { apiFetch } from "../../api/client";
import {
  clearByPrefix,
  clearCached,
  getCached,
  setCached,
  DEFAULT_CACHE_TTL,
} from "../../api/cache";
import type { Item, PagedResult } from "../../api/types";

export type ItemSort = {
  field: string;
  direction: "asc" | "desc";
};

export type ItemListQuery = {
  moduleId: string;
  page?: number;
  size?: number;
  search?: string;
  states?: string[];
  sort?: ItemSort;
};

export type ListItemsOptions = {
  forceRefresh?: boolean;
};

const ITEMS_CACHE_PREFIX = "items:list:";

function buildParams(query: ItemListQuery) {
  const params = new URLSearchParams({
    moduleId: query.moduleId,
    page: String(query.page ?? 0),
    size: String(query.size ?? 25),
  });

  if (query.search) params.set("search", query.search);
  if (query.states?.length) params.set("state", query.states.join(","));
  if (query.sort)
    params.set("sort", `${query.sort.field},${query.sort.direction}`);

  return params.toString();
}

function buildCacheKey(collectionId: string, params: string) {
  return `${ITEMS_CACHE_PREFIX}${collectionId}:${params}`;
}

// The backend currently only supports paging + moduleId. Extra params may be ignored server-side; client-side
// filtering/sorting is handled in the hook layer when unsupported.
export async function listItems(
  collectionId: string,
  queryOrModuleId: ItemListQuery | string,
  options?: ListItemsOptions
) {
  const query: ItemListQuery =
    typeof queryOrModuleId === "string"
      ? { moduleId: queryOrModuleId }
      : queryOrModuleId;

  const params = buildParams(query);
  const cacheKey = buildCacheKey(collectionId, params);
  if (!options?.forceRefresh) {
    const cached = getCached<PagedResult<Item>>(cacheKey);
    if (cached) return cached;
  }

  const url = `/collections/${collectionId}/items?${params}`;
  const result = await apiFetch<PagedResult<Item>>(url);
  return setCached(cacheKey, result, DEFAULT_CACHE_TTL, true);
}

export async function getItem(collectionId: string, itemId: string) {
  return apiFetch<Item>(`/collections/${collectionId}/items/${itemId}`);
}

export async function updateItem(
  collectionId: string,
  itemId: string,
  payload: Partial<Item>
) {
  const updated = await apiFetch<Item>(
    `/collections/${collectionId}/items/${itemId}`,
    {
      method: "PUT",
      body: payload,
    }
  );
  clearItemsCache(collectionId);
  return updated;
}

export async function deleteItem(collectionId: string, itemId: string) {
  const res = await apiFetch<void>(
    `/collections/${collectionId}/items/${itemId}`,
    { method: "DELETE" }
  );
  clearItemsCache(collectionId);
  return res;
}

export async function changeItemState(
  collectionId: string,
  itemId: string,
  stateKey: string
) {
  const updated = await apiFetch<Item>(
    `/collections/${collectionId}/items/${itemId}/state`,
    {
      method: "POST",
      body: { stateKey },
    }
  );
  clearItemsCache(collectionId);
  return updated;
}

export async function createItem(
  collectionId: string,
  payload: {
    moduleId: string;
    attributes: Record<string, any>;
    stateKey?: string;
  }
) {
  const created = await apiFetch<Item>(`/collections/${collectionId}/items`, {
    method: "POST",
    body: payload,
  });
  clearItemsCache(collectionId);
  return created;
}

export async function setItemImageFromUrl(
  collectionId: string,
  itemId: string,
  url: string
) {
  const updated = await apiFetch<Item>(
    `/collections/${collectionId}/items/${itemId}/image/from-url`,
    {
      method: "POST",
      body: { url },
    }
  );
  clearItemsCache(collectionId);
  return updated;
}

export async function uploadItemImage(
  collectionId: string,
  itemId: string,
  file: File
) {
  const formData = new FormData();
  formData.append("file", file);

  const updated = await apiFetch<Item>(
    `/collections/${collectionId}/items/${itemId}/image/upload`,
    {
      method: "POST",
      body: formData,
    }
  );
  clearItemsCache(collectionId);
  return updated;
}

export function clearItemsCache(collectionId: string, moduleId?: string) {
  if (moduleId) {
    clearByPrefix(`${ITEMS_CACHE_PREFIX}${collectionId}:moduleId=${moduleId}`);
    return;
  }
  clearByPrefix(`${ITEMS_CACHE_PREFIX}${collectionId}:`);
}
