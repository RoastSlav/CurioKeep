import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { apiFetch, subscribeAuthEvents } from "./client"
import { ApiError } from "./errors"

function jsonResponse(body: unknown, init: ResponseInit = {}): Response {
    return new Response(JSON.stringify(body), {
        headers: { "Content-Type": "application/json" },
        ...init,
    })
}

describe("apiFetch", () => {
    const fetchMock = vi.fn<typeof fetch>()

    beforeEach(() => {
        fetchMock.mockReset()
        vi.stubGlobal("fetch", fetchMock)
    })

    afterEach(() => {
        vi.unstubAllGlobals()
        document.cookie = "XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT"
    })

    it("prefixes the path with /api and returns the parsed JSON body", async () => {
        fetchMock.mockResolvedValueOnce(jsonResponse({ id: "1" }))

        const result = await apiFetch<{ id: string }>("/collections")

        expect(result).toEqual({ id: "1" })
        expect(fetchMock).toHaveBeenCalledWith("/api/collections", expect.objectContaining({ method: "GET", credentials: "include" }))
    })

    it("returns null for an empty response body", async () => {
        fetchMock.mockResolvedValueOnce(new Response(null, { status: 204 }))

        await expect(apiFetch("/items/empty")).resolves.toBeNull()
    })

    it("throws an ApiError carrying the status and the message from the error body", async () => {
        fetchMock.mockResolvedValueOnce(jsonResponse({ code: "BAD_REQUEST", message: "Title is required" }, { status: 400 }))

        const error = await apiFetch("/items/bad").catch((e: unknown) => e)

        expect(error).toBeInstanceOf(ApiError)
        expect(error).toMatchObject({ status: 400, message: "Title is required" })
    })

    it("falls back to the status text when the error body has no message", async () => {
        fetchMock.mockResolvedValueOnce(new Response("nope", { status: 502, statusText: "Bad Gateway" }))

        await expect(apiFetch("/items/upstream")).rejects.toMatchObject({ status: 502, message: "Bad Gateway" })
    })

    it("notifies auth listeners on 401 and 403", async () => {
        const listener = vi.fn()
        const unsubscribe = subscribeAuthEvents(listener)
        fetchMock.mockResolvedValueOnce(jsonResponse({ message: "Unauthorized" }, { status: 401 }))

        await apiFetch("/auth/me").catch(() => undefined)
        unsubscribe()

        expect(listener).toHaveBeenCalledWith("AUTH_REQUIRED")
    })

    it("sends JSON bodies with the CSRF token from the cookie on writes", async () => {
        document.cookie = "XSRF-TOKEN=token%20123"
        fetchMock.mockResolvedValueOnce(jsonResponse({ ok: true }))

        await apiFetch("/collections", { method: "POST", body: { name: "Books" } })

        const [, init] = fetchMock.mock.calls[0]
        expect(init?.body).toBe(JSON.stringify({ name: "Books" }))
        expect(init?.headers).toMatchObject({ "Content-Type": "application/json", "X-XSRF-TOKEN": "token 123" })
    })

    it("does not send a CSRF header on reads", async () => {
        document.cookie = "XSRF-TOKEN=abc"
        fetchMock.mockResolvedValueOnce(jsonResponse([]))

        await apiFetch("/collections/list")

        const [, init] = fetchMock.mock.calls[0]
        expect(init?.headers).not.toHaveProperty("X-XSRF-TOKEN")
    })

    it("shares one request between concurrent identical GETs", async () => {
        fetchMock.mockImplementation(() => Promise.resolve(jsonResponse({ n: 1 })))

        const [first, second] = await Promise.all([apiFetch("/modules"), apiFetch("/modules")])

        expect(first).toEqual(second)
        expect(fetchMock).toHaveBeenCalledTimes(1)
    })

    it("does not dedupe writes", async () => {
        fetchMock.mockImplementation(() => Promise.resolve(jsonResponse({ ok: true })))

        await Promise.all([
            apiFetch("/items", { method: "POST", body: { a: 1 } }),
            apiFetch("/items", { method: "POST", body: { a: 1 } }),
        ])

        expect(fetchMock).toHaveBeenCalledTimes(2)
    })
})
