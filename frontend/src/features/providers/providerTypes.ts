import type { Attributes } from "@/features/items/itemTypes";
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
  rawData?: Attributes;
  normalizedFields?: Attributes | string | null;
  assets?: ProviderAsset[];
  confidence?: { score?: number; reason?: string };
  error?: string;
};

export type ProviderLookupResponse = {
  results: ProviderLookupResult[];
  best?: ProviderLookupResult | null;
  mergedAttributes: Attributes;
  assets?: ProviderAsset[];
  providerResults?: ProviderLookupResult[];
  merged?: Attributes;
  fieldValues?: Attributes;
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
