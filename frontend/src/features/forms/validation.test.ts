import { describe, expect, it } from "vitest"
import { field } from "@/test/fixtures"
import { validateAttributes, validateField } from "./validation"

describe("validateField", () => {
    it("reports a required field when the value is empty", () => {
        const title = field({ key: "title", label: "Title", required: true })

        expect(validateField(title, "")).toBe("Title is required")
        expect(validateField(title, "   ")).toBe("Title is required")
        expect(validateField(title, undefined)).toBe("Title is required")
    })

    it("accepts an empty value for an optional field", () => {
        expect(validateField(field({ key: "subtitle" }), "")).toBeUndefined()
    })

    it("enforces text length and pattern constraints", () => {
        const code = field({ key: "code", label: "Code", constraints: { minLength: 3, maxLength: 5, pattern: "^[A-Z]+$" } })

        expect(validateField(code, "AB")).toBe("Code must be at least 3 characters")
        expect(validateField(code, "ABCDEF")).toBe("Code must be at most 5 characters")
        expect(validateField(code, "abcd")).toBe("Code does not match required pattern")
        expect(validateField(code, "ABCD")).toBeUndefined()
    })

    it("rejects non-numeric input and out-of-range numbers", () => {
        const pages = field({ key: "pages", label: "Pages", type: "NUMBER", constraints: { min: 1, max: 1000 } })

        expect(validateField(pages, "abc")).toBe("Pages must be a number")
        expect(validateField(pages, 0)).toBe("Pages must be ≥ 1")
        expect(validateField(pages, "1001")).toBe("Pages must be ≤ 1000")
        expect(validateField(pages, "412")).toBeUndefined()
    })

    it("rejects invalid dates and invalid JSON text", () => {
        expect(validateField(field({ key: "released", label: "Released", type: "DATE" }), "not-a-date")).toBe("Released must be a valid date")
        expect(validateField(field({ key: "extra", label: "Extra", type: "JSON" }), "{oops")).toBe("Extra must be valid JSON")
        expect(validateField(field({ key: "extra", label: "Extra", type: "JSON" }), "{\"a\":1}")).toBeUndefined()
    })

    it("treats an empty tag list as missing for a required tags field", () => {
        const tags = field({ key: "tags", label: "Tags", type: "TAGS", required: true })

        expect(validateField(tags, [])).toBe("Tags is required")
        expect(validateField(tags, ["scifi"])).toBeUndefined()
    })
})

describe("validateAttributes", () => {
    it("returns an error per failing field keyed by field key", () => {
        const fields = [
            field({ key: "title", label: "Title", required: true }),
            field({ key: "pages", label: "Pages", type: "NUMBER" }),
        ]

        expect(validateAttributes(fields, { pages: "x" })).toEqual({
            title: "Title is required",
            pages: "Pages must be a number",
        })
    })
})
