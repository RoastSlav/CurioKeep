import { useEffect, useState, useCallback } from 'react';
import type { Member } from '../api/collectionMembersApi';
import * as api from '../api/collectionMembersApi';

export function useCollectionMembers(collectionId?: string) {
  const [members, setMembers] = useState<Member[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    if (!collectionId) return;
    setLoading(true);
    try {
      const data = await api.listMembers(collectionId);
      setMembers(data);
      setError(null);
    } catch (e: any) {
      setError(e.message || 'Failed');
    } finally {
      setLoading(false);
    }
  }, [collectionId]);

  useEffect(() => { load(); }, [load]);

  const changeRole = useCallback(async (userId: string, role: string) => {
    if (!collectionId) return;
    const prev = members;
    setMembers(m => m.map(x => x.userId === userId ? { ...x, role: role as any } : x));
    try {
      await api.updateMemberRole(collectionId, userId, role);
    } catch (e) {
      setMembers(prev);
      throw e;
    }
  }, [collectionId, members]);

  const remove = useCallback(async (userId: string) => {
    if (!collectionId) return;
    const prev = members;
    setMembers(m => m.filter(x => x.userId !== userId));
    try {
      await api.removeMember(collectionId, userId);
    } catch (e) {
      setMembers(prev);
      throw e;
    }
  }, [collectionId, members]);

  return { members, loading, error, reload: load, changeRole, remove };
}
