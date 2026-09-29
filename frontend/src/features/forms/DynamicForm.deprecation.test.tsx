import { render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { describe, expect, it, vi } from "vitest"
import { field } from "@/test/fixtures"
import DynamicForm from "./DynamicForm"

const fields = [
    field({ key: "title", label: "Title", required: true, order: 1 }),
    field({ key: "authors", label: "Authors", order: 2 }),
    field({ key: "old_authors", label: "Author text", deprecated: true, replacedBy: "authors", order: 3 }),
    field({ key: "pages", label: "Pages", type: "NUMBER", order: 4 }),
]

describe("DynamicForm and deprecated fields", () => {
    it("never offers a deprecated field for a new item", () => {
        render(<DynamicForm fields={fields} onSubmit={vi.fn()} />)

        expect(screen.queryByLabelText(/author text/i)).not.toBeInTheDocument()
        expect(screen.queryByText("Deprecated fields")).not.toBeInTheDocument()
    })

    it("does not show a deprecated field the item has no value in", () => {
        render(<DynamicForm fields={fields} initialValues={{ title: "Dune", old_authors: "  " }} onSubmit={vi.fn()} />)

        expect(screen.queryByLabelText(/author text/i)).not.toBeInTheDocument()
    })

    it("shows a deprecated field that still holds a value, marked as deprecated with its replacement", () => {
        render(<DynamicForm fields={fields} initialValues={{ title: "Dune", old_authors: "Frank Herbert" }} onSubmit={vi.fn()} />)

        expect(screen.getByRole("heading", { name: "Deprecated fields" })).toBeInTheDocument()
        expect(screen.getByLabelText(/author text/i)).toHaveValue("Frank Herbert")
        expect(screen.getByText("Deprecated")).toBeInTheDocument()
        expect(screen.getByText("Replaced by Authors")).toBeInTheDocument()
    })

    it("moves the value to the replacing field and empties the old one", async () => {
        const onSubmit = vi.fn()
        render(<DynamicForm fields={fields} initialValues={{ title: "Dune", old_authors: "Frank Herbert" }} onSubmit={onSubmit} />)

        await userEvent.click(screen.getByRole("button", { name: "Move to Authors" }))

        expect(screen.getByLabelText(/^authors/i)).toHaveValue("Frank Herbert")
        expect(screen.queryByLabelText(/author text/i)).toHaveValue("")
        await userEvent.click(screen.getByRole("button", { name: "Save" }))
        const saved = onSubmit.mock.calls[0][0]
        expect(saved.authors).toBe("Frank Herbert")
        expect(saved.old_authors).toBeUndefined()
    })

    it("will not overwrite a value the replacing field already has", async () => {
        render(<DynamicForm fields={fields} initialValues={{ title: "Dune", authors: "Someone", old_authors: "Frank Herbert" }} onSubmit={vi.fn()} />)

        expect(screen.getByRole("button", { name: "Move to Authors" })).toBeDisabled()
        expect(screen.getByText("Authors already has a value.")).toBeInTheDocument()
    })

    it("lets a deprecated field that was required be saved empty", async () => {
        const onSubmit = vi.fn()
        const required = [field({ key: "title", label: "Title", required: true }), field({ key: "old", label: "Old", deprecated: true, required: true })]
        render(<DynamicForm fields={required} initialValues={{ title: "Dune", old: "x" }} onSubmit={onSubmit} />)

        await userEvent.clear(screen.getByLabelText(/^old/i))
        await userEvent.click(screen.getByRole("button", { name: "Save" }))

        expect(onSubmit).toHaveBeenCalledTimes(1)
    })

    it("does not offer a field the module has made inactive", () => {
        render(<DynamicForm fields={[...fields, field({ key: "retired", label: "Retired", active: false })]} initialValues={{ retired: "x" }} onSubmit={vi.fn()} />)

        expect(screen.queryByLabelText(/retired/i)).not.toBeInTheDocument()
    })

    it("keeps values it does not show when the item is saved, but leaves the cover to its own endpoints", async () => {
        const onSubmit = vi.fn()
        render(
            <DynamicForm
                fields={[field({ key: "title", label: "Title", required: true }), field({ key: "secret", label: "Secret", ui: { hidden: true } })]}
                initialValues={{ title: "Dune", secret: "kept", removed_in_v2: { a: 1 }, providerImageUrl: "/api/assets/x.png" }}
                onSubmit={onSubmit}
            />,
        )

        await userEvent.click(screen.getByRole("button", { name: "Save" }))

        expect(onSubmit).toHaveBeenCalledWith({ title: "Dune", secret: "kept", removed_in_v2: { a: 1 } })
    })

    it("saves a number that arrived as text as a number", async () => {
        const onSubmit = vi.fn()
        render(<DynamicForm fields={fields} initialValues={{ title: "Dune", pages: "412" }} onSubmit={onSubmit} />)

        await userEvent.click(screen.getByRole("button", { name: "Save" }))

        expect(onSubmit.mock.calls[0][0].pages).toBe(412)
    })
})
