export type ModuleToggle = { moduleKey: string; enabled: boolean };
const base = '/api/collections';

export async function listCollectionModules(collectionId: string) {
  const res = await fetch(`${base}/${collectionId}/modules`, { credentials: 'include' });
  if (!res.ok) throw new Error('Failed to load modules');
  return res.json();
}

export async function toggleCollectionModule(collectionId: string, moduleKey: string, enabled: boolean) {
  const res = await fetch(`${base}/${collectionId}/modules/${encodeURIComponent(moduleKey)}`, {
    method: 'PUT',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ enabled }),
  });
  if (!res.ok) throw new Error('Failed to update module');
  return res.json();
}
