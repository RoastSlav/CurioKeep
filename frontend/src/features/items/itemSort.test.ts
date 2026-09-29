import { describe, expect, it } from "vitest"
import { field } from "@/test/fixtures"
import { defaultDirection, sortOptions } from "./itemSort"

describe("sortOptions", () => {
    it("offers the built-in orders first", () => {
        expect(sortOptions(undefined).map((option) => option.value)).toEqual(["createdAt", "updatedAt", "title"])
    })

    it("adds the active sortable fields of the module in module order", () => {
        const options = sortOptions([
            field({ key: "pages", label: "Pages", sortable: true, order: 2 }),
            field({ key: "year", label: "Year", sortable: true, order: 1 }),
            field({ key: "notes", label: "Notes", sortable: false }),
            field({ key: "old", label: "Old", sortable: true, active: false }),
        ])

        expect(options.map((option) => option.value)).toEqual(["createdAt", "updatedAt", "title", "year", "pages"])
        expect(options.at(-1)?.label).toBe("Pages")
    })

    it("does not repeat a built-in order that the module also declares", () => {
        const options = sortOptions([field({ key: "title", label: "Title", sortable: true })])

        expect(options.filter((option) => option.value === "title")).toHaveLength(1)
    })
})

describe("defaultDirection", () => {
    it("starts dates newest first and everything else ascending", () => {
        expect(defaultDirection("createdAt")).toBe("desc")
        expect(defaultDirection("updatedAt")).toBe("desc")
        expect(defaultDirection("title")).toBe("asc")
        expect(defaultDirection("pages")).toBe("asc")
    })
})
