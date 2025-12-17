export type SetupStatus = { setupRequired: boolean };

export type User = {
    id: string;
    email: string;
    displayName: string;
    admin: boolean;
};

export type ModuleSummary = {
    moduleKey: string;
    name: string;
    version?: string;
    description?: string;
    // Legacy alias for convenience when mapping backend responses that used `key`.
    key?: string;
};

export type ModuleField = {
    fieldKey: string;
    label: string;
    fieldType: string;
    required?: boolean;
};

export type ModuleDetails = {
    moduleKey: string;
    name: string;
    version?: string;
    description?: string;
    states?: { key: string; label?: string }[];
    providers?: { key: string; label?: string; description?: string }[];
    fields?: ModuleField[];
    key?: string;
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
    request: {
        moduleId: string;
        identifiers: { idType: string; idValue: string }[];
    };
    best?: unknown;
    results?: unknown[];
    assets?: { url: string }[];
};
