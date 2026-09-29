import { describe, expect, it } from "vitest"
import { toInputValue, toStringList } from "./formValue"

describe("toInputValue", () => {
    it("passes strings and numbers through", () => {
        expect(toInputValue("Dune")).toBe("Dune")
        expect(toInputValue(412)).toBe(412)
    })

    it("stringifies booleans and blanks everything else", () => {
        expect(toInputValue(true)).toBe("true")
        expect(toInputValue(undefined)).toBe("")
        expect(toInputValue(null)).toBe("")
        expect(toInputValue({ a: 1 })).toBe("")
    })
})

describe("toStringList", () => {
    it("keeps only string entries of an array", () => {
        expect(toStringList(["a", 1, "b", null])).toEqual(["a", "b"])
    })

    it("returns an empty list for non-arrays", () => {
        expect(toStringList("a")).toEqual([])
        expect(toStringList(undefined)).toEqual([])
    })
})
