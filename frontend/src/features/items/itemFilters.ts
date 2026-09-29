import type { FieldContract } from "@/features/modules/moduleTypes"

export type FieldFilter =
    | { kind: "in"; values: string[] }
    | { kind: "contains"; text: string }
    | { kind: "range"; min?: number; max?: number }
    | { kind: "dates"; from?: string; to?: string }

export type FieldFilters = Record<string, FieldFilter>

/** Which control a field gets in the filter dialog, or null for a type the server cannot filter on. */
export function filterKindFor(field: FieldContract): FieldFilter["kind"] | null {
    switch (field.type) {
        case "ENUM":
        case "BOOLEAN":
        case "TAGS":
            return "in"
        case "TEXT":
        case "LINK":
            return "contains"
        case "NUMBER":
            return "range"
        case "DATE":
            return "dates"
        default:
            return null
    }
}

export function filterableFields(fields: FieldContract[] | undefined): FieldContract[] {
    return (fields ?? [])
        .filter((field) => field.active && field.filterable && filterKindFor(field) !== null)
        .sort((a, b) => a.order - b.order)
}

function isEmpty(filter: FieldFilter): boolean {
    switch (filter.kind) {
        case "in":
            return filter.values.length === 0
        case "contains":
            return filter.text.trim() === ""
        case "range":
            return filter.min === undefined && filter.max === undefined
        case "dates":
            return !filter.from && !filter.to
    }
}

/** The filters that actually restrict something; an empty control is the same as no filter. */
export function cleanFilters(filters: FieldFilters): FieldFilters {
    return Object.fromEntries(Object.entries(filters).filter(([, filter]) => !isEmpty(filter)))
}

export function countActiveFilters(filters: FieldFilters): number {
    return Object.keys(cleanFilters(filters)).length
}

/** The query parameters the server understands: `<fieldKey>.<operator>=<value>`. */
export function filterParams(filters: FieldFilters): Record<string, string> {
    const params: Record<string, string> = {}
    for (const [key, filter] of Object.entries(cleanFilters(filters))) {
        switch (filter.kind) {
            case "in":
                params[`${key}.in`] = filter.values.join(",")
                break
            case "contains":
                params[`${key}.contains`] = filter.text.trim()
                break
            case "range":
                if (filter.min !== undefined) params[`${key}.gte`] = String(filter.min)
                if (filter.max !== undefined) params[`${key}.lte`] = String(filter.max)
                break
            case "dates":
                if (filter.from) params[`${key}.from`] = filter.from
                if (filter.to) params[`${key}.to`] = filter.to
                break
        }
    }
    return params
}
