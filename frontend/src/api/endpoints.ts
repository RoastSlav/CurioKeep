import { apiFetch } from "./client";
import type { LoginRequest, SetupStatus, User } from "./types";

export function login(body: LoginRequest) {
    return apiFetch<void>("/auth/login", { method: "POST", body });
}

export function logout() {
    return apiFetch<void>("/auth/logout", { method: "POST" });
}

export function fetchCurrentUser() {
    return apiFetch<User>("/auth/me");
}

export function fetchSetupStatus() {
    return apiFetch<SetupStatus>("/setup/status");
}
