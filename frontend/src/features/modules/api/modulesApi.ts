import { apiFetch } from "../../../api/client";
import {
  clearByPrefix,
  clearCached,
  getCached,
  setCached,
  DEFAULT_CACHE_TTL,
} from "../../../api/cache";

export type ModuleSource = "BUILTIN" | "IMPORTED" | "USER";

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

export type ModuleContract = {
  key: string;
  version: string;
  name: string;
  description?: string;
  meta?: ModuleMeta;
  states: StateContract[];
  providers: ProviderContract[];
  fields: FieldContract[];
  workflows: WorkflowContract[];
  extensions: Record<string, unknown>;
};

export type ModuleMeta = {
  authors: Author[];
  license?: string;
  homepage?: string;
  repository?: string;
  icon?: string;
  tags: string[];
  minAppVersion?: string;
};

export type Author = {
  name?: string;
  email?: string;
  url?: string;
};

export type StateContract = {
  key: string;
  label: string;
  order: number;
  active: boolean;
  deprecated: boolean;
};

export type ProviderContract = {
  key: string;
  enabled: boolean;
  priority: number;
  supportsIdentifiers: IdentifierType[];
};

export type IdentifierType =
  | "ISBN10"
  | "ISBN13"
  | "UPC"
  | "EAN"
  | "ASIN"
  | "CUSTOM";

export type FieldContract = {
  key: string;
  label: string;
  type: FieldType;
  required: boolean;
  searchable: boolean;
  filterable: boolean;
  sortable: boolean;
  order: number;
  active: boolean;
  deprecated: boolean;
  defaultValue?: unknown;
  identifiers: IdentifierType[];
  enumValues: EnumValue[];
  constraints?: Record<string, unknown>;
  ui?: Record<string, unknown>;
  providerMappings: ProviderMapping[];
};

export type FieldType =
  | "TEXT"
  | "NUMBER"
  | "DATE"
  | "BOOLEAN"
  | "ENUM"
  | "TAGS"
  | "LINK"
  | "JSON";

export type EnumValue = {
  key: string;
  label: string;
};

export type ProviderMapping = {
  provider: string;
  path?: string;
  transform?: string;
};

export type WorkflowContract = {
  key: string;
  label: string;
  steps: WorkflowStep[];
};

export type WorkflowStep = {
  type: WorkflowStepType;
  field?: string;
  fields: string[];
  providers: string[];
  label?: string;
};

export type WorkflowStepType =
  | "PROMPT"
  | "PROMPT_ANY"
  | "LOOKUP_METADATA"
  | "APPLY_METADATA"
  | "SAVE_ITEM";

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
