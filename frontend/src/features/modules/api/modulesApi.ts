import {apiFetch} from "../../../api/client"

export type ModuleSource = "BUILTIN" | "IMPORTED" | "USER"

export type ModuleSummary = {
    id: string
    moduleKey: string
    name: string
    version: string
    source: ModuleSource
    updatedAt: string
}

export type ModuleDetails = {
    id: string
    moduleKey: string
    name: string
    version: string
    source: ModuleSource
    checksum: string
    contract: ModuleContract
    createdAt: string
    updatedAt: string
}

export type ModuleContract = {
    key: string
    version: string
    name: string
    description?: string
    meta?: ModuleMeta
    states: StateContract[]
    providers: ProviderContract[]
    fields: FieldContract[]
    workflows: WorkflowContract[]
    extensions: Record<string, unknown>
}

export type ModuleMeta = {
    authors: Author[]
    license?: string
    homepage?: string
    repository?: string
    icon?: string
    tags: string[]
    minAppVersion?: string
}

export type Author = {
    name?: string
    email?: string
    url?: string
}

export type StateContract = {
    key: string
    label: string
    order: number
    active: boolean
    deprecated: boolean
}

export type ProviderContract = {
    key: string
    enabled: boolean
    priority: number
    supportsIdentifiers: IdentifierType[]
}

export type IdentifierType = "ISBN10" | "ISBN13" | "UPC" | "EAN" | "ASIN" | "CUSTOM"

export type FieldContract = {
    key: string
    label: string
    type: FieldType
    required: boolean
    searchable: boolean
    filterable: boolean
    sortable: boolean
    order: number
    active: boolean
    deprecated: boolean
    defaultValue?: unknown
    identifiers: IdentifierType[]
    enumValues: EnumValue[]
    constraints?: Record<string, unknown>
    ui?: Record<string, unknown>
    providerMappings: ProviderMapping[]
}

export type FieldType = "TEXT" | "NUMBER" | "DATE" | "BOOLEAN" | "ENUM" | "TAGS" | "LINK" | "JSON"

export type EnumValue = {
    key: string
    label: string
}

export type ProviderMapping = {
    provider: string
    path?: string
    transform?: string
}

export type WorkflowContract = {
    key: string
    label: string
    steps: WorkflowStep[]
}

export type WorkflowStep = {
    type: WorkflowStepType
    field?: string
    fields: string[]
    providers: string[]
    label?: string
}

export type WorkflowStepType = "PROMPT" | "PROMPT_ANY" | "LOOKUP_METADATA" | "APPLY_METADATA" | "SAVE_ITEM"

export type ModuleRawXmlResponse = {
    xmlRaw: string
}

export type ScanFailure = {
    source: string
    reason: string
}

export type ScanModulesResponse = {
    imported: ModuleSummary[]
    skipped: string[]
    failed: ScanFailure[]
}

export function listModules() {
    return apiFetch<ModuleSummary[]>("/modules")
}

export function getModuleDetails(moduleKey: string) {
    return apiFetch<ModuleDetails>(`/modules/${encodeURIComponent(moduleKey)}`)
}

export function getModuleRawXml(moduleKey: string) {
    return apiFetch<ModuleRawXmlResponse>(`/modules/${encodeURIComponent(moduleKey)}/raw`)
}

type DeleteModuleResponse = {
    ok: boolean
}

export function deleteImportedModule(moduleKey: string) {
    return apiFetch<DeleteModuleResponse>(`/admin/modules/${encodeURIComponent(moduleKey)}`, {
        method: "DELETE",
    })
}

export function importModuleXml(file: File) {
    const formData = new FormData()
    formData.append("file", file)
    return apiFetch<ModuleDetails>("/admin/modules/import", {
        method: "POST",
        body: formData,
    })
}

export function scanModulesFolder() {
    return apiFetch<ScanModulesResponse>("/admin/modules/scan", {
        method: "POST",
    })
}
