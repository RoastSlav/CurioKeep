import { useEffect, useState, useCallback } from 'react';
import * as api from '../api/collectionModulesApi';

export function useCollectionModules(collectionId?: string) {
  const [modules, setModules] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    if (!collectionId) return;
    setLoading(true);
    try {
      const data = await api.listCollectionModules(collectionId);
      setModules(data);
      setError(null);
    } catch (e: any) {
      setError(e.message || 'Failed');
    } finally { setLoading(false); }
  }, [collectionId]);

  useEffect(() => { load(); }, [load]);

  const toggle = useCallback(async (moduleKey: string, enabled: boolean) => {
    if (!collectionId) return;
    const prev = modules;
    setModules(m => m.map(x => x.moduleKey === moduleKey ? { ...x, enabled } : x));
    try {
      await api.toggleCollectionModule(collectionId, moduleKey, enabled);
    } catch (e) {
      setModules(prev);
      throw e;
    }
  }, [collectionId, modules]);

  return { modules, loading, error, reload: load, toggle };
}
