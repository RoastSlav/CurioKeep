import { describe, expect, it } from "vitest"
import { MAX_PASSWORD_BYTES, MIN_PASSWORD_LENGTH, passwordProblem } from "./passwordPolicy"

describe("passwordProblem", () => {
    it("accepts a password at the minimum length", () => {
        expect(passwordProblem("a".repeat(MIN_PASSWORD_LENGTH))).toBeNull()
    })

    it("rejects a password that is too short", () => {
        expect(passwordProblem("a".repeat(MIN_PASSWORD_LENGTH - 1))).toMatch(/at least 10/)
        expect(passwordProblem("")).not.toBeNull()
    })

    it("counts characters, not UTF-16 units, for the minimum", () => {
        expect(passwordProblem("😀".repeat(MIN_PASSWORD_LENGTH))).toBeNull()
        expect(passwordProblem("😀".repeat(MIN_PASSWORD_LENGTH - 1))).not.toBeNull()
    })

    it("rejects a password over the byte limit even when it has few characters", () => {
        expect(passwordProblem("a".repeat(MAX_PASSWORD_BYTES))).toBeNull()
        expect(passwordProblem("a".repeat(MAX_PASSWORD_BYTES + 1))).toMatch(/too long/)
        expect(passwordProblem("é".repeat(MAX_PASSWORD_BYTES / 2 + 1))).toMatch(/too long/)
    })
})
