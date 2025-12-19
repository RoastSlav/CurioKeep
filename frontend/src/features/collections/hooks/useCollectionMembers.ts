import { useCallback, useEffect, useState } from "react";
import type { CollectionMember } from "../../../api/types";
import {
  listCollectionMembers,
  removeCollectionMember,
  updateCollectionMemberRole,
} from "../api/collectionMembersApi";

type Options = {
  auto?: boolean;
};

export function useCollectionMembers(
  collectionId?: string,
  { auto = true }: Options = {}
) {
  const [members, setMembers] = useState<CollectionMember[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    setMembers([]);
    setError(null);
    setLoaded(false);
  }, [collectionId]);

  const refresh = useCallback(async () => {
    if (!collectionId) return;
    setLoading(true);
    setError(null);
    try {
      const data = await listCollectionMembers(collectionId);
      setMembers(data);
      setLoaded(true);
    } catch (err: any) {
      setError(err?.message || "Failed to load members");
    } finally {
      setLoading(false);
    }
  }, [collectionId]);

  useEffect(() => {
    if (!auto) return;
    void refresh();
  }, [auto, refresh]);

  const changeRole = useCallback(
    async (userId: string, role: CollectionMember["role"]) => {
      if (!collectionId) return;
      setSaving(true);
      const snapshot = members;
      setMembers((prev) =>
        prev.map((m) => (m.userId === userId ? { ...m, role } : m))
      );
      try {
        const updated = await updateCollectionMemberRole(
          collectionId,
          userId,
          role
        );
        setMembers((prev) =>
          prev.map((m) => (m.userId === userId ? updated : m))
        );
        return updated;
      } catch (err) {
        setMembers(snapshot);
        throw err;
      } finally {
        setSaving(false);
      }
    },
    [collectionId, members]
  );

  const remove = useCallback(
    async (userId: string) => {
      if (!collectionId) return;
      setSaving(true);
      const snapshot = members;
      setMembers((prev) => prev.filter((m) => m.userId !== userId));
      try {
        await removeCollectionMember(collectionId, userId);
        return true;
      } catch (err) {
        setMembers(snapshot);
        throw err;
      } finally {
        setSaving(false);
      }
    },
    [collectionId, members]
  );

  return {
    members,
    loading,
    saving,
    error,
    loaded,
    refresh,
    changeRole,
    remove,
    setMembers,
  };
}
