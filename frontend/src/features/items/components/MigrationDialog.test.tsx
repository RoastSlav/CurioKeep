import { render, screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { MigrationDialog } from "./MigrationDialog"
import { MigrationNotice } from "./MigrationNotice"

function jsonResponse(body: unknown, status = 200): Response {
    return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } })
}

const PREVIEW = {
    targetVersion: "2.0.0",
    behind: 12,
    changed: 10,
    versions: [{ version: "1.0.0", items: 12 }],
    samples: [{ itemId: "i1", title: "Dune", changes: [{ field: "old_notes", before: "first" }, { field: "notes", after: "first" }] }],
}

describe("MigrationDialog", () => {
    const fetchMock = vi.fn<typeof fetch>()
    const onMigrated = vi.fn()
    const onOpenChange = vi.fn()

    beforeEach(() => {
        fetchMock.mockReset()
        onMigrated.mockReset()
        onOpenChange.mockReset()
        vi.stubGlobal("fetch", fetchMock)
    })

    afterEach(() => {
        vi.unstubAllGlobals()
    })

    function renderDialog() {
        render(<MigrationDialog open onOpenChange={onOpenChange} collectionId="c1" moduleId="m1" onMigrated={onMigrated} />)
    }

    it("shows what would change before anything is written", async () => {
        fetchMock.mockResolvedValue(jsonResponse(PREVIEW))
        renderDialog()

        expect(await screen.findByText(/12 items are on an earlier version/)).toBeInTheDocument()
        expect(screen.getByText(/would change values on 10/)).toBeInTheDocument()
        expect(screen.getByText("Dune")).toBeInTheDocument()
        expect(screen.getByText("old_notes").parentElement).toHaveTextContent('old_notes: "first" → (empty)')
        expect(screen.getByText("notes").parentElement).toHaveTextContent('notes: (empty) → "first"')
        expect(fetchMock).toHaveBeenCalledTimes(1)
        const [url, init] = fetchMock.mock.calls[0]
        expect(url).toBe("/api/collections/c1/items/migration?moduleId=m1")
        expect(init?.method ?? "GET").toBe("GET")
        expect(onMigrated).not.toHaveBeenCalled()
    })

    it("cannot be accepted until the preview has loaded", () => {
        fetchMock.mockReturnValue(new Promise(() => {}))
        renderDialog()

        expect(screen.getByRole("button", { name: "Accept and migrate" })).toBeDisabled()
    })

    it("migrates on accept, reports the outcome and refreshes the list", async () => {
        fetchMock.mockResolvedValueOnce(jsonResponse(PREVIEW)).mockResolvedValueOnce(jsonResponse({ migrated: 12, changed: 10, skipped: 1 }))
        renderDialog()

        await userEvent.click(await screen.findByRole("button", { name: "Accept and migrate" }))

        expect(await screen.findByText(/Brought 12 items up to date; 10 had values changed, 1 could not be read/)).toBeInTheDocument()
        const [url, init] = fetchMock.mock.calls[1]
        expect(url).toBe("/api/collections/c1/items/migration?moduleId=m1")
        expect(init?.method).toBe("POST")
        expect(onMigrated).toHaveBeenCalledTimes(1)
        await userEvent.click(screen.getByRole("button", { name: "Done" }))
        expect(onOpenChange).toHaveBeenCalledWith(false)
    })

    it("shows the server's reason when the migration is refused and leaves the list alone", async () => {
        fetchMock
            .mockResolvedValueOnce(jsonResponse(PREVIEW))
            .mockResolvedValueOnce(jsonResponse({ error: "FORBIDDEN", message: "You need to be an admin of this collection" }, 403))
        renderDialog()

        await userEvent.click(await screen.findByRole("button", { name: "Accept and migrate" }))

        expect(await screen.findByText("You need to be an admin of this collection")).toBeInTheDocument()
        await waitFor(() => expect(screen.getByRole("button", { name: "Accept and migrate" })).toBeEnabled())
        expect(onMigrated).not.toHaveBeenCalled()
    })

    it("reports a preview that could not be read", async () => {
        fetchMock.mockResolvedValue(jsonResponse({ error: "MODULE_NOT_FOUND", message: "Module not found" }, 400))
        renderDialog()

        expect(await screen.findByText("Module not found")).toBeInTheDocument()
        expect(screen.getByRole("button", { name: "Accept and migrate" })).toBeDisabled()
    })
})

describe("MigrationNotice", () => {
    it("renders nothing when no item is waiting", () => {
        const { container } = render(<MigrationNotice pending={0} onReview={vi.fn()} />)

        expect(container).toBeEmptyDOMElement()
    })

    it("says how many items can be brought up to date and offers the review", async () => {
        const onReview = vi.fn()
        render(<MigrationNotice pending={1} onReview={onReview} />)

        expect(screen.getByText(/1 item was saved under an earlier version/)).toBeInTheDocument()
        await userEvent.click(screen.getByRole("button", { name: "Review changes" }))
        expect(onReview).toHaveBeenCalledTimes(1)
    })
})
