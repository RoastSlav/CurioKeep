export type IdentifierType = "ISBN10" | "ISBN13" | "UPC" | "EAN" | "ASIN" | "CUSTOM";

export type ItemIdentifier = {
    type: IdentifierType;
    value: string;
};

export type Item = {
    id: string;
    collectionId: string;
    moduleId: string;
    stateKey: string;
    attributes: Record<string, any>;
    identifiers?: ItemIdentifier[];
    createdAt?: string;
    updatedAt?: string;
};
