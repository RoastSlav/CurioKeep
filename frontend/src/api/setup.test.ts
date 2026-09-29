import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { clearAllCached } from "./cache"
import { apiFetch } from "./client"
import { getSetupStatus } from "./setup"

vi.mock("./client", () => ({ apiFetch: vi.fn() }))

describe("getSetupStatus", () => {
    const apiFetchMock = vi.mocked(apiFetch)

    beforeEach(() => {
        apiFetchMock.mockReset()
        clearAllCached()
    })

    afterEach(() => {
        clearAllCached()
    })

    it("asks the server again while setup is still required", async () => {
        apiFetchMock.mockResolvedValue({ setupRequired: true })

        await getSetupStatus()
        await getSetupStatus()

        expect(apiFetchMock).toHaveBeenCalledTimes(2)
    })

    it("serves a completed setup from the cache", async () => {
        apiFetchMock.mockResolvedValue({ setupRequired: false })

        await getSetupStatus()
        const second = await getSetupStatus()

        expect(second).toEqual({ setupRequired: false })
        expect(apiFetchMock).toHaveBeenCalledTimes(1)
    })

    it("sees a completed setup right after a required one", async () => {
        apiFetchMock.mockResolvedValueOnce({ setupRequired: true })
        apiFetchMock.mockResolvedValueOnce({ setupRequired: false })

        await getSetupStatus()

        await expect(getSetupStatus()).resolves.toEqual({ setupRequired: false })
    })
})
