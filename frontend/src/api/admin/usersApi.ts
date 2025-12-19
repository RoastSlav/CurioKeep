import { apiFetch } from "../client"

export type AdminUser = {
  id: string
  email: string
  displayName: string | null
  admin: boolean
  status: string
  authProvider: string
  providerSubject: string | null
  lastLoginAt: string | null
  createdAt: string
  updatedAt: string
}

export type UpdateUserStatusRequest = {
  status: string
}

export type UpdateUserAdminRequest = {
  admin: boolean
}

export async function listUsers() {
  return apiFetch<AdminUser[]>("/admin/users")
}

export async function updateUserStatus(id: string, payload: UpdateUserStatusRequest) {
  return apiFetch<{ ok: boolean }>(`/admin/users/${id}/status`, {
    method: "POST",
    body: payload,
  })
}

export async function updateUserAdmin(id: string, payload: UpdateUserAdminRequest) {
  return apiFetch<{ ok: boolean }>(`/admin/users/${id}/admin`, {
    method: "POST",
    body: payload,
  })
}

export async function deleteUser(id: string) {
  return apiFetch<{ ok: boolean }>(`/admin/users/${id}`, {
    method: "DELETE",
  })
}
