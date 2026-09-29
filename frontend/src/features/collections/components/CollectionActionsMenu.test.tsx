import { render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { describe, expect, it, vi } from "vitest"
import CollectionActionsMenu from "./CollectionActionsMenu"

function renderMenu(props: Partial<Parameters<typeof CollectionActionsMenu>[0]> = {}) {
    const handlers = { onAddItem: vi.fn(), onOpenSettings: vi.fn(), onExport: vi.fn(), onImport: vi.fn() }
    render(<CollectionActionsMenu role="OWNER" moduleName="Books" {...handlers} {...props} />)
    return handlers
}

describe("CollectionActionsMenu", () => {
    it("shows an owner everything", () => {
        renderMenu()

        expect(screen.getByRole("button", { name: /Add Item/ })).toBeInTheDocument()
        expect(screen.getByRole("button", { name: /Export/ })).toBeInTheDocument()
        expect(screen.getByRole("button", { name: /Import/ })).toBeInTheDocument()
        expect(screen.getByRole("button", { name: /Collection Settings/ })).toBeInTheDocument()
    })

    it("lets a viewer export but not add, import or manage", () => {
        renderMenu({ role: "VIEWER", onAddItem: undefined, onOpenSettings: undefined, onImport: undefined })

        expect(screen.getByRole("button", { name: /Export/ })).toBeInTheDocument()
        expect(screen.queryByRole("button", { name: /Add Item/ })).not.toBeInTheDocument()
        expect(screen.queryByRole("button", { name: /Import/ })).not.toBeInTheDocument()
        expect(screen.queryByRole("button", { name: /Collection Settings/ })).not.toBeInTheDocument()
    })

    it("lets an editor import but not manage the collection", () => {
        renderMenu({ role: "EDITOR" })

        expect(screen.getByRole("button", { name: /Import/ })).toBeInTheDocument()
        expect(screen.queryByRole("button", { name: /Collection Settings/ })).not.toBeInTheDocument()
    })

    it("renders nothing for a viewer when there is nothing to export", () => {
        const { container } = render(<CollectionActionsMenu role="VIEWER" />)

        expect(container).toBeEmptyDOMElement()
    })

    it("offers JSON for everything and CSV for the module on screen", async () => {
        const { onExport } = renderMenu()

        await userEvent.click(screen.getByRole("button", { name: /Export/ }))
        await userEvent.click(await screen.findByRole("menuitem", { name: "All items as JSON" }))
        await userEvent.click(screen.getByRole("button", { name: /Export/ }))
        await userEvent.click(await screen.findByRole("menuitem", { name: "Books as CSV" }))

        expect(onExport).toHaveBeenNthCalledWith(1, "json")
        expect(onExport).toHaveBeenNthCalledWith(2, "csv")
    })

    it("opens the import dialog on request", async () => {
        const { onImport } = renderMenu()

        await userEvent.click(screen.getByRole("button", { name: /Import/ }))

        expect(onImport).toHaveBeenCalledTimes(1)
    })
})
