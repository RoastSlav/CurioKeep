import { describe, expect, it } from "vitest"
import { ApiError, getErrorMessage, isApiError } from "./errors"

describe("getErrorMessage", () => {
    it("uses the message of an Error", () => {
        expect(getErrorMessage(new Error("boom"), "fallback")).toBe("boom")
        expect(getErrorMessage(new ApiError(500, "server exploded"), "fallback")).toBe("server exploded")
    })

    it("falls back for empty messages and non-Error values", () => {
        expect(getErrorMessage(new Error(""), "fallback")).toBe("fallback")
        expect(getErrorMessage("just a string", "fallback")).toBe("fallback")
        expect(getErrorMessage(undefined, "fallback")).toBe("fallback")
    })
})

describe("isApiError", () => {
    it("recognises ApiError instances only", () => {
        expect(isApiError(new ApiError(404, "missing"))).toBe(true)
        expect(isApiError(new Error("missing"))).toBe(false)
        expect(isApiError({ status: 404 })).toBe(false)
    })
})
