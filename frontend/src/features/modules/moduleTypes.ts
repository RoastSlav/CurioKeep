import type { IdentifierType } from "../items/itemTypes";

// Mirrors the compiled module contract the backend returns (org.rostislav.curiokeep.modules.contract).
// Field flags such as `required` and `filterable` are direct properties, not nested under a `flags` object.

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

export type Constraints = {
  min?: number;
  max?: number;
  minLength?: number;
  maxLength?: number;
  pattern?: string;
  multi?: boolean;
  uniqueWithinCollection?: boolean;
};

export type UiHints = {
  widget?: string;
  placeholder?: string;
  helpText?: string;
  group?: string;
  hidden?: boolean;
};

export type ProviderMapping = {
  provider: string;
  path: string;
  transform?: "TRIM" | "JOIN_COMMA" | "FIRST" | "TO_INT";
};

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
  constraints?: Constraints;
  ui?: UiHints;
  providerMappings: ProviderMapping[];
  /** Key of the field that takes over from this deprecated one. */
  replacedBy?: string;
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

// SELECT_IMAGE is inserted client-side by WorkflowRunner; the backend never sends it.
export type WorkflowStepType =
  | "PROMPT"
  | "PROMPT_ANY"
  | "LOOKUP_METADATA"
  | "APPLY_METADATA"
  | "SAVE_ITEM"
  | "SELECT_IMAGE";

export type WorkflowStep = {
  type: WorkflowStepType;
  field?: string;
  fields?: string[];
  providers?: string[];
  query?: string;
  label?: string;
};

export type WorkflowContract = {
  key: string;
  label?: string;
  steps: WorkflowStep[];
};

export type Author = {
  name?: string;
  email?: string;
  url?: string;
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
