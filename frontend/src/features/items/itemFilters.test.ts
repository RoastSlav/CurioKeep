import { describe, expect, it } from "vitest"
import { field } from "@/test/fixtures"
import { cleanFilters, countActiveFilters, filterableFields, filterKindFor, filterParams, type FieldFilters } from "./itemFilters"

describe("filterKindFor", () => {
    it("maps each field type to the control that suits it", () => {
        expect(filterKindFor(field({ key: "a", type: "ENUM" }))).toBe("in")
        expect(filterKindFor(field({ key: "a", type: "BOOLEAN" }))).toBe("in")
        expect(filterKindFor(field({ key: "a", type: "TAGS" }))).toBe("in")
        expect(filterKindFor(field({ key: "a", type: "TEXT" }))).toBe("contains")
        expect(filterKindFor(field({ key: "a", type: "LINK" }))).toBe("contains")
        expect(filterKindFor(field({ key: "a", type: "NUMBER" }))).toBe("range")
        expect(filterKindFor(field({ key: "a", type: "DATE" }))).toBe("dates")
        expect(filterKindFor(field({ key: "a", type: "JSON" }))).toBeNull()
    })
})

describe("filterableFields", () => {
    it("keeps active filterable fields with a supported type, in module order", () => {
        const fields = [
            field({ key: "b", filterable: true, order: 2 }),
            field({ key: "a", filterable: true, order: 1 }),
            field({ key: "hidden", filterable: false }),
            field({ key: "old", filterable: true, active: false }),
            field({ key: "blob", filterable: true, type: "JSON" }),
        ]

        expect(filterableFields(fields).map((f) => f.key)).toEqual(["a", "b"])
        expect(filterableFields(undefined)).toEqual([])
    })
})

describe("filterParams", () => {
    it("writes each filter as <fieldKey>.<operator>", () => {
        const filters: FieldFilters = {
            format: { kind: "in", values: ["HARDCOVER", "PAPERBACK"] },
            publisher: { kind: "contains", text: " ace " },
            pages: { kind: "range", min: 100, max: 500 },
            bought: { kind: "dates", from: "2020-01-01", to: "2020-12-31" },
        }

        expect(filterParams(filters)).toEqual({
            "format.in": "HARDCOVER,PAPERBACK",
            "publisher.contains": "ace",
            "pages.gte": "100",
            "pages.lte": "500",
            "bought.from": "2020-01-01",
            "bought.to": "2020-12-31",
        })
    })

    it("sends only the bounds that are set", () => {
        expect(filterParams({ pages: { kind: "range", min: 0 } })).toEqual({ "pages.gte": "0" })
        expect(filterParams({ bought: { kind: "dates", to: "2020" } })).toEqual({ "bought.to": "2020" })
    })

    it("leaves out filters that restrict nothing", () => {
        const filters: FieldFilters = {
            format: { kind: "in", values: [] },
            publisher: { kind: "contains", text: "   " },
            pages: { kind: "range" },
            bought: { kind: "dates" },
        }

        expect(filterParams(filters)).toEqual({})
        expect(cleanFilters(filters)).toEqual({})
        expect(countActiveFilters(filters)).toBe(0)
    })

    it("counts the filters that are in effect", () => {
        expect(countActiveFilters({
            format: { kind: "in", values: ["A"] },
            pages: { kind: "range", max: 5 },
            publisher: { kind: "contains", text: "" },
        })).toBe(2)
    })
})
