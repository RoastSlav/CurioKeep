import { act, renderHook, waitFor } from "@testing-library/react"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import type { Item } from "@/api/types"
import { useItemList } from "./useItemList"

function item(id: string, stateKey = "OWNED"): Item {
    return { id, collectionId: "c1", moduleId: "m1", stateKey, attributes: { title: id } }
}

type PageBody = { content: Item[]; totalElements: number; totalPages: number; number: number; size: number }

function pageBody(items: Item[], overrides: Partial<PageBody> = {}): PageBody {
    return { content: items, totalElements: items.length, totalPages: 1, number: 0, size: 25, ...overrides }
}

function jsonResponse(body: unknown, status = 200): Response {
    return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } })
}

function deferred<T>() {
    let resolve!: (value: T) => void
    const promise = new Promise<T>((r) => {
        resolve = r
    })
    return { promise, resolve }
}

describe("useItemList", () => {
    const fetchMock = vi.fn<typeof fetch>()

    beforeEach(() => {
        fetchMock.mockReset()
        vi.stubGlobal("fetch", fetchMock)
    })

    afterEach(() => {
        vi.unstubAllGlobals()
    })

    const requestedUrls = () => fetchMock.mock.calls.map(([url]) => new URL(String(url), "http://localhost"))
    const lastUrl = () => requestedUrls().at(-1) as URL

    it("loads the first page of the module from the server", async () => {
        fetchMock.mockResolvedValue(jsonResponse(pageBody([item("a"), item("b")], { totalElements: 40, totalPages: 2 })))

        const { result } = renderHook(() => useItemList("c1", "m1", ""))

        expect(result.current.loading).toBe(true)
        await waitFor(() => expect(result.current.items).toHaveLength(2))
        expect(result.current.total).toBe(40)
        expect(result.current.totalPages).toBe(2)
        expect(result.current.loading).toBe(false)
        expect(lastUrl().pathname).toBe("/api/collections/c1/items")
        expect(lastUrl().searchParams.get("moduleId")).toBe("m1")
        expect(lastUrl().searchParams.get("page")).toBe("0")
        expect(lastUrl().searchParams.get("size")).toBe("25")
        expect(lastUrl().searchParams.has("search")).toBe(false)
    })

    it("does not fetch until it knows the collection and the module", () => {
        renderHook(() => useItemList("c1", undefined, ""))

        expect(fetchMock).not.toHaveBeenCalled()
    })

    it("sends the search text and goes back to the first page when it changes", async () => {
        fetchMock.mockImplementation(async () => jsonResponse(pageBody([item("a")], { totalElements: 60, totalPages: 3 })))
        const { result, rerender } = renderHook(({ search }) => useItemList("c1", "m1", search), { initialProps: { search: "" } })
        await waitFor(() => expect(result.current.items).toHaveLength(1))
        act(() => result.current.setPage(2))
        await waitFor(() => expect(lastUrl().searchParams.get("page")).toBe("2"))

        rerender({ search: "dune" })

        await waitFor(() => expect(lastUrl().searchParams.get("search")).toBe("dune"))
        expect(lastUrl().searchParams.get("page")).toBe("0")
        expect(result.current.page).toBe(0)
    })

    it("sends state, sort and field filters and returns to the first page for each change", async () => {
        fetchMock.mockImplementation(async () => jsonResponse(pageBody([item("a")], { totalElements: 60, totalPages: 3 })))
        const { result } = renderHook(() => useItemList("c1", "m1", ""))
        await waitFor(() => expect(result.current.items).toHaveLength(1))
        act(() => result.current.setPage(2))
        await waitFor(() => expect(result.current.page).toBe(2))

        act(() => result.current.setStates(["WISHLIST"]))
        await waitFor(() => expect(lastUrl().searchParams.get("state")).toBe("WISHLIST"))
        expect(lastUrl().searchParams.get("page")).toBe("0")

        act(() => result.current.setSort({ field: "title", direction: "desc" }))
        await waitFor(() => expect(lastUrl().searchParams.get("sort")).toBe("title,desc"))

        act(() => result.current.setFilters({ publisher: { kind: "contains", text: "ace" }, pages: { kind: "range", min: 100 } }))
        await waitFor(() => expect(lastUrl().searchParams.get("publisher.contains")).toBe("ace"))
        expect(lastUrl().searchParams.get("pages.gte")).toBe("100")
        expect(lastUrl().searchParams.get("state")).toBe("WISHLIST")
    })

    it("changing the page size returns to the first page", async () => {
        fetchMock.mockImplementation(async () => jsonResponse(pageBody([item("a")], { totalElements: 200, totalPages: 8 })))
        const { result } = renderHook(() => useItemList("c1", "m1", ""))
        await waitFor(() => expect(result.current.items).toHaveLength(1))
        act(() => result.current.setPage(3))
        await waitFor(() => expect(result.current.page).toBe(3))

        act(() => result.current.setSize(50))

        await waitFor(() => expect(lastUrl().searchParams.get("size")).toBe("50"))
        expect(lastUrl().searchParams.get("page")).toBe("0")
    })

    it("keeps showing the previous page while the next one loads", async () => {
        const next = deferred<Response>()
        fetchMock.mockResolvedValueOnce(jsonResponse(pageBody([item("a")], { totalElements: 60, totalPages: 3 })))
        fetchMock.mockReturnValueOnce(next.promise)
        const { result } = renderHook(() => useItemList("c1", "m1", ""))
        await waitFor(() => expect(result.current.items).toHaveLength(1))

        act(() => result.current.setPage(1))

        await waitFor(() => expect(result.current.fetching).toBe(true))
        expect(result.current.items.map((i) => i.id)).toEqual(["a"])
        expect(result.current.loading).toBe(false)

        await act(async () => next.resolve(jsonResponse(pageBody([item("b")], { totalElements: 60, totalPages: 3 }))))
        await waitFor(() => expect(result.current.items.map((i) => i.id)).toEqual(["b"]))
        expect(result.current.fetching).toBe(false)
    })

    it("ignores a slow response that was overtaken by a newer request", async () => {
        const slow = deferred<Response>()
        fetchMock.mockReturnValueOnce(slow.promise)
        fetchMock.mockResolvedValueOnce(jsonResponse(pageBody([item("new")])))
        const { result, rerender } = renderHook(({ search }) => useItemList("c1", "m1", search), { initialProps: { search: "d" } })

        rerender({ search: "dune" })
        await waitFor(() => expect(result.current.items.map((i) => i.id)).toEqual(["new"]))
        await act(async () => slow.resolve(jsonResponse(pageBody([item("stale")]))))

        expect(result.current.items.map((i) => i.id)).toEqual(["new"])
    })

    it("steps back to the last page when the current one has become empty", async () => {
        fetchMock.mockImplementation(async (url) => {
            const page = new URL(String(url), "http://localhost").searchParams.get("page")
            return page === "2"
                ? jsonResponse(pageBody([], { totalElements: 50, totalPages: 2, number: 2 }))
                : jsonResponse(pageBody([item("last")], { totalElements: 50, totalPages: 2, number: 1 }))
        })
        const { result } = renderHook(() => useItemList("c1", "m1", ""))
        await waitFor(() => expect(result.current.items).toHaveLength(1))

        act(() => result.current.setPage(2))

        await waitFor(() => expect(result.current.page).toBe(1))
        await waitFor(() => expect(result.current.items.map((i) => i.id)).toEqual(["last"]))
    })

    it("starts over when the module changes", async () => {
        fetchMock.mockImplementation(async (url) => {
            const moduleId = new URL(String(url), "http://localhost").searchParams.get("moduleId")
            return jsonResponse(pageBody([item(`item-of-${moduleId}`)], { totalElements: 60, totalPages: 3 }))
        })
        const { result, rerender } = renderHook(({ moduleId }) => useItemList("c1", moduleId, ""), { initialProps: { moduleId: "m1" } })
        await waitFor(() => expect(result.current.items).toHaveLength(1))
        act(() => result.current.setStates(["WISHLIST"]))
        act(() => result.current.setPage(1))
        await waitFor(() => expect(result.current.page).toBe(1))

        rerender({ moduleId: "m2" })

        expect(result.current.items).toEqual([])
        expect(result.current.page).toBe(0)
        expect(result.current.states).toEqual([])
        await waitFor(() => expect(result.current.items.map((i) => i.id)).toEqual(["item-of-m2"]))
        expect(lastUrl().searchParams.has("state")).toBe(false)
    })

    it("reports a failed request and can retry it", async () => {
        fetchMock.mockResolvedValueOnce(jsonResponse({ error: "INTERNAL_ERROR", message: "Unexpected error" }, 500))
        fetchMock.mockResolvedValueOnce(jsonResponse(pageBody([item("a")])))
        const { result } = renderHook(() => useItemList("c1", "m1", ""))

        await waitFor(() => expect(result.current.error).toBe("Unexpected error"))
        act(() => result.current.reload())

        await waitFor(() => expect(result.current.items).toHaveLength(1))
        expect(result.current.error).toBeNull()
    })

    it("replaces one item in place without a request", async () => {
        fetchMock.mockResolvedValue(jsonResponse(pageBody([item("a"), item("b")])))
        const { result } = renderHook(() => useItemList("c1", "m1", ""))
        await waitFor(() => expect(result.current.items).toHaveLength(2))
        const callsBefore = fetchMock.mock.calls.length

        act(() => result.current.patchItem(item("a", "WISHLIST")))

        expect(result.current.items.map((i) => i.stateKey)).toEqual(["WISHLIST", "OWNED"])
        expect(fetchMock.mock.calls.length).toBe(callsBefore)
    })
})
