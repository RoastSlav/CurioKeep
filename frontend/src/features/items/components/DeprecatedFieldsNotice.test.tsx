import { render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { describe, expect, it, vi } from "vitest"
import { field } from "@/test/fixtures"
import { DeprecatedFieldsNotice } from "./DeprecatedFieldsNotice"

const usage = [
    { field: field({ key: "old_authors", label: "Author text", deprecated: true, replacedBy: "authors" }), count: 42 },
    { field: field({ key: "old_note", label: "Old note", deprecated: true }), count: 1 },
]

describe("DeprecatedFieldsNotice", () => {
    it("renders nothing when no item uses a deprecated field", () => {
        const { container } = render(<DeprecatedFieldsNotice usage={[]} onShow={vi.fn()} onClear={vi.fn()} />)

        expect(container).toBeEmptyDOMElement()
    })

    it("says how many items use each deprecated field and what replaces it", () => {
        render(<DeprecatedFieldsNotice usage={usage} onShow={vi.fn()} onClear={vi.fn()} />)

        expect(screen.getByText(/42 items/)).toBeInTheDocument()
        expect(screen.getByText(/replaced by authors/)).toBeInTheDocument()
        expect(screen.getByText(/1 item(?!s)/)).toBeInTheDocument()
    })

    it("shows the items of a field on request", async () => {
        const onShow = vi.fn()
        render(<DeprecatedFieldsNotice usage={usage} onShow={onShow} onClear={vi.fn()} />)

        await userEvent.click(screen.getAllByRole("button", { name: "Show these items" })[0])

        expect(onShow).toHaveBeenCalledWith("old_authors")
    })

    it("offers to go back to all items while one field's items are shown", async () => {
        const onClear = vi.fn()
        render(<DeprecatedFieldsNotice usage={usage} showing="old_authors" onShow={vi.fn()} onClear={onClear} />)

        await userEvent.click(screen.getByRole("button", { name: "Show all items" }))

        expect(onClear).toHaveBeenCalledTimes(1)
        expect(screen.getAllByRole("button", { name: "Show these items" })).toHaveLength(1)
    })
})
