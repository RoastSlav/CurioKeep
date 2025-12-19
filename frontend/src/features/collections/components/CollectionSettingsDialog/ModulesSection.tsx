"use client"

import {useMemo, useState} from "react"
import {Loader2, RefreshCw} from "lucide-react"
import type {CollectionModule, ModuleSummary} from "../../../../api/types"
import DisableModuleConfirmDialog from "./DisableModuleConfirmDialog"
import {Button} from "../../../../../components/ui/button"
import {Switch} from "../../../../../components/ui/switch"
import {Badge} from "../../../../../components/ui/badge"
import {Alert, AlertDescription} from "../../../../../components/ui/alert"

type Props = {
    availableModules: ModuleSummary[]
    enabledModules: CollectionModule[]
    loading?: boolean
    saving?: boolean
    error?: string | null
    onEnable: (moduleKey: string) => Promise<void>
    onDisable: (moduleKey: string) => Promise<void>
    onRefresh?: () => void
}

export default function ModulesSection({
                                           availableModules,
                                           enabledModules,
                                           loading,
                                           saving,
                                           error,
                                           onEnable,
                                           onDisable,
                                           onRefresh,
}: Props) {
    const enabledKeys = useMemo(() => new Set(enabledModules.map((m) => m.moduleKey)), [enabledModules])
    const sortedModules = useMemo(
        () => [...availableModules].sort((a, b) => (a.name || a.moduleKey).localeCompare(b.name || b.moduleKey)),
        [availableModules],
    )

    const [pendingDisable, setPendingDisable] = useState<string | null>(null)

    const handleToggle = async (moduleKey: string, next: boolean) => {
        if (next) {
            await onEnable(moduleKey)
        } else {
            setPendingDisable(moduleKey)
        }
    }

    const confirmDisable = async () => {
        if (!pendingDisable) return
        const key = pendingDisable
        setPendingDisable(null)
        await onDisable(key)
    }

    return (
        <div className="space-y-4">
            <div className="flex items-center justify-between">
                <h3 className="text-lg font-bold uppercase">Modules</h3>
                <div className="flex items-center gap-2">
                    {saving && <Loader2 className="w-4 h-4 animate-spin"/>}
                    {onRefresh && (
                        <Button size="sm" variant="outline" onClick={onRefresh} disabled={loading}>
                            <RefreshCw className="w-4 h-4 mr-1"/>
                            Refresh
                        </Button>
                    )}
                </div>
            </div>

            {error && (
                <Alert variant="destructive">
                    <AlertDescription>{error}</AlertDescription>
                </Alert>
            )}

            {loading ? (
                <div className="flex items-center gap-2 text-muted-foreground">
                    <Loader2 className="w-4 h-4 animate-spin"/>
                    <span>Loading modules...</span>
                </div>
            ) : sortedModules.length ? (
                <div className="space-y-3">
                    {sortedModules.map((mod) => {
                        const enabled = enabledKeys.has(mod.moduleKey)
                        return (
                            <div key={mod.moduleKey}
                                 className="flex items-center justify-between border-2 border-border p-3 bg-card">
                                <div className="space-y-1">
                                    <p className="font-bold">{mod.name || mod.moduleKey}</p>
                                    <div className="flex items-center gap-2">
                                        {mod.version &&
                                            <span className="text-xs text-muted-foreground">v{mod.version}</span>}
                                        {mod.source && (
                                            <Badge variant="outline" className="text-xs">
                                                {mod.source.toString()}
                                            </Badge>
                                        )}
                                    </div>
                                </div>
                                <div className="flex items-center gap-2">
                                    <span
                                        className="text-sm text-muted-foreground">{enabled ? "Enabled" : "Disabled"}</span>
                                    <Switch
                                        checked={enabled}
                                        onCheckedChange={(checked) => void handleToggle(mod.moduleKey, checked)}
                                        disabled={saving}
                                    />
                                </div>
                            </div>
                        )
                    })}
                </div>
            ) : (
                <Alert>
                    <AlertDescription>No modules available. Modules will appear once loaded on the
                        server.</AlertDescription>
                </Alert>
            )}

            <DisableModuleConfirmDialog
                open={Boolean(pendingDisable)}
                moduleName={pendingDisable || undefined}
                onCancel={() => setPendingDisable(null)}
                onConfirm={() => void confirmDisable()}
            />
        </div>
    )
}
