import type { ModuleDefinition } from "../features/modules/moduleTypes";

export type User = {
    id: string;
    email: string;
    displayName?: string | null;
    roles?: string[];
    isAdmin?: boolean;
};

export type LoginRequest = {
    email: string;
    password: string;
};

export type SetupStatus = {
    setupRequired: boolean;
};

export type Collection = {
    id: string;
    name: string;
    description?: string | null;
    role: "OWNER" | "ADMIN" | "EDITOR" | "VIEWER" | string;
    createdAt?: string;
    itemsCount?: number;
    modulesCount?: number;
    enabledModules?: CollectionModuleRef[];
};

export type CollectionMember = {
    userId: string;
    email: string;
    displayName?: string | null;
    role: "OWNER" | "ADMIN" | "EDITOR" | "VIEWER" | string;
    joinedAt?: string;
};

export type CollectionInvite = {
    token: string;
    role: "ADMIN" | "EDITOR" | "VIEWER" | string;
    expiresAt?: string | null;
    collectionId: string;
};

export type InviteValidateResponse = {
    valid: boolean;
    reason?: string | null;
    collectionId?: string | null;
    role?: "ADMIN" | "EDITOR" | "VIEWER" | string | null;
};

export type CreateCollectionInviteRequest = {
    role: "ADMIN" | "EDITOR" | "VIEWER" | string;
    expiresInDays?: number | null;
};

export type ModuleSummary = {
    id: string;
    moduleKey: string;
    name?: string;
    version?: string;
    source?: "BUILTIN" | "USER" | string;
    updatedAt?: string;
};

export type CollectionModule = {
    moduleKey: string;
    name?: string;
    version?: string;
    source?: string;
    enabledAt?: string;
    moduleId: string;
};

export type CollectionModuleRef = {
    moduleKey: string;
    moduleId: string;
    name?: string;
};

export type CreateCollectionRequest = {
    name: string;
    description?: string;
};

export type ApiResult<T> = T;

export type PagedResult<T> = {
    content: T[];
    totalElements: number;
    totalPages: number;
    number: number;
    size: number;
};

export type ModuleDetails = {
    id: string;
    moduleKey: string;
    name: string;
    version: string;
    source?: string;
    checksum?: string;
    contract: ModuleDefinition;
    createdAt?: string;
    updatedAt?: string;
};

export type {
    ModuleDefinition,
    ModuleStateDef,
    FieldDef,
    FieldFlags,
    FieldConstraints,
    FieldUI,
    ModuleProviderDef,
    WorkflowDef,
    WorkflowStep,
    WorkflowStepType,
} from "../features/modules/moduleTypes";

export type { Item, ItemIdentifier, IdentifierType } from "../features/items/itemTypes";

export type {
    ProviderAsset,
    ProviderLookupRequest,
    ProviderLookupResponse,
    ProviderLookupResult,
} from "../features/providers/providerTypes";
