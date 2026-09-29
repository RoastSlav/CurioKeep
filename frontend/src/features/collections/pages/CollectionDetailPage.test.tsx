import { render, screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { MemoryRouter, Route, Routes } from "react-router-dom"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { clearAllCached } from "@/api/cache"
import { AuthContext, type AuthContextValue } from "@/auth/authState"
import { ToastProvider } from "@/components/Toasts"
import { ThemeProvider } from "@/contexts/ThemeProvider"
import { field } from "@/test/fixtures"
import CollectionDetailPage from "./CollectionDetailPage"

const CONTRACT = {
    key: "books",
    version: "1.0.0",
    name: "Books",
    states: [
        { key: "OWNED", label: "Owned", order: 1, active: true, deprecated: false },
        { key: "WISHLIST", label: "Wishlist", order: 2, active: true, deprecated: false },
    ],
    providers: [],
    fields: [
        field({ key: "title", label: "Title", searchable: true, sortable: true, order: 1 }),
        field({ key: "publisher", label: "Publisher", filterable: true, order: 2 }),
        field({ key: "old_publisher", label: "Publisher text", deprecated: true, replacedBy: "publisher", order: 9 }),
        field({
            key: "format",
            label: "Format",
            type: "ENUM",
            filterable: true,
            order: 3,
            enumValues: [{ key: "HARDCOVER", label: "Hardcover" }, { key: "PAPERBACK", label: "Paperback" }],
        }),
    ],
    workflows: [],
    extensions: {},
}

function jsonResponse(body: unknown): Response {
    return new Response(JSON.stringify(body), { status: 200, headers: { "Content-Type": "application/json" } })
}

function books(from: number, count: number) {
    return Array.from({ length: count }, (_, i) => {
        const n = String(from + i).padStart(2, "0")
        return { id: `item-${n}`, collectionId: "c1", moduleId: "m1", stateKey: "OWNED", attributes: { title: `Book ${n}` } }
    })
}

const auth: AuthContextValue = {
    user: { id: "u1", email: "u@example.test", displayName: "U", isAdmin: false },
    loading: false,
    error: null,
    login: vi.fn(),
    logout: vi.fn(),
    refreshMe: vi.fn(),
}

describe("CollectionDetailPage items", () => {
    const fetchMock = vi.fn<typeof fetch>()

    /** The answer for an item list request; tests override it to simulate what the server would return. */
    let listAnswer: (params: URLSearchParams) => { content: unknown[]; totalElements: number; totalPages: number }
    let countsAnswer: unknown

    beforeEach(() => {
        clearAllCached()
        countsAnswer = { modules: { m1: { total: 60, byState: { OWNED: 50, WISHLIST: 10 }, deprecatedFieldUse: {} } } }
        listAnswer = (params) => {
            const page = Number(params.get("page"))
            return { content: books(page * 25 + 1, 25), totalElements: 60, totalPages: 3 }
        }
        fetchMock.mockReset()
        fetchMock.mockImplementation(async (input) => {
            const url = new URL(String(input), "http://localhost")
            switch (url.pathname) {
                case "/api/collections/c1":
                    return jsonResponse({ id: "c1", name: "My Books", role: "OWNER" })
                case "/api/collections/c1/modules":
                    return jsonResponse([{ moduleKey: "books", name: "Books", version: "1.0.0", moduleId: "m1" }])
                case "/api/modules":
                    return jsonResponse([])
                case "/api/modules/books":
                    return jsonResponse({ id: "m1", moduleKey: "books", name: "Books", version: "1.0.0", source: "BUILTIN", checksum: "x", contract: CONTRACT, createdAt: "", updatedAt: "" })
                case "/api/collections/c1/items/counts":
                    return jsonResponse(countsAnswer)
                case "/api/collections/c1/items":
                    return jsonResponse(listAnswer(url.searchParams))
                default:
                    return new Response("{}", { status: 404, headers: { "Content-Type": "application/json" } })
            }
        })
        vi.stubGlobal("fetch", fetchMock)
    })

    afterEach(() => {
        vi.unstubAllGlobals()
    })

    const listRequests = () =>
        fetchMock.mock.calls.map(([input]) => new URL(String(input), "http://localhost")).filter((url) => url.pathname === "/api/collections/c1/items")
    const lastListRequest = () => listRequests().at(-1) as URL

    // a row prints its title in more than one place, so "present" means at least one match
    const titleShown = async (title: string) => (await screen.findAllByText(title)).length > 0
    const titleGone = (title: string) => screen.queryAllByText(title).length === 0

    function renderPage() {
        render(
            <MemoryRouter initialEntries={["/collections/c1"]}>
                <AuthContext.Provider value={auth}>
                    <ThemeProvider>
                        <ToastProvider>
                            <Routes>
                                <Route path="/collections/:id" element={<CollectionDetailPage />} />
                            </Routes>
                        </ToastProvider>
                    </ThemeProvider>
                </AuthContext.Provider>
            </MemoryRouter>,
        )
    }

    it("shows the totals from the server and the first page of a longer list", async () => {
        renderPage()

        expect(await titleShown("Book 01")).toBe(true)
        expect(screen.getAllByText("Book 25").length).toBeGreaterThan(0)
        expect(titleGone("Book 26")).toBe(true)
        expect(screen.getByText("Showing 1–25 of 60")).toBeInTheDocument()
        // the stats describe the whole module, not the 25 rows on screen
        expect(await screen.findByText("Total: 60")).toBeInTheDocument()
        expect(screen.getByText("Owned: 50")).toBeInTheDocument()
        expect(screen.getByText("Wishlist: 10")).toBeInTheDocument()
    })

    it("fetches the next page from the server when asked", async () => {
        renderPage()
        await titleShown("Book 01")

        await userEvent.click(screen.getByRole("button", { name: "Next page" }))

        expect(await titleShown("Book 26")).toBe(true)
        expect(screen.getByText("Showing 26–50 of 60")).toBeInTheDocument()
        expect(lastListRequest().searchParams.get("page")).toBe("1")
    })

    it("searches on the server after the typing pauses, from the first page", async () => {
        renderPage()
        await titleShown("Book 01")
        await userEvent.click(screen.getByRole("button", { name: "Next page" }))
        await titleShown("Book 26")
        listAnswer = () => ({ content: books(7, 1), totalElements: 1, totalPages: 1 })

        await userEvent.type(screen.getByRole("searchbox", { name: "Search items" }), "seven")

        await waitFor(() => expect(lastListRequest().searchParams.get("search")).toBe("seven"), { timeout: 3000 })
        expect(lastListRequest().searchParams.get("page")).toBe("0")
        expect(await titleShown("Book 07")).toBe(true)
        expect(screen.getByText("Showing 1–1 of 1")).toBeInTheDocument()
        // one request per pause, not one per keystroke
        expect(listRequests().filter((url) => url.searchParams.has("search")).length).toBeLessThanOrEqual(2)
    })

    it("filters by state on the server when a state is chosen", async () => {
        renderPage()
        await titleShown("Book 01")

        await userEvent.click(await screen.findByText("Wishlist: 10"))

        await waitFor(() => expect(lastListRequest().searchParams.get("state")).toBe("WISHLIST"))
        expect(lastListRequest().searchParams.get("page")).toBe("0")
    })

    it("applies a field filter from the filter dialog", async () => {
        renderPage()
        await titleShown("Book 01")

        await userEvent.click(await screen.findByRole("button", { name: /Filters/ }))
        await userEvent.click(await screen.findByRole("checkbox", { name: "Hardcover" }))
        await userEvent.click(screen.getByRole("button", { name: "Apply" }))

        await waitFor(() => expect(lastListRequest().searchParams.get("format.in")).toBe("HARDCOVER"))
        expect(await screen.findByLabelText("1 active")).toBeInTheDocument()
    })

    it("sorts on the server", async () => {
        renderPage()
        await titleShown("Book 01")

        await userEvent.click(screen.getByRole("button", { name: /switch to ascending/ }))

        await waitFor(() => expect(lastListRequest().searchParams.get("sort")).toBe("createdAt,asc"))
    })

    it("points out items that still use a deprecated field and can show just those", async () => {
        countsAnswer = { modules: { m1: { total: 60, byState: { OWNED: 50, WISHLIST: 10 }, deprecatedFieldUse: { old_publisher: 7 } } } }
        renderPage()
        await titleShown("Book 01")

        expect(await screen.findByText(/7 items/)).toBeInTheDocument()
        expect(screen.getByText(/replaced by publisher/)).toBeInTheDocument()
        await userEvent.click(screen.getByRole("button", { name: "Show these items" }))

        await waitFor(() => expect(lastListRequest().searchParams.get("old_publisher.has")).toBe("true"))
        expect(lastListRequest().searchParams.get("page")).toBe("0")
        await userEvent.click(await screen.findByRole("button", { name: "Show all items" }))
        await waitFor(() => expect(lastListRequest().searchParams.has("old_publisher.has")).toBe(false))
    })

    it("shows no deprecation notice when no item uses a deprecated field", async () => {
        renderPage()
        await titleShown("Book 01")

        expect(screen.queryByText(/deprecated/i)).not.toBeInTheDocument()
    })

    it("says nothing matched, instead of that the module is empty, when a filter finds nothing", async () => {
        renderPage()
        await titleShown("Book 01")
        listAnswer = () => ({ content: [], totalElements: 0, totalPages: 0 })

        await userEvent.click(await screen.findByText("Wishlist: 10"))

        expect(await screen.findByText("No matching items")).toBeInTheDocument()
        expect(screen.queryByText(/No items in/)).not.toBeInTheDocument()
    })

    it("keeps the current rows on screen while the next page is loading", async () => {
        renderPage()
        await titleShown("Book 01")
        let release: (response: Response) => void = () => {}
        const held = new Promise<Response>((resolve) => {
            release = resolve
        })
        const answer = fetchMock.getMockImplementation()
        fetchMock.mockImplementation(async (input, init) => {
            const url = new URL(String(input), "http://localhost")
            return url.pathname === "/api/collections/c1/items" && url.searchParams.get("page") === "1" ? held : (answer as typeof fetch)(input, init)
        })

        await userEvent.click(screen.getByRole("button", { name: "Next page" }))

        expect(screen.getAllByText("Book 01").length).toBeGreaterThan(0)
        expect(screen.queryByText("Loading items...")).not.toBeInTheDocument()
        release(jsonResponse({ content: books(26, 25), totalElements: 60, totalPages: 3 }))
        expect(await titleShown("Book 26")).toBe(true)
    })
})
