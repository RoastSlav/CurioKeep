export type Member = {
  userId: string;
  displayName: string;
  email: string;
  role: 'OWNER' | 'ADMIN' | 'EDITOR' | 'VIEWER';
};

const base = '/api/collections';

export async function listMembers(collectionId: string): Promise<Member[]> {
  const res = await fetch(`${base}/${collectionId}/members`, { credentials: 'include' });
  if (!res.ok) throw new Error('Failed to load members');
  return res.json();
}

export async function updateMemberRole(collectionId: string, userId: string, role: string) {
  const res = await fetch(`${base}/${collectionId}/members/${userId}`, {
    method: 'PUT',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ role }),
  });
  if (!res.ok) throw new Error('Failed to update member role');
  return res.json();
}

export async function removeMember(collectionId: string, userId: string) {
  const res = await fetch(`${base}/${collectionId}/members/${userId}`, {
    method: 'DELETE',
    credentials: 'include',
  });
  if (!res.ok) throw new Error('Failed to remove member');
  return;
}
