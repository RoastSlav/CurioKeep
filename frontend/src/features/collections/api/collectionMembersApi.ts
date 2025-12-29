import { apiFetch } from "../../../api/client";
import type { CollectionMember } from "../../../api/types";

export async function listCollectionMembers(collectionId: string) {
    return apiFetch<CollectionMember[]>(`/collections/${collectionId}/members`);
}

export async function updateCollectionMemberRole(collectionId: string, userId: string, role: CollectionMember["role"]) {
    return apiFetch<CollectionMember>(`/collections/${collectionId}/members/${userId}`, {
        method: "PUT",
        body: { role },
    });
}

export async function removeCollectionMember(collectionId: string, userId: string) {
    return apiFetch<{ ok: boolean }>(`/collections/${collectionId}/members/${userId}`, { method: "DELETE" });
}
