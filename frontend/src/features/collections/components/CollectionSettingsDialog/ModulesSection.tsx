import { Alert, Box, Button, Chip, CircularProgress, FormControlLabel, Stack, Switch, Typography } from "@mui/material";
import { useMemo, useState } from "react";
import type { CollectionModule, ModuleSummary } from "../../../../api/types";
import DisableModuleConfirmDialog from "./DisableModuleConfirmDialog";

type Props = {
    availableModules: ModuleSummary[];
    enabledModules: CollectionModule[];
    loading?: boolean;
    saving?: boolean;
    error?: string | null;
    onEnable: (moduleKey: string) => Promise<void>;
    onDisable: (moduleKey: string) => Promise<void>;
    onRefresh?: () => void;
};

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
    const enabledKeys = useMemo(() => new Set(enabledModules.map((m) => m.moduleKey)), [enabledModules]);
    const sortedModules = useMemo(
        () =>
            [...availableModules].sort((a, b) => (a.name || a.moduleKey).localeCompare(b.name || b.moduleKey)),
        [availableModules]
    );

    const [pendingDisable, setPendingDisable] = useState<string | null>(null);

    const handleToggle = async (moduleKey: string, next: boolean) => {
        if (next) {
            await onEnable(moduleKey);
        } else {
            setPendingDisable(moduleKey);
        }
    };

    const confirmDisable = async () => {
        if (!pendingDisable) return;
        const key = pendingDisable;
        setPendingDisable(null);
        await onDisable(key);
    };

    return (
        <Stack spacing={2}>
            <Stack direction="row" alignItems="center" justifyContent="space-between">
                <Typography variant="h6" fontWeight={700}>
                    Modules
                </Typography>
                <Stack direction="row" spacing={1} alignItems="center">
                    {saving && <CircularProgress size={18} thickness={5} />}
                    {onRefresh && (
                        <Button size="small" variant="outlined" onClick={onRefresh} disabled={loading}>
                            Refresh
                        </Button>
                    )}
                </Stack>
            </Stack>

            {error && <Alert severity="error">{error}</Alert>}

            {loading ? (
                <Stack direction="row" spacing={1} alignItems="center">
                    <CircularProgress size={20} thickness={4} />
                    <Typography color="text.secondary">Loading modules…</Typography>
                </Stack>
            ) : sortedModules.length ? (
                <Stack spacing={1.5}>
                    {sortedModules.map((mod) => {
                        const enabled = enabledKeys.has(mod.moduleKey);
                        return (
                            <Box
                                key={mod.moduleKey}
                                sx={{
                                    display: "flex",
                                    alignItems: "center",
                                    justifyContent: "space-between",
                                    border: "1px solid",
                                    borderColor: "divider",
                                    borderRadius: 1,
                                    px: 1.5,
                                    py: 1,
                                }}
                            >
                                <Stack spacing={0.3}>
                                    <Typography fontWeight={700}>{mod.name || mod.moduleKey}</Typography>
                                    <Stack direction="row" spacing={1} alignItems="center">
                                        {mod.version && (
                                            <Typography variant="caption" color="text.secondary">
                                                v{mod.version}
                                            </Typography>
                                        )}
                                        {mod.source && (
                                            <Chip size="small" label={mod.source.toString()} variant="outlined" />
                                        )}
                                    </Stack>
                                </Stack>
                                <FormControlLabel
                                    control={
                                        <Switch
                                            checked={enabled}
                                            onChange={(e) => void handleToggle(mod.moduleKey, e.target.checked)}
                                            disabled={saving}
                                        />
                                    }
                                    label={enabled ? "Enabled" : "Disabled"}
                                />
                            </Box>
                        );
                    })}
                </Stack>
            ) : (
                <Alert severity="info">No modules available. Modules will appear once loaded on the server.</Alert>
            )}

            <DisableModuleConfirmDialog
                open={Boolean(pendingDisable)}
                moduleName={pendingDisable || undefined}
                onCancel={() => setPendingDisable(null)}
                onConfirm={() => void confirmDisable()}
            />
        </Stack>
    );
}
