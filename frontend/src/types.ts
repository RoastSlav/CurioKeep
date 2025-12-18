export type SetupStatus = { setupRequired: boolean };

export type User = {
    id: string;
    email: string;
    displayName: string;
    admin: boolean;
};

export type AdminUser = {
    id: string;
    email: string;
    displayName: string;
    admin: boolean;
    status?: string;
    authProvider?: string;
    providerSubject?: string;
    lastLoginAt?: string;
};

export type InviteValidation = { valid: boolean };

export type ModuleSummary = {
    moduleKey: string;
    name: string;
    version?: string;
    description?: string;
    // Legacy alias for convenience when mapping backend responses that used `key`.
    key?: string;
};

export type Page<T> = {
    content: T[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
};

export type ModuleField = {
    fieldKey: string;
    label: string;
    fieldType: string;
    key?: string;
    required?: boolean;
    order?: number;
    searchable?: boolean;
    filterable?: boolean;
    sortable?: boolean;
    active?: boolean;
    deprecated?: boolean;
    identifiers?: unknown[];
    enumValues?: unknown[];
    providerMappings?: ProviderMapping[];
};

export type ProviderMapping = {
    providerKey: string;
    path: string;
    transform?: string;
};

export type WorkflowStep = {
    type: "PROMPT" | "PROMPT_ANY" | "LOOKUP_METADATA" | "APPLY_METADATA" | "SAVE_ITEM";
    field?: string;
    fields?: string[];
    providers?: string[];
    label?: string;
    extensions?: Record<string, unknown>;
};

export type Workflow = {
    key: string;
    label?: string;
    steps: WorkflowStep[];
    extensions?: Record<string, unknown>;
};

export type ModuleDetails = {
    id?: string;
    moduleKey: string;
    name: string;
    version?: string;
    description?: string;
    states?: { key: string; label?: string }[];
    providers?: { key: string; label?: string; description?: string; supportsIdentifiers?: string[] }[];
    fields?: ModuleField[];
    workflows?: Workflow[];
    key?: string;
};

export type ProviderCredentialField = {
    name: string;
    label: string;
    description?: string;
    secret: boolean;
};

export type ProviderInfo = {
    key: string;
    displayName: string;
    description?: string;
    supportedIdTypes: string[];
    priority?: number | null;
    websiteUrl?: string;
    apiUrl?: string;
    dataReturned?: string;
    highlights?: string[];
    credentialFields?: ProviderCredentialField[];
    credentialsConfigured: boolean;
};

export type ProviderStatus = {
    key: string;
    available: boolean;
    message?: string;
    supportedIdTypes: string[];
    rateLimited?: boolean;
    retryAfterSeconds?: number;
    credentialsRequired: boolean;
    credentialsConfigured: boolean;
};

export type Collection = {
    id: string;
    name: string;
    description?: string;
    role: string;
    createdAt?: string;
};

export type CollectionModule = {
    moduleKey: string;
    moduleName?: string;
    version?: string;
};

export type ProviderLookupResult = {
    request: ProviderLookupRequest;
    best?: ProviderResult;
    results?: ProviderResult[];
    mergedAttributes?: Record<string, unknown>;
    assets?: ProviderAsset[];
};

export type ProviderLookupRequest = {
    moduleId: string;
    identifiers: ItemIdentifier[];
};

export type ProviderAsset = {
    url: string;
    type?: string;
    width?: number;
    height?: number;
};

export type ProviderResult = {
    providerKey: string;
    confidence?: { score?: number; reason?: string } | null;
    raw?: Record<string, unknown>;
    normalized?: Record<string, unknown>;
    normalizedFields?: { json?: string };
    mappedAttributes?: Record<string, unknown>;
};

export type ItemIdentifier = {
    idType: string;
    idValue: string;
};

export type CreateItemRequest = {
    moduleId: string;
    stateKey?: string;
    title?: string;
    attributes: Record<string, unknown>;
    identifiers?: ItemIdentifier[];
};

export type UpdateItemRequest = {
    stateKey?: string | null;
    title?: string | null;
    attributes?: Record<string, unknown> | null;
    identifiers?: ItemIdentifier[] | null;
};

export type Item = {
    id: string;
    moduleId: string;
    collectionId?: string;
    stateKey: string;
    title?: string;
    attributes: Record<string, unknown>;
    identifiers?: ItemIdentifier[];
    createdBy?: string;
    createdAt?: string;
    updatedAt?: string;
};
