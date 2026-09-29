import type { Attributes } from "@/features/items/itemTypes";
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
  /** Field filters as the server expects them: `<fieldKey>.<operator>` mapped to a value. */
  filters?: Record<string, string>;
};

export type ModuleItemCounts = {
  total: number;
  byState: Record<string, number>;
  /** For each deprecated field that still has values, how many items hold one. */
  deprecatedFieldUse: Record<string, number>;
  /** Items on an earlier module version that the module declares a migration for. */
  pendingMigration?: number;
};

export type ItemCounts = {
  /** Keyed by module id; a module with no items is absent. */
  modules: Record<string, ModuleItemCounts>;
};

export type ImportItemError = {
  /** Position of the item in the file's items list, starting at 0. */
  index: number;
  reason: string;
};

export type ImportResult = {
  imported: number;
  failed: number;
  errors: ImportItemError[];
};

export type MigrationFieldChange = {
  field: string;
  /** Absent when the attribute would be added. */
  before?: unknown;
  /** Absent when the attribute would be removed. */
  after?: unknown;
};

export type MigrationPreview = {
  targetVersion: string;
  /** Items on an earlier module version. */
  behind: number;
  /** Of those, how many would have values changed. */
  changed: number;
  versions?: { version: string; items: number }[];
  samples?: { itemId: string; title?: string; changes: MigrationFieldChange[] }[];
};

export type MigrationResult = {
  migrated: number;
  changed: number;
  skipped?: number;
};

export type ExportFormat = "json" | "csv";

/** The download link for an export. It is opened by the browser, which sends the session cookie itself. */
export function exportUrl(collectionId: string, format: ExportFormat, moduleId?: string): string {
  const params = new URLSearchParams({ format });
  if (moduleId) params.set("moduleId", moduleId);
  return `/api/collections/${collectionId}/export?${params.toString()}`;
}

export function previewMigration(collectionId: string, moduleId: string, options?: { signal?: AbortSignal }) {
  return apiFetch<MigrationPreview>(`/collections/${collectionId}/items/migration?moduleId=${encodeURIComponent(moduleId)}`, {
    signal: options?.signal,
    dedupe: false,
  });
}

export function applyMigration(collectionId: string, moduleId: string) {
  return apiFetch<MigrationResult>(`/collections/${collectionId}/items/migration?moduleId=${encodeURIComponent(moduleId)}`, {
    method: "POST",
  });
}

export function importItems(collectionId: string, file: File) {
  const body = new FormData();
  body.append("file", file);
  return apiFetch<ImportResult>(`/collections/${collectionId}/import`, { method: "POST", body });
}

export const DEFAULT_PAGE_SIZE = 25;
export const PAGE_SIZES = [10, DEFAULT_PAGE_SIZE, 50, 100];

export function buildListParams(query: ItemListQuery): string {
  const params = new URLSearchParams({
    moduleId: query.moduleId,
    page: String(query.page ?? 0),
    size: String(query.size ?? DEFAULT_PAGE_SIZE),
  });

  if (query.search) params.set("search", query.search);
  if (query.states?.length) params.set("state", query.states.join(","));
  if (query.sort) params.set("sort", `${query.sort.field},${query.sort.direction}`);
  for (const [name, value] of Object.entries(query.filters ?? {})) {
    params.set(name, value);
  }

  return params.toString();
}

// Every request goes to the server: search, state, sort and filters are applied there, so a page is always a
// slice of the full result. There is no client-side cache because any edit by anyone can change a page.
export function listItems(
  collectionId: string,
  query: ItemListQuery,
  options?: { signal?: AbortSignal }
) {
  return apiFetch<PagedResult<Item>>(
    `/collections/${collectionId}/items?${buildListParams(query)}`,
    { signal: options?.signal, dedupe: false }
  );
}

export function fetchItemCounts(collectionId: string, options?: { signal?: AbortSignal }) {
  return apiFetch<ItemCounts>(`/collections/${collectionId}/items/counts`, {
    signal: options?.signal,
    dedupe: false,
  });
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
  return updated;
}

export async function deleteItem(collectionId: string, itemId: string) {
  const res = await apiFetch<void>(
    `/collections/${collectionId}/items/${itemId}`,
    { method: "DELETE" }
  );
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
  return updated;
}

export async function createItem(
  collectionId: string,
  payload: {
    moduleId: string;
    attributes: Attributes;
    stateKey?: string;
  }
) {
  const created = await apiFetch<Item>(`/collections/${collectionId}/items`, {
    method: "POST",
    body: payload,
  });
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
  return updated;
}
