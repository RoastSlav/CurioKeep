import { render, screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { MemoryRouter, useLocation, useNavigate } from "react-router-dom"
import { beforeEach, describe, expect, it, vi } from "vitest"
import { getSetupStatus } from "../api/setup"
import { AuthContext, type AuthContextValue } from "../auth/authState"
import AppGate from "./AppGate"
import { useSetupStatus } from "./setupStatusContext"

vi.mock("../api/setup", () => ({ getSetupStatus: vi.fn() }))

const refreshMe = vi.fn<AuthContextValue["refreshMe"]>()

const auth: AuthContextValue = {
    user: null,
    loading: false,
    error: null,
    login: vi.fn(),
    logout: vi.fn(),
    refreshMe,
}

function Probe() {
    const navigate = useNavigate()
    const location = useLocation()
    const { loading, setupRequired } = useSetupStatus()
    return (
        <>
            <p>{`path:${location.pathname}`}</p>
            <p>{loading ? "loading" : setupRequired ? "setup required" : "setup complete"}</p>
            <button onClick={() => navigate("/login")}>go to login</button>
        </>
    )
}

function renderGate() {
    return render(
        <MemoryRouter initialEntries={["/"]}>
            <AuthContext.Provider value={auth}>
                <AppGate>
                    <Probe />
                </AppGate>
            </AuthContext.Provider>
        </MemoryRouter>,
    )
}

describe("AppGate", () => {
    beforeEach(() => {
        vi.mocked(getSetupStatus).mockReset()
        refreshMe.mockReset()
    })

    it("checks the setup status once, not on every navigation", async () => {
        vi.mocked(getSetupStatus).mockResolvedValue({ setupRequired: false })
        renderGate()
        await screen.findByText("setup complete")

        await userEvent.click(screen.getByRole("button", { name: "go to login" }))

        expect(await screen.findByText("path:/login")).toBeInTheDocument()
        expect(screen.getByText("setup complete")).toBeInTheDocument()
        expect(getSetupStatus).toHaveBeenCalledTimes(1)
        expect(refreshMe).toHaveBeenCalledTimes(1)
    })

    it("leaves the redirect to the routes and skips the session lookup while setup is required", async () => {
        vi.mocked(getSetupStatus).mockResolvedValue({ setupRequired: true })
        renderGate()

        await waitFor(() => expect(screen.getByText("setup required")).toBeInTheDocument())
        expect(screen.getByText("path:/")).toBeInTheDocument()
        expect(refreshMe).not.toHaveBeenCalled()
    })
})
