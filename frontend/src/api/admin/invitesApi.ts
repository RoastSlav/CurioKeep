import { apiFetch } from "../client";

export type AdminInvite = {
  email: string;
  invitedBy: {
    displayName: string | null;
    email: string;
  };
  createdAt: string;
  token: string;
};

export type CreateInviteRequest = {
  email: string;
};

export type CreateInviteResponse = {
  token: string;
};

export type InviteValidateResponse = {
  valid: boolean;
};

export type AcceptInviteRequest = {
  token: string;
  password: string;
  displayName: string;
};

export async function listInvites() {
  return apiFetch<AdminInvite[]>("/admin/invites");
}

export async function createInvite(payload: CreateInviteRequest) {
  return apiFetch<CreateInviteResponse>("/admin/invites", {
    method: "POST",
    body: payload,
  });
}

export async function revokeInvite(token: string) {
  return apiFetch<{ ok: boolean }>(`/admin/invites/${token}`, {
    method: "DELETE",
  });
}

export async function validateInvite(token: string) {
  return apiFetch<InviteValidateResponse>(`/invites/${token}/validate`);
}

export async function acceptInvite(payload: AcceptInviteRequest) {
  return apiFetch<{ ok: boolean }>("/invites/accept", {
    method: "POST",
    body: payload,
  });
}
