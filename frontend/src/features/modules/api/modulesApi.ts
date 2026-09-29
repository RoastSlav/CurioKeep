import { apiFetch } from "../../../api/client";
import type { ModuleContract } from "../moduleTypes";
import {
  clearByPrefix,
  clearCached,
  getCached,
  setCached,
  DEFAULT_CACHE_TTL,
} from "../../../api/cache";

export type ModuleSource = "BUILTIN" | "IMPORTED";

export type ModuleSummary = {
  id: string;
  moduleKey: string;
  name: string;
  version: string;
  source: ModuleSource;
  updatedAt: string;
};

export type ModuleDetails = {
  id: string;
  moduleKey: string;
  name: string;
  version: string;
  source: ModuleSource;
  checksum: string;
  contract: ModuleContract;
  createdAt: string;
  updatedAt: string;
};

export type ModuleRawXmlResponse = {
  xmlRaw: string;
};

export type ScanFailure = {
  source: string;
  reason: string;
};

export type ScanModulesResponse = {
  imported: ModuleSummary[];
  skipped: string[];
  failed: ScanFailure[];
};

const MODULE_LIST_CACHE_KEY = "modules:list";
const MODULE_DETAIL_CACHE_PREFIX = "modules:detail:";

function detailCacheKey(moduleKey: string) {
  return `${MODULE_DETAIL_CACHE_PREFIX}${moduleKey}`;
}

function clearModuleCaches() {
  clearCached(MODULE_LIST_CACHE_KEY);
  clearByPrefix(MODULE_DETAIL_CACHE_PREFIX);
}

export async function listModules({ forceRefresh = false } = {}) {
  if (!forceRefresh) {
    const cached = getCached<ModuleSummary[]>(MODULE_LIST_CACHE_KEY);
    if (cached) return cached;
  }

  const data = await apiFetch<ModuleSummary[]>("/modules");
  setCached(MODULE_LIST_CACHE_KEY, data, DEFAULT_CACHE_TTL, true);
  return data;
}

export async function getModuleDetails(
  moduleKey: string,
  { forceRefresh = false } = {}
) {
  const cacheKey = detailCacheKey(moduleKey);

  if (!forceRefresh) {
    const cached = getCached<ModuleDetails>(cacheKey);
    if (cached) return cached;
  }

  const data = await apiFetch<ModuleDetails>(
    `/modules/${encodeURIComponent(moduleKey)}`
  );
  setCached(cacheKey, data, DEFAULT_CACHE_TTL, true);
  return data;
}

export function getModuleRawXml(moduleKey: string) {
  return apiFetch<ModuleRawXmlResponse>(
    `/modules/${encodeURIComponent(moduleKey)}/raw`
  );
}

type DeleteModuleResponse = {
  ok: boolean;
};

export function deleteImportedModule(moduleKey: string) {
  return apiFetch<DeleteModuleResponse>(
    `/admin/modules/${encodeURIComponent(moduleKey)}`,
    {
      method: "DELETE",
    }
  ).then((res) => {
    clearModuleCaches();
    return res;
  });
}

export function importModuleXml(file: File) {
  const formData = new FormData();
  formData.append("file", file);
  return apiFetch<ModuleDetails>("/admin/modules/import", {
    method: "POST",
    body: formData,
  }).then((res) => {
    clearModuleCaches();
    return res;
  });
}

export function scanModulesFolder() {
  return apiFetch<ScanModulesResponse>("/admin/modules/scan", {
    method: "POST",
  }).then((res) => {
    clearModuleCaches();
    return res;
  });
}
