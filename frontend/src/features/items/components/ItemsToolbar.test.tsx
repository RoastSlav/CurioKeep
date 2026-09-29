import { render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { describe, expect, it, vi } from "vitest"
import { sortOptions } from "../itemSort"
import { ItemsToolbar } from "./ItemsToolbar"

function renderToolbar(props: Partial<Parameters<typeof ItemsToolbar>[0]> = {}) {
    const handlers = { onSearchChange: vi.fn(), onSortChange: vi.fn(), onOpenFilters: vi.fn() }
    render(
        <ItemsToolbar
            search=""
            sort={null}
            sortOptions={sortOptions(undefined)}
            activeFilterCount={0}
            {...handlers}
            {...props}
        />,
    )
    return handlers
}

describe("ItemsToolbar", () => {
    it("reports what is typed in the search box", async () => {
        const { onSearchChange } = renderToolbar()

        await userEvent.type(screen.getByRole("searchbox", { name: "Search items" }), "du")

        expect(onSearchChange).toHaveBeenLastCalledWith("u")
        expect(onSearchChange).toHaveBeenCalledTimes(2)
    })

    it("shows the text it is given", () => {
        renderToolbar({ search: "dune" })

        expect(screen.getByRole("searchbox", { name: "Search items" })).toHaveValue("dune")
    })

    it("starts from newest first and flips the direction of the current field", async () => {
        const { onSortChange } = renderToolbar()

        await userEvent.click(screen.getByRole("button", { name: /switch to ascending/ }))

        expect(onSortChange).toHaveBeenCalledWith({ field: "createdAt", direction: "asc" })
    })

    it("flips an explicit sort in the other direction", async () => {
        const { onSortChange } = renderToolbar({ sort: { field: "title", direction: "asc" } })

        await userEvent.click(screen.getByRole("button", { name: /switch to descending/ }))

        expect(onSortChange).toHaveBeenCalledWith({ field: "title", direction: "desc" })
    })

    it("offers filters and shows how many are in effect", async () => {
        const { onOpenFilters } = renderToolbar({ activeFilterCount: 2 })

        expect(screen.getByLabelText("2 active")).toBeInTheDocument()
        await userEvent.click(screen.getByRole("button", { name: /Filters/ }))

        expect(onOpenFilters).toHaveBeenCalledTimes(1)
    })

    it("hides the filters button when the module has nothing to filter on", () => {
        renderToolbar({ onOpenFilters: undefined })

        expect(screen.queryByRole("button", { name: /Filters/ })).not.toBeInTheDocument()
    })
})
