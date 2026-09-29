import { render, screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { ImportItemsDialog } from "./ImportItemsDialog"

function jsonResponse(body: unknown, status = 200): Response {
    return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } })
}

describe("ImportItemsDialog", () => {
    const fetchMock = vi.fn<typeof fetch>()
    const onImported = vi.fn()
    const onOpenChange = vi.fn()

    beforeEach(() => {
        fetchMock.mockReset()
        onImported.mockReset()
        onOpenChange.mockReset()
        vi.stubGlobal("fetch", fetchMock)
    })

    afterEach(() => {
        vi.unstubAllGlobals()
    })

    function renderDialog() {
        render(<ImportItemsDialog open onOpenChange={onOpenChange} collectionId="c1" onImported={onImported} />)
    }

    const exportFile = () => new File(['{"format":"curiokeep-export","version":1,"items":[]}'], "export.json", { type: "application/json" })

    it("cannot import until a file is chosen", () => {
        renderDialog()

        expect(screen.getByRole("button", { name: "Import" })).toBeDisabled()
    })

    it("uploads the chosen file as multipart form data to the collection", async () => {
        fetchMock.mockResolvedValue(jsonResponse({ imported: 3, failed: 0, errors: [] }))
        renderDialog()

        await userEvent.upload(screen.getByLabelText("Export file"), exportFile())
        await userEvent.click(screen.getByRole("button", { name: "Import" }))

        await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1))
        const [url, init] = fetchMock.mock.calls[0]
        expect(url).toBe("/api/collections/c1/import")
        expect(init?.method).toBe("POST")
        expect(init?.body).toBeInstanceOf(FormData)
        expect((init?.body as FormData).get("file")).toBeInstanceOf(File)
        expect((init?.headers as Record<string, string>)["Content-Type"]).toBeUndefined()
    })

    it("reports what was imported and refreshes the list", async () => {
        fetchMock.mockResolvedValue(jsonResponse({ imported: 3, failed: 0, errors: [] }))
        renderDialog()

        await userEvent.upload(screen.getByLabelText("Export file"), exportFile())
        await userEvent.click(screen.getByRole("button", { name: "Import" }))

        expect(await screen.findByText(/Imported 3 items/)).toBeInTheDocument()
        expect(onImported).toHaveBeenCalledTimes(1)
        await userEvent.click(screen.getByRole("button", { name: "Done" }))
        expect(onOpenChange).toHaveBeenCalledWith(false)
    })

    it("lists the items that were skipped and says when there are more than are shown", async () => {
        fetchMock.mockResolvedValue(jsonResponse({
            imported: 1,
            failed: 60,
            errors: [{ index: 4, reason: "INVALID_FIELD_pages" }, { index: 9, reason: "MODULE_NOT_ENABLED" }],
        }))
        renderDialog()

        await userEvent.upload(screen.getByLabelText("Export file"), exportFile())
        await userEvent.click(screen.getByRole("button", { name: "Import" }))

        expect(await screen.findByText(/Imported 1 item, skipped 60/)).toBeInTheDocument()
        expect(screen.getByText("Item 5: INVALID_FIELD_pages")).toBeInTheDocument()
        expect(screen.getByText("Item 10: MODULE_NOT_ENABLED")).toBeInTheDocument()
        expect(screen.getByText("…and 58 more.")).toBeInTheDocument()
    })

    it("does not refresh the list when nothing was imported", async () => {
        fetchMock.mockResolvedValue(jsonResponse({ imported: 0, failed: 1, errors: [{ index: 0, reason: "MODULE_NOT_ENABLED" }] }))
        renderDialog()

        await userEvent.upload(screen.getByLabelText("Export file"), exportFile())
        await userEvent.click(screen.getByRole("button", { name: "Import" }))

        await screen.findByText(/Imported 0 items, skipped 1/)
        expect(onImported).not.toHaveBeenCalled()
    })

    it("shows the server's reason when the file is refused and lets the user try again", async () => {
        fetchMock.mockResolvedValue(jsonResponse({ error: "INVALID_EXPORT_FILE", message: "That is not a CurioKeep export" }, 400))
        renderDialog()

        await userEvent.upload(screen.getByLabelText("Export file"), exportFile())
        await userEvent.click(screen.getByRole("button", { name: "Import" }))

        expect(await screen.findByText("That is not a CurioKeep export")).toBeInTheDocument()
        expect(screen.getByRole("button", { name: "Import" })).toBeEnabled()
        expect(onImported).not.toHaveBeenCalled()
    })
})
