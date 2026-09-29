import { describe, expect, it } from "vitest"
import { cn, omitKey } from "./utils"

describe("omitKey", () => {
    it("returns a copy without the key and leaves the original untouched", () => {
        const original = { a: 1, b: 2 }

        expect(omitKey(original, "a")).toEqual({ b: 2 })
        expect(original).toEqual({ a: 1, b: 2 })
    })

    it("ignores a key that is not present", () => {
        expect(omitKey({ a: 1 }, "z")).toEqual({ a: 1 })
    })
})

describe("cn", () => {
    it("lets the later Tailwind class win a conflict", () => {
        expect(cn("p-2", "p-4")).toBe("p-4")
    })
})
