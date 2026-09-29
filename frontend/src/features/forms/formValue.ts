/** Narrows a stored attribute value to something an `<input>` can display. */
export function toInputValue(value: unknown): string | number {
    if (typeof value === "string" || typeof value === "number") return value;
    if (typeof value === "boolean") return String(value);
    return "";
}

export function toStringList(value: unknown): string[] {
    return Array.isArray(value) ? value.filter((entry): entry is string => typeof entry === "string") : [];
}
