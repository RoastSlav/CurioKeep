import { render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { describe, expect, it, vi } from "vitest"
import { ItemsPagination } from "./ItemsPagination"

function renderPagination(props: Partial<Parameters<typeof ItemsPagination>[0]> = {}) {
    const onPageChange = vi.fn()
    const onSizeChange = vi.fn()
    render(<ItemsPagination page={0} size={25} total={120} onPageChange={onPageChange} onSizeChange={onSizeChange} {...props} />)
    return { onPageChange, onSizeChange }
}

describe("ItemsPagination", () => {
    it("shows which items are on screen out of the total, not just the page", () => {
        renderPagination({ page: 1, size: 25, total: 120 })

        expect(screen.getByText("Showing 26–50 of 120")).toBeInTheDocument()
        expect(screen.getByText("Page 2 of 5")).toBeInTheDocument()
    })

    it("clips the range on the last page", () => {
        renderPagination({ page: 4, size: 25, total: 120 })

        expect(screen.getByText("Showing 101–120 of 120")).toBeInTheDocument()
    })

    it("moves to the neighbouring pages", async () => {
        const { onPageChange } = renderPagination({ page: 2 })

        await userEvent.click(screen.getByRole("button", { name: "Next page" }))
        await userEvent.click(screen.getByRole("button", { name: "Previous page" }))

        expect(onPageChange).toHaveBeenNthCalledWith(1, 3)
        expect(onPageChange).toHaveBeenNthCalledWith(2, 1)
    })

    it("cannot go before the first or past the last page", () => {
        renderPagination({ page: 0, size: 50, total: 120 })
        expect(screen.getByRole("button", { name: "Previous page" })).toBeDisabled()

        renderPagination({ page: 2, size: 50, total: 120 })
        expect(screen.getAllByRole("button", { name: "Next page" }).at(-1)).toBeDisabled()
    })

    it("cannot be used while a page is loading", () => {
        renderPagination({ page: 1, disabled: true })

        expect(screen.getByRole("button", { name: "Previous page" })).toBeDisabled()
        expect(screen.getByRole("button", { name: "Next page" })).toBeDisabled()
    })

    it("renders nothing when there are no items", () => {
        renderPagination({ total: 0 })

        expect(screen.queryByRole("navigation")).not.toBeInTheDocument()
    })
})
