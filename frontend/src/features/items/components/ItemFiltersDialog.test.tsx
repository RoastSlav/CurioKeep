import { render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { describe, expect, it, vi } from "vitest"
import { field } from "@/test/fixtures"
import type { FieldContract } from "@/features/modules/moduleTypes"
import type { FieldFilters } from "../itemFilters"
import { ItemFiltersDialog } from "./ItemFiltersDialog"

const FIELDS: FieldContract[] = [
    field({ key: "format", label: "Format", type: "ENUM", filterable: true, order: 1, enumValues: [{ key: "HARDCOVER", label: "Hardcover" }, { key: "PAPERBACK", label: "Paperback" }] }),
    field({ key: "publisher", label: "Publisher", type: "TEXT", filterable: true, order: 2 }),
    field({ key: "pages", label: "Pages", type: "NUMBER", filterable: true, order: 3 }),
    field({ key: "bought", label: "Bought", type: "DATE", filterable: true, order: 4 }),
    field({ key: "signed", label: "Signed", type: "BOOLEAN", filterable: true, order: 5 }),
    field({ key: "labels", label: "Labels", type: "TAGS", filterable: true, order: 6 }),
]

function renderDialog(filters: FieldFilters = {}) {
    const onApply = vi.fn()
    const onOpenChange = vi.fn()
    render(<ItemFiltersDialog open onOpenChange={onOpenChange} fields={FIELDS} filters={filters} onApply={onApply} />)
    return { onApply, onOpenChange }
}

describe("ItemFiltersDialog", () => {
    it("applies the choices made for each kind of field", async () => {
        const { onApply, onOpenChange } = renderDialog()

        await userEvent.click(screen.getByRole("checkbox", { name: "Hardcover" }))
        await userEvent.click(screen.getByRole("checkbox", { name: "Paperback" }))
        await userEvent.type(screen.getByLabelText("Publisher contains"), "Ace")
        await userEvent.type(screen.getByLabelText("Pages from"), "100")
        await userEvent.type(screen.getByLabelText("Pages to"), "500")
        await userEvent.type(screen.getByLabelText("Bought from"), "2020-01-01")
        await userEvent.click(screen.getByRole("checkbox", { name: "Yes" }))
        await userEvent.type(screen.getByLabelText("Labels, comma separated"), "sci-fi, classic")
        await userEvent.click(screen.getByRole("button", { name: "Apply" }))

        expect(onApply).toHaveBeenCalledWith({
            format: { kind: "in", values: ["HARDCOVER", "PAPERBACK"] },
            publisher: { kind: "contains", text: "Ace" },
            pages: { kind: "range", min: 100, max: 500 },
            bought: { kind: "dates", from: "2020-01-01" },
            signed: { kind: "in", values: ["true"] },
            labels: { kind: "in", values: ["sci-fi", "classic"] },
        })
        expect(onOpenChange).toHaveBeenCalledWith(false)
    })

    it("starts from the filters already in effect", () => {
        renderDialog({
            format: { kind: "in", values: ["PAPERBACK"] },
            publisher: { kind: "contains", text: "Ace" },
            pages: { kind: "range", min: 10 },
        })

        expect(screen.getByRole("checkbox", { name: "Paperback" })).toBeChecked()
        expect(screen.getByRole("checkbox", { name: "Hardcover" })).not.toBeChecked()
        expect(screen.getByLabelText("Publisher contains")).toHaveValue("Ace")
        expect(screen.getByLabelText("Pages from")).toHaveValue(10)
    })

    it("drops the controls that were left empty", async () => {
        const { onApply } = renderDialog()

        await userEvent.click(screen.getByRole("button", { name: "Apply" }))

        expect(onApply).toHaveBeenCalledWith({})
    })

    it("clears everything with one button", async () => {
        const { onApply } = renderDialog({ format: { kind: "in", values: ["PAPERBACK"] }, publisher: { kind: "contains", text: "Ace" } })

        await userEvent.click(screen.getByRole("button", { name: "Clear all" }))
        await userEvent.click(screen.getByRole("button", { name: "Apply" }))

        expect(onApply).toHaveBeenCalledWith({})
    })

    it("does not apply anything when cancelled", async () => {
        const { onApply, onOpenChange } = renderDialog()

        await userEvent.click(screen.getByRole("checkbox", { name: "Hardcover" }))
        await userEvent.click(screen.getByRole("button", { name: "Cancel" }))

        expect(onApply).not.toHaveBeenCalled()
        expect(onOpenChange).toHaveBeenCalledWith(false)
    })

    it("un-ticking a choice removes it from the filter", async () => {
        const { onApply } = renderDialog({ format: { kind: "in", values: ["HARDCOVER", "PAPERBACK"] } })

        await userEvent.click(screen.getByRole("checkbox", { name: "Hardcover" }))
        await userEvent.click(screen.getByRole("button", { name: "Apply" }))

        expect(onApply).toHaveBeenCalledWith({ format: { kind: "in", values: ["PAPERBACK"] } })
    })
})
