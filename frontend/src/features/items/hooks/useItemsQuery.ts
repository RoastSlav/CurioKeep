import { useEffect, useMemo, useState } from "react";
import type { Item, ModuleDefinition } from "../../../api/types";
import { listItems, type ItemListQuery, type ItemSort } from "../api";

export type FieldFilter =
    | { type: "enum"; values: string[] }
    | { type: "text"; value?: string }
    | { type: "number"; min?: number; max?: number }
    | { type: "date"; from?: string; to?: string };

export type ItemsQueryState = {
    page: number;
    size: number;
    search?: string;
    states?: string[];
    filters?: Record<string, FieldFilter>;
    sort?: ItemSort;
};

export function getDefaultSort(moduleDefinition?: ModuleDefinition | null): ItemSort {
    if (moduleDefinition) {
        const sortable = (moduleDefinition.fields || [])
            .filter((f) => f.flags?.sortable)
            .sort((a, b) => (a.flags?.order ?? Number.MAX_SAFE_INTEGER) - (b.flags?.order ?? Number.MAX_SAFE_INTEGER));

        if (sortable.length) {
            return { field: sortable[0].key, direction: "asc" };
        }

        const titleLike = (moduleDefinition.fields || []).find((f) => /title|name/i.test(f.label || f.key));
        if (titleLike) {
            return { field: titleLike.key, direction: "asc" };
        }
    }
    return { field: "createdAt", direction: "desc" };
}

function normalizeText(value: unknown): string {
    if (value === null || value === undefined) return "";
    return String(value).toLowerCase();
}

function valueMatchesFilter(value: unknown, filter: FieldFilter): boolean {
    if (filter.type === "enum") {
        if (!filter.values.length) return true;
        if (Array.isArray(value)) {
            return value.some((v) => filter.values.includes(String(v)));
        }
        return filter.values.includes(String(value));
    }
    if (filter.type === "text") {
        if (!filter.value) return true;
        return normalizeText(value).includes(filter.value.toLowerCase());
    }
    if (filter.type === "number") {
        const num = typeof value === "number" ? value : Number(value);
        if (Number.isNaN(num)) return false;
        if (filter.min !== undefined && num < filter.min) return false;
        if (filter.max !== undefined && num > filter.max) return false;
        return true;
    }
    if (filter.type === "date") {
        const dateValue = value ? new Date(String(value)).getTime() : NaN;
        if (Number.isNaN(dateValue)) return false;
        if (filter.from && dateValue < new Date(filter.from).getTime()) return false;
        if (filter.to && dateValue > new Date(filter.to).getTime()) return false;
        return true;
    }
    return true;
}

function applyClientFilters(
    items: Item[],
    moduleDefinition: ModuleDefinition | null | undefined,
    search?: string,
    states?: string[],
    filters?: Record<string, FieldFilter>
) {
    let result = items;

    if (states?.length) {
        const stateSet = new Set(states.map((s) => s.toUpperCase()));
        result = result.filter((item) => stateSet.has(item.stateKey.toUpperCase()));
    }

    if (moduleDefinition && search) {
        const searchableFields = (moduleDefinition.fields || []).filter((f) => f.flags?.searchable);
        const term = search.toLowerCase();
        result = result.filter((item) =>
            searchableFields.some((field) => normalizeText(item.attributes?.[field.key]).includes(term))
        );
    }

    if (moduleDefinition && filters) {
        const entries = Object.entries(filters);
        result = result.filter((item) =>
            entries.every(([fieldKey, filter]) => valueMatchesFilter(item.attributes?.[fieldKey], filter))
        );
    }

    return result;
}

function applySort(items: Item[], moduleDefinition: ModuleDefinition | null | undefined, sort?: ItemSort) {
    if (!sort) return items;
    const { field, direction } = sort;
    const dir = direction === "desc" ? -1 : 1;

    const getValue = (item: Item) => {
        if (field === "createdAt" || field === "updatedAt") return item[field];
        return moduleDefinition ? item.attributes?.[field] : undefined;
    };

    return [...items].sort((a, b) => {
        const av = getValue(a);
        const bv = getValue(b);
        if (av === bv) return 0;
        if (av === undefined || av === null) return 1;
        if (bv === undefined || bv === null) return -1;
        if (typeof av === "number" && typeof bv === "number") return (av - bv) * dir;
        return normalizeText(av).localeCompare(normalizeText(bv)) * dir;
    });
}

export function useItemsQuery(
    collectionId: string | undefined,
    moduleId: string | null | undefined,
    moduleDefinition: ModuleDefinition | null | undefined,
    query: ItemsQueryState
) {
    const [items, setItems] = useState<Item[]>([]);
    const [total, setTotal] = useState(0);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const fetchItems = async () => {
            if (!collectionId || !moduleId) return;
            setLoading(true);
            setError(null);
            try {
                const response = await listItems(collectionId, {
                    moduleId,
                    page: query.page,
                    size: query.size,
                    search: query.search,
                    states: query.states,
                    sort: query.sort,
                } as ItemListQuery);

                let filtered = response.content;
                let totalCount = response.totalElements;

                // Backend currently lacks rich filters; apply client-side fallback on the current page.
                const filtersPresent = Boolean((query.states && query.states.length) || query.search || (query.filters && Object.keys(query.filters).length));
                if (filtersPresent || query.sort) {
                    filtered = applyClientFilters(response.content, moduleDefinition, query.search, query.states, query.filters);
                    filtered = applySort(filtered, moduleDefinition, query.sort);
                    totalCount = filtered.length;
                }

                setItems(filtered);
                setTotal(totalCount);
            } catch (err: any) {
                setError(err?.message || "Failed to load items");
            } finally {
                setLoading(false);
            }
        };

        void fetchItems();
    }, [collectionId, moduleId, moduleDefinition, query.page, query.size, query.search, query.states, query.filters, query.sort]);

    return useMemo(
        () => ({ items, total, loading, error }),
        [items, total, loading, error]
    );
}
