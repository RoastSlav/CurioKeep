type CreateInviteResponse = { token: string };

const base = '/api/collections';

export async function createCollectionInvite(collectionId: string, role: 'ADMIN'|'EDITOR'|'VIEWER') {
  const res = await fetch(`${base}/${collectionId}/invites`, {
    method: 'POST',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ role }),
  });
  if (!res.ok) throw new Error('Failed to create invite');
  return (await res.json()) as CreateInviteResponse;
}

export async function validateInvite(token: string) {
  const res = await fetch(`/api/collections/invites/${encodeURIComponent(token)}`, { credentials: 'include' });
  if (!res.ok) throw new Error('Failed to validate invite');
  return res.json();
}

export async function acceptInvite(token: string) {
  const res = await fetch('/api/collections/invites/accept', {
    method: 'POST',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ token }),
  });
  if (!res.ok) throw new Error('Failed to accept invite');
  return res.json();
}

export async function listInvites(collectionId: string) {
  const res = await fetch(`${base}/${collectionId}/invites`, { credentials: 'include' });
  if (!res.ok) throw new Error('Failed to list invites');
  return res.json();
}
