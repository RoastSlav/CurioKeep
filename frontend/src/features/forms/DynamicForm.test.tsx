import { render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { describe, expect, it, vi } from "vitest"
import { field } from "@/test/fixtures"
import DynamicForm from "./DynamicForm"

const fields = [
    field({ key: "title", label: "Title", required: true }),
    field({ key: "pages", label: "Pages", type: "NUMBER" }),
]

describe("DynamicForm", () => {
    it("renders one labelled input per visible field and hides hidden ones", () => {
        render(
            <DynamicForm
                fields={[...fields, field({ key: "secret", label: "Secret", ui: { hidden: true } })]}
                onSubmit={vi.fn()}
            />
        )

        expect(screen.getByLabelText(/title/i)).toBeInTheDocument()
        expect(screen.getByLabelText(/pages/i)).toBeInTheDocument()
        expect(screen.queryByLabelText(/secret/i)).not.toBeInTheDocument()
    })

    it("marks a required field in its label", () => {
        render(<DynamicForm fields={fields} onSubmit={vi.fn()} />)

        expect(screen.getByText("Title").textContent).toContain("*")
        expect(screen.getByText("Pages").textContent).not.toContain("*")
    })

    it("shows the validation error and does not submit while a required field is empty", async () => {
        const onSubmit = vi.fn()
        render(<DynamicForm fields={fields} onSubmit={onSubmit} />)

        await userEvent.click(screen.getByRole("button", { name: "Save" }))

        expect(await screen.findByText("Title is required")).toBeInTheDocument()
        expect(onSubmit).not.toHaveBeenCalled()
    })

    it("submits the entered values once the form is valid", async () => {
        const onSubmit = vi.fn()
        render(<DynamicForm fields={fields} onSubmit={onSubmit} submitLabel="Create" />)

        await userEvent.type(screen.getByLabelText(/title/i), "Dune")
        await userEvent.click(screen.getByRole("button", { name: "Create" }))

        expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ title: "Dune" }))
    })

    it("clears a field's error as soon as the user edits it", async () => {
        render(<DynamicForm fields={fields} onSubmit={vi.fn()} />)

        await userEvent.click(screen.getByRole("button", { name: "Save" }))
        expect(await screen.findByText("Title is required")).toBeInTheDocument()

        await userEvent.type(screen.getByLabelText(/title/i), "D")

        expect(screen.queryByText("Title is required")).not.toBeInTheDocument()
    })

    it("offers a cancel button only when a handler is given", async () => {
        const onCancel = vi.fn()
        const { rerender } = render(<DynamicForm fields={fields} onSubmit={vi.fn()} />)
        expect(screen.queryByRole("button", { name: "Cancel" })).not.toBeInTheDocument()

        rerender(<DynamicForm fields={fields} onSubmit={vi.fn()} onCancel={onCancel} />)
        await userEvent.click(screen.getByRole("button", { name: "Cancel" }))

        expect(onCancel).toHaveBeenCalledTimes(1)
    })
})
