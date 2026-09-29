export type IdentifierType = "ISBN10" | "ISBN13" | "UPC" | "EAN" | "ASIN" | "CUSTOM";

export type Attributes = Record<string, unknown>;

export type ItemIdentifier = {
    type: IdentifierType;
    value: string;
};

export type Item = {
    id: string;
    collectionId: string;
    moduleId: string;
    stateKey: string;
    attributes: Attributes;
    identifiers?: ItemIdentifier[];
    createdAt?: string;
    updatedAt?: string;
};
