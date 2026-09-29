import { render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { describe, expect, it, vi } from "vitest"
import type { ProviderLookupResponse } from "@/api/types"
import type { ModuleContract } from "@/features/modules/moduleTypes"
import { field } from "@/test/fixtures"
import ApplyMetadataStep from "./ApplyMetadataStep"

const moduleDefinition: ModuleContract = {
    key: "books",
    version: "1.0.0",
    name: "Books",
    states: [],
    providers: [],
    fields: [field({ key: "title", label: "Title" }), field({ key: "publisher", label: "Publisher" })],
    workflows: [],
    extensions: {},
}

const lookup: ProviderLookupResponse = {
    results: [],
    mergedAttributes: { title: "Dune", publisher: "Ace" },
}

describe("ApplyMetadataStep", () => {
    it("asks for a lookup first when there is no result yet", async () => {
        const onSkip = vi.fn()
        render(<ApplyMetadataStep moduleDefinition={moduleDefinition} lookup={null} attributes={{}} onApply={vi.fn()} onSkip={onSkip} />)

        expect(screen.getByText("Run metadata lookup first.")).toBeInTheDocument()
        await userEvent.click(screen.getByRole("button", { name: "Skip" }))
        expect(onSkip).toHaveBeenCalledTimes(1)
    })

    it("keeps the user's selection when the lookup result briefly disappears", async () => {
        const props = { moduleDefinition, attributes: {}, onApply: vi.fn(), onSkip: vi.fn() }
        const { rerender } = render(<ApplyMetadataStep {...props} lookup={lookup} />)
        await userEvent.click(screen.getAllByRole("button", { name: "Apply" })[0])
        expect(screen.getByRole("button", { name: "Selected" })).toBeInTheDocument()

        rerender(<ApplyMetadataStep {...props} lookup={null} />)
        expect(screen.getByText("Run metadata lookup first.")).toBeInTheDocument()
        rerender(<ApplyMetadataStep {...props} lookup={lookup} />)

        // Hooks placed after an early return are dropped while the step shows "run lookup first".
        expect(screen.getByRole("button", { name: "Selected" })).toBeInTheDocument()
    })

    it("lists only values that differ from the current attributes", () => {
        render(
            <ApplyMetadataStep
                moduleDefinition={moduleDefinition}
                lookup={lookup}
                attributes={{ title: "Dune" }}
                onApply={vi.fn()}
                onSkip={vi.fn()}
            />
        )

        expect(screen.queryByText("Suggested: Dune")).not.toBeInTheDocument()
        expect(screen.getByText("Suggested: Ace")).toBeInTheDocument()
        expect(screen.getByText("Publisher")).toBeInTheDocument()
    })

    it("tells the user when there is nothing new to apply", () => {
        render(
            <ApplyMetadataStep
                moduleDefinition={moduleDefinition}
                lookup={lookup}
                attributes={{ title: "Dune", publisher: "Ace" }}
                onApply={vi.fn()}
                onSkip={vi.fn()}
            />
        )

        expect(screen.getByText("No new data to apply.")).toBeInTheDocument()
        expect(screen.getByRole("button", { name: "Apply all" })).toBeDisabled()
    })

    it("applies every suggestion over the current attributes", async () => {
        const onApply = vi.fn()
        render(
            <ApplyMetadataStep
                moduleDefinition={moduleDefinition}
                lookup={lookup}
                attributes={{ title: "Old", notes: "mine" }}
                onApply={onApply}
                onSkip={vi.fn()}
            />
        )

        await userEvent.click(screen.getByRole("button", { name: "Apply all" }))

        expect(onApply).toHaveBeenCalledWith({ title: "Dune", publisher: "Ace", notes: "mine" })
    })

    it("applies only the selected suggestions and keeps the rest", async () => {
        const onApply = vi.fn()
        render(
            <ApplyMetadataStep
                moduleDefinition={moduleDefinition}
                lookup={lookup}
                attributes={{ title: "Old" }}
                onApply={onApply}
                onSkip={vi.fn()}
            />
        )
        expect(screen.getByRole("button", { name: "Next" })).toBeDisabled()

        await userEvent.click(screen.getAllByRole("button", { name: "Apply" })[1])
        await userEvent.click(screen.getByRole("button", { name: "Next" }))

        expect(onApply).toHaveBeenCalledWith({ title: "Old", publisher: "Ace" })
    })
})
