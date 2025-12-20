import type { IdentifierType, ItemIdentifier } from "../items/itemTypes";

export type ProviderAsset = {
  url: string;
  type?: string;
  label?: string;
  providerKey?: string;
};

export type ProviderLookupIdentifier = {
  idType: IdentifierType;
  idValue: string;
};

export type ProviderLookupRequest = {
  moduleId: string;
  identifiers: Array<ProviderLookupIdentifier | ItemIdentifier>;
  providers?: string[];
  query?: string;
};

export type ProviderLookupResult = {
  providerKey: string;
  rawData?: Record<string, any>;
  normalizedFields?: Record<string, any> | string | null;
  assets?: ProviderAsset[];
  confidence?: { score?: number; reason?: string };
  error?: string;
};

export type ProviderLookupResponse = {
  results: ProviderLookupResult[];
  best?: ProviderLookupResult | null;
  mergedAttributes: Record<string, any>;
  assets?: ProviderAsset[];
  providerResults?: ProviderLookupResult[];
  merged?: Record<string, any>;
  fieldValues?: Record<string, any>;
};

export type Provider = {
  key: string;
  displayName: string;
  description?: string;
  supportedIdTypes: IdentifierType[];
  priority?: number;
  websiteUrl?: string;
  apiUrl?: string;
  dataReturned?: string;
  highlights?: string[];
  credentialFields: CredentialField[];
  credentialsConfigured: boolean;
};

export type CredentialField = {
  name: string;
  label: string;
  helpText?: string;
  secret: boolean;
  required: boolean;
};

export type CredentialStatus = {
  key: string;
  credentialsConfigured: boolean;
};

export type UpdateCredentialsRequest = {
  values: Record<string, string>;
};
