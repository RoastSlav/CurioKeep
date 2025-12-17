export type SetupStatus = { setupRequired: boolean };

export type User = {
    id: string;
    email: string;
    displayName: string;
    admin: boolean;
};

export type ModuleSummary = {
    key: string;
    name: string;
    version?: string;
    description?: string;
};

export type ModuleField = {
    fieldKey: string;
    label: string;
    fieldType: string;
    required?: boolean;
};

export type ModuleDetails = {
    key: string;
    name: string;
    version?: string;
    description?: string;
    states?: { key: string; label?: string }[];
    providers?: { key: string; label?: string; description?: string }[];
    fields?: ModuleField[];
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
