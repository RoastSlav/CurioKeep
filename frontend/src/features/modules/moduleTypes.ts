export type FieldType =
  | "TEXT"
  | "NUMBER"
  | "DATE"
  | "BOOLEAN"
  | "ENUM"
  | "TAGS"
  | "LINK"
  | "JSON";

export type FieldEnumValue = {
  key: string;
  label: string;
};

export type FieldFlags = {
  required?: boolean;
  searchable?: boolean;
  filterable?: boolean;
  sortable?: boolean;
  order?: number;
};

export type FieldConstraints = {
  min?: number;
  max?: number;
  minLength?: number;
  maxLength?: number;
  pattern?: string;
  multi?: boolean;
  uniqueWithinCollection?: boolean;
};

export type FieldUI = {
  widget?: string;
  placeholder?: string;
  helpText?: string;
  group?: string;
  hidden?: boolean;
};

export type FieldDef = {
  key: string;
  label: string;
  type: FieldType;
  flags?: FieldFlags;
  enumValues?: FieldEnumValue[];
  constraints?: FieldConstraints;
  ui?: FieldUI;
  identifiers?: string[];
  providerMappings?: { provider: string; path: string; transform?: string }[];
};

export type ModuleStateDef = {
  key: string;
  label: string;
  order?: number;
  active?: boolean;
  deprecated?: boolean;
};

export type ModuleProviderDef = {
  key: string;
  name?: string;
  description?: string;
  supportedIdentifiers?: string[];
};

export type WorkflowStepType =
  | "PROMPT"
  | "PROMPT_ANY"
  | "LOOKUP_METADATA"
  | "APPLY_METADATA"
  | "SELECT_IMAGE"
  | "SAVE_ITEM";

export type WorkflowStep = {
  type: WorkflowStepType;
  field?: string;
  fields?: string[];
  providers?: string[];
  query?: string;
  label?: string;
  extensions?: Record<string, any>;
};

export type WorkflowDef = {
  key: string;
  label?: string;
  steps: WorkflowStep[];
  extensions?: Record<string, any>;
};

export type ModuleDefinition = {
  key: string;
  version: string;
  name: string;
  description?: string;
  states: ModuleStateDef[];
  fields: FieldDef[];
  providers?: ModuleProviderDef[];
  workflows?: WorkflowDef[];
};
