import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { buildListParams, exportUrl, fetchItemCounts, importItems, listItems } from "./api"

describe("buildListParams", () => {
    it("always names the module and the page", () => {
        const params = new URLSearchParams(buildListParams({ moduleId: "m1" }))

        expect(params.get("moduleId")).toBe("m1")
        expect(params.get("page")).toBe("0")
        expect(params.get("size")).toBe("25")
        expect([...params.keys()]).toEqual(["moduleId", "page", "size"])
    })

    it("adds search, states, sort and field filters only when they are set", () => {
        const params = new URLSearchParams(
            buildListParams({
                moduleId: "m1",
                page: 3,
                size: 50,
                search: "dune",
                states: ["OWNED", "WISHLIST"],
                sort: { field: "title", direction: "desc" },
                filters: { "format.in": "HARDCOVER,PAPERBACK", "pages.gte": "100" },
            }),
        )

        expect(params.get("page")).toBe("3")
        expect(params.get("size")).toBe("50")
        expect(params.get("search")).toBe("dune")
        expect(params.get("state")).toBe("OWNED,WISHLIST")
        expect(params.get("sort")).toBe("title,desc")
        expect(params.get("format.in")).toBe("HARDCOVER,PAPERBACK")
        expect(params.get("pages.gte")).toBe("100")
    })

    it("leaves empty search and states out", () => {
        const params = new URLSearchParams(buildListParams({ moduleId: "m1", search: "", states: [] }))

        expect(params.has("search")).toBe(false)
        expect(params.has("state")).toBe(false)
    })

    it("encodes text that would break a query string", () => {
        const text = buildListParams({ moduleId: "m1", search: "a&b=c #d" })

        expect(new URLSearchParams(text).get("search")).toBe("a&b=c #d")
        expect(text).not.toContain("a&b")
    })
})

describe("item requests", () => {
    const fetchMock = vi.fn<typeof fetch>()

    beforeEach(() => {
        fetchMock.mockReset()
        vi.stubGlobal("fetch", fetchMock)
    })

    afterEach(() => {
        vi.unstubAllGlobals()
    })

    it("asks the server every time and lets the caller cancel a request", async () => {
        fetchMock.mockImplementation(async () => new Response(JSON.stringify({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 25 }), { headers: { "Content-Type": "application/json" } }))
        const controller = new AbortController()

        await listItems("c1", { moduleId: "m1" }, { signal: controller.signal })
        await listItems("c1", { moduleId: "m1" })

        expect(fetchMock).toHaveBeenCalledTimes(2)
        expect(fetchMock.mock.calls[0][1]?.signal).toBe(controller.signal)
    })

    it("loads the counts of a collection", async () => {
        fetchMock.mockResolvedValue(new Response(JSON.stringify({ modules: { m1: { total: 5, byState: { OWNED: 5 } } } }), { headers: { "Content-Type": "application/json" } }))

        const counts = await fetchItemCounts("c1")

        expect(fetchMock.mock.calls[0][0]).toBe("/api/collections/c1/items/counts")
        expect(counts.modules.m1.total).toBe(5)
    })
})

describe("export and import", () => {
    it("links to the export of a collection, optionally limited to one module", () => {
        expect(exportUrl("c1", "json")).toBe("/api/collections/c1/export?format=json")
        expect(exportUrl("c1", "csv", "m1")).toBe("/api/collections/c1/export?format=csv&moduleId=m1")
    })

    it("uploads a file as form data", async () => {
        const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(new Response(JSON.stringify({ imported: 1, failed: 0, errors: [] }), { headers: { "Content-Type": "application/json" } }))
        vi.stubGlobal("fetch", fetchMock)
        try {
            const result = await importItems("c1", new File(["{}"], "export.json"))

            expect(result.imported).toBe(1)
            expect(fetchMock.mock.calls[0][0]).toBe("/api/collections/c1/import")
            expect(fetchMock.mock.calls[0][1]?.body).toBeInstanceOf(FormData)
        } finally {
            vi.unstubAllGlobals()
        }
    })
})
