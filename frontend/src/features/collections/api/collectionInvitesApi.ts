import { apiFetch } from "../../../api/client";
import type { CollectionInvite, InviteValidateResponse, CollectionMember, CreateCollectionInviteRequest } from "../../../api/types";

export async function createCollectionInvite(collectionId: string, payload: CreateCollectionInviteRequest) {
    return apiFetch<CollectionInvite>(`/collections/${collectionId}/invites`, {
        method: "POST",
        body: payload,
    });
}

export async function listCollectionInvites(collectionId: string) {
    return apiFetch<CollectionInvite[]>(`/collections/${collectionId}/invites`);
}

export async function validateCollectionInvite(token: string) {
    return apiFetch<InviteValidateResponse>(`/collections/invites/${token}/validate`);
}

export async function acceptCollectionInvite(token: string) {
    return apiFetch<CollectionMember>("/collections/invites/accept", {
        method: "POST",
        body: { token },
    });
}

export async function revokeCollectionInvite(collectionId: string, token: string) {
    return apiFetch<void>(`/collections/${collectionId}/invites/${token}`, {
        method: "DELETE",
    });
}
