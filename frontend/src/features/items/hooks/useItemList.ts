import { useCallback, useEffect, useMemo, useState } from "react"
import { getErrorMessage } from "@/api/errors"
import type { Item } from "@/api/types"
import { DEFAULT_PAGE_SIZE, listItems, type ItemSort } from "../api"
import { filterParams, type FieldFilters } from "../itemFilters"

type Controls = {
    page: number
    size: number
    states: string[]
    sort: ItemSort | null
    filters: FieldFilters
}

type Result = {
    items: Item[]
    total: number
    totalPages: number
}

const INITIAL_CONTROLS: Controls = { page: 0, size: DEFAULT_PAGE_SIZE, states: [], sort: null, filters: {} }

/**
 * One module's items in a collection, fetched page by page from the server with the given search text plus the
 * state, sort and filter controls held here. The previous page stays in place while the next one loads, so the list
 * does not blank out on every keystroke or click.
 *
 * Changing the module starts over; changing the search or any control returns to the first page.
 */
export function useItemList(collectionId: string | undefined, moduleId: string | undefined, search: string) {
    const [controls, setControls] = useState<Controls>(INITIAL_CONTROLS)
    const [result, setResult] = useState<Result | null>(null)
    const [fetching, setFetching] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [reloadCount, setReloadCount] = useState(0)

    // Adjusting state while rendering, as React recommends, instead of resetting it in an effect after a stale render.
    const scope = `${collectionId ?? ""}/${moduleId ?? ""}`
    const [seenScope, setSeenScope] = useState(scope)
    if (seenScope !== scope) {
        setSeenScope(scope)
        setControls(INITIAL_CONTROLS)
        setResult(null)
        setError(null)
    }
    const [seenSearch, setSeenSearch] = useState(search)
    if (seenSearch !== search) {
        setSeenSearch(search)
        setControls((current) => ({ ...current, page: 0 }))
    }

    useEffect(() => {
        if (!collectionId || !moduleId) return
        const controller = new AbortController()
        setFetching(true)
        setError(null)
        listItems(
            collectionId,
            {
                moduleId,
                page: controls.page,
                size: controls.size,
                search: search || undefined,
                states: controls.states,
                sort: controls.sort ?? undefined,
                filters: filterParams(controls.filters),
            },
            { signal: controller.signal },
        )
            .then((page) => {
                if (controller.signal.aborted) return // a newer request has replaced this one
                // Deleting the last items of the last page leaves it empty; step back to the page that still exists.
                if (page.content.length === 0 && controls.page > 0 && page.totalPages > 0) {
                    setControls((current) => ({ ...current, page: page.totalPages - 1 }))
                    return
                }
                setResult({ items: page.content, total: page.totalElements, totalPages: page.totalPages })
                setFetching(false)
            })
            .catch((err: unknown) => {
                if (controller.signal.aborted) return
                setError(getErrorMessage(err, "Failed to load items"))
                setFetching(false)
            })
        return () => controller.abort()
    }, [collectionId, moduleId, search, controls, reloadCount])

    const setPage = useCallback((page: number) => setControls((current) => ({ ...current, page })), [])
    const setSize = useCallback((size: number) => setControls((current) => ({ ...current, size, page: 0 })), [])
    const setStates = useCallback((states: string[]) => setControls((current) => ({ ...current, states, page: 0 })), [])
    const setSort = useCallback((sort: ItemSort | null) => setControls((current) => ({ ...current, sort, page: 0 })), [])
    const setFilters = useCallback((filters: FieldFilters) => setControls((current) => ({ ...current, filters, page: 0 })), [])
    const reload = useCallback(() => setReloadCount((count) => count + 1), [])
    const patchItem = useCallback(
        (updated: Item) =>
            setResult((current) =>
                current ? { ...current, items: current.items.map((item) => (item.id === updated.id ? updated : item)) } : current,
            ),
        [],
    )

    return useMemo(
        () => ({
            items: result?.items ?? [],
            total: result?.total ?? 0,
            totalPages: result?.totalPages ?? 0,
            page: controls.page,
            size: controls.size,
            states: controls.states,
            sort: controls.sort,
            filters: controls.filters,
            /** True until the first page has arrived; afterwards the list keeps showing the last page. */
            loading: result === null && fetching,
            fetching,
            error,
            setPage,
            setSize,
            setStates,
            setSort,
            setFilters,
            reload,
            patchItem,
        }),
        [result, controls, fetching, error, setPage, setSize, setStates, setSort, setFilters, reload, patchItem],
    )
}
