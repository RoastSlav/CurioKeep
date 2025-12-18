import { apiFetch } from "../../api/client";
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

// The backend currently only supports paging + moduleId. Extra params may be ignored server-side; client-side
// filtering/sorting is handled in the hook layer when unsupported.
export async function listItems(collectionId: string, queryOrModuleId: ItemListQuery | string) {
    const query: ItemListQuery = typeof queryOrModuleId === "string" ? { moduleId: queryOrModuleId } : queryOrModuleId;

    const params = new URLSearchParams({
        moduleId: query.moduleId,
        page: String(query.page ?? 0),
        size: String(query.size ?? 25),
    });

    if (query.search) params.set("search", query.search);
    if (query.states?.length) params.set("state", query.states.join(","));
    if (query.sort) params.set("sort", `${query.sort.field},${query.sort.direction}`);

    return apiFetch<PagedResult<Item>>(`/collections/${collectionId}/items?${params.toString()}`);
}

export async function getItem(collectionId: string, itemId: string) {
    return apiFetch<Item>(`/collections/${collectionId}/items/${itemId}`);
}

export async function updateItem(collectionId: string, itemId: string, payload: Partial<Item>) {
    return apiFetch<Item>(`/collections/${collectionId}/items/${itemId}`, {
        method: "PUT",
        body: payload,
    });
}

export async function deleteItem(collectionId: string, itemId: string) {
    return apiFetch<void>(`/collections/${collectionId}/items/${itemId}`, { method: "DELETE" });
}

export async function changeItemState(collectionId: string, itemId: string, stateKey: string) {
    return apiFetch<Item>(`/collections/${collectionId}/items/${itemId}/state`, {
        method: "POST",
        body: { stateKey },
    });
}

export async function createItem(collectionId: string, payload: { moduleId: string; attributes: Record<string, any>; stateKey?: string }) {
    return apiFetch<Item>(`/collections/${collectionId}/items`, {
        method: "POST",
        body: payload,
    });
}

export async function setItemImageFromUrl(collectionId: string, itemId: string, url: string) {
    return apiFetch<Item>(`/collections/${collectionId}/items/${itemId}/image/from-url`, {
        method: "POST",
        body: { url },
    });
}

export async function uploadItemImage(collectionId: string, itemId: string, file: File) {
    const formData = new FormData();
    formData.append("file", file);

    return apiFetch<Item>(`/collections/${collectionId}/items/${itemId}/image/upload`, {
        method: "POST",
        body: formData,
    });
}
