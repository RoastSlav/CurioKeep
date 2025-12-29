import { apiFetch } from "../client";
import { clearCached, getCached, setCached, DEFAULT_CACHE_TTL } from "../cache";

export type AdminUser = {
  id: string;
  email: string;
  displayName: string | null;
  admin: boolean;
  status: string;
  authProvider: string;
  providerSubject: string | null;
  lastLoginAt: string | null;
  createdAt: string;
  updatedAt: string;
};

export type UpdateUserStatusRequest = {
  status: string;
};

export type UpdateUserAdminRequest = {
  admin: boolean;
};

const USERS_CACHE_KEY = "admin:users";

export async function listUsers({ forceRefresh = false } = {}) {
  if (!forceRefresh) {
    const cached = getCached<AdminUser[]>(USERS_CACHE_KEY);
    if (cached) return cached;
  }

  const users = await apiFetch<AdminUser[]>("/admin/users");
  setCached(USERS_CACHE_KEY, users, DEFAULT_CACHE_TTL, true);
  return users;
}

export async function updateUserStatus(
  id: string,
  payload: UpdateUserStatusRequest
) {
  const res = await apiFetch<{ ok: boolean }>(`/admin/users/${id}/status`, {
    method: "POST",
    body: payload,
  });
  clearCached(USERS_CACHE_KEY);
  return res;
}

export async function updateUserAdmin(
  id: string,
  payload: UpdateUserAdminRequest
) {
  const res = await apiFetch<{ ok: boolean }>(`/admin/users/${id}/admin`, {
    method: "POST",
    body: payload,
  });
  clearCached(USERS_CACHE_KEY);
  return res;
}

export async function deleteUser(id: string) {
  const res = await apiFetch<{ ok: boolean }>(`/admin/users/${id}`, {
    method: "DELETE",
  });
  clearCached(USERS_CACHE_KEY);
  return res;
}

export function clearUsersCache() {
  clearCached(USERS_CACHE_KEY);
}
