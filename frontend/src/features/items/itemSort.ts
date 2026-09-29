import type { FieldContract } from "@/features/modules/moduleTypes"
import type { ItemSort } from "./api"

export type SortOption = { value: string; label: string }

/** What the server does when no sort is given. */
export const DEFAULT_SORT: ItemSort = { field: "createdAt", direction: "desc" }

const BUILT_IN: SortOption[] = [
    { value: "createdAt", label: "Date added" },
    { value: "updatedAt", label: "Last updated" },
    { value: "title", label: "Title" },
]

/** The built-in orders plus every active field the module marks sortable. */
export function sortOptions(fields: FieldContract[] | undefined): SortOption[] {
    const builtIn = new Set(BUILT_IN.map((option) => option.value))
    const fromModule = (fields ?? [])
        .filter((field) => field.active && field.sortable && !builtIn.has(field.key))
        .sort((a, b) => a.order - b.order)
        .map((field) => ({ value: field.key, label: field.label || field.key }))
    return [...BUILT_IN, ...fromModule]
}

/** Dates read best newest first, everything else A to Z or low to high. */
export function defaultDirection(field: string): ItemSort["direction"] {
    return field === "createdAt" || field === "updatedAt" ? "desc" : "asc"
}
