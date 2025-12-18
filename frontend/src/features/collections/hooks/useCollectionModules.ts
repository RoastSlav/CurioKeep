import { useCallback, useEffect, useMemo, useState } from "react";
import type { CollectionModule, ModuleSummary } from "../../../api/types";
import {
    disableCollectionModule,
    enableCollectionModule,
    listAvailableModules,
    listEnabledModules,
} from "../api/collectionModulesApi";

export function useCollectionModules(collectionId?: string) {
    const [availableModules, setAvailableModules] = useState<ModuleSummary[]>([]);
    const [enabledModules, setEnabledModules] = useState<CollectionModule[]>([]);
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const hasModules = useMemo(() => availableModules.length > 0, [availableModules]);

    const refresh = useCallback(async () => {
        if (!collectionId) return;
        setLoading(true);
        setError(null);
        try {
            const [available, enabled] = await Promise.all([
                listAvailableModules(),
                listEnabledModules(collectionId),
            ]);
            setAvailableModules(available);
            setEnabledModules(enabled);
        } catch (err: any) {
            setError(err?.message || "Failed to load modules");
        } finally {
            setLoading(false);
        }
    }, [collectionId]);

    useEffect(() => {
        void refresh();
    }, [refresh]);

    const enable = useCallback(
        async (moduleKey: string) => {
            if (!collectionId) return;
            setSaving(true);
            const snapshot = enabledModules;
            const existing = enabledModules.find((m) => m.moduleKey === moduleKey);
            if (!existing) {
                const found = availableModules.find((m) => m.moduleKey === moduleKey);
                setEnabledModules((prev) => [
                    ...prev,
                    {
                        moduleKey,
                        moduleId: found?.id || moduleKey,
                        name: found?.name,
                        version: found?.version,
                        source: found?.source,
                        enabledAt: new Date().toISOString(),
                    },
                ]);
            }
            try {
                await enableCollectionModule(collectionId, moduleKey);
                const next = await listEnabledModules(collectionId);
                setEnabledModules(next);
                return next;
            } catch (err) {
                setEnabledModules(snapshot);
                throw err;
            } finally {
                setSaving(false);
            }
        },
        [availableModules, collectionId, enabledModules]
    );

    const disable = useCallback(
        async (moduleKey: string) => {
            if (!collectionId) return;
            setSaving(true);
            const snapshot = enabledModules;
            setEnabledModules((prev) => prev.filter((m) => m.moduleKey !== moduleKey));
            try {
                await disableCollectionModule(collectionId, moduleKey);
                const next = await listEnabledModules(collectionId);
                setEnabledModules(next);
                return next;
            } catch (err) {
                setEnabledModules(snapshot);
                throw err;
            } finally {
                setSaving(false);
            }
        },
        [collectionId, enabledModules]
    );

    return {
        availableModules,
        enabledModules,
        hasModules,
        loading,
        saving,
        error,
        refresh,
        enable,
        disable,
        setEnabledModules,
    };
}
