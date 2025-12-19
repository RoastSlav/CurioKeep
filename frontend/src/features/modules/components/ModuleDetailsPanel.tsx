import { Box, Button, Chip, Divider, Paper, Skeleton, Stack, Typography, useTheme } from "@mui/material";
import { useState, type ReactNode } from "react";
import ErrorState from "../../../components/ErrorState";
import type { ModuleDetails, ModuleSummary } from "../api/modulesApi";
import DeleteModuleDialog from "./DeleteModuleDialog";
import ModuleInfoCards from "./ModuleInfoCards";
import { useAuth } from "../../../auth/useAuth";

type Props = {
    module?: ModuleDetails;
    summary?: ModuleSummary;
    loading?: boolean;
    error?: string;
    onViewRaw?: () => void;
    onViewContract?: () => void;
    onModuleDeleted?: (moduleKey: string) => void;
};

const Section = ({ title, children }: { title: string; children: ReactNode }) => (
    <Box>
        <Typography variant="subtitle2" color="text.secondary" gutterBottom>
            {title}
        </Typography>
        <Stack spacing={1}>{children}</Stack>
    </Box>
);

export default function ModuleDetailsPanel({
    module,
    summary,
    loading,
    error,
    onViewRaw,
    onViewContract,
    onModuleDeleted,
}: Props) {
    const theme = useTheme();
    const { user } = useAuth();
    const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
    const canDelete = Boolean(user?.isAdmin && module?.source === "IMPORTED");

    const showSkeleton = !!loading && !module && !error;

    if (showSkeleton) {
        return (
            <Paper sx={{ p: 3, minHeight: 280 }}>
                <Stack spacing={2}>
                    <Skeleton variant="text" width="45%" height={32} />
                    <Skeleton variant="text" width="30%" height={20} />
                    <Skeleton variant="rectangular" height={160} sx={{ borderRadius: 2 }} />
                </Stack>
            </Paper>
        );
    }

    if (error) {
        return (
            <Paper sx={{ p: 3 }}>
                <ErrorState message={error} />
            </Paper>
        );
    }

    if (!module) {
        if (summary) {
            return (
                <Paper sx={{ p: 3, minHeight: 220 }}>
                    <Stack spacing={1}>
                        <Box sx={{ display: "flex", flexWrap: "wrap", justifyContent: "space-between", gap: 1 }}>
                            <Typography variant="h5" fontWeight={600} color="accent.main">
                                {summary.name}
                            </Typography>
                            <Chip label={summary.source} size="small" color="primary" />
                        </Box>
                        <Typography color="text.secondary">
                            Version {summary.version} · {summary.moduleKey}
                        </Typography>
                        <Typography color="secondary.main">
                            Module details are syncing. The detailed contract metadata will appear shortly.
                        </Typography>
                    </Stack>
                </Paper>
            );
        }

        return (
            <Paper sx={{ p: 3, minHeight: 220 }}>
                <Stack spacing={1} alignItems="center" justifyContent="center" sx={{ height: "100%" }}>
                    <Typography variant="h6" color="accent.main">
                        Select a module
                    </Typography>
                    <Typography color="secondary.main" textAlign="center">
                        Choose a module from the left column to view its contract and associated metadata.
                    </Typography>
                </Stack>
            </Paper>
        );
    }

    const description = module.contract.description || "No description provided.";

    const handleOpenDelete = () => setDeleteDialogOpen(true);
    const handleCloseDelete = () => setDeleteDialogOpen(false);

    return (
        <Paper sx={{ p: 3, minHeight: 320 }}>
            <Stack spacing={2}>
                <Stack
                    direction="row"
                    alignItems="flex-start"
                    justifyContent="space-between"
                    flexWrap="wrap"
                    spacing={1}
                >
                    <Box sx={{ minWidth: 0 }}>
                        <Typography
                            variant="h5"
                            fontWeight={600}
                            color="accent.main"
                            noWrap
                            sx={{ overflow: "hidden", textOverflow: "ellipsis" }}
                        >
                            {module.name}
                        </Typography>
                        <Typography
                            variant="body2"
                            color="text.secondary"
                            noWrap
                            sx={{ overflow: "hidden", textOverflow: "ellipsis" }}
                        >
                            {description}
                        </Typography>
                    </Box>
                    <Stack direction="row" spacing={1}>
                        <Button size="small" variant="outlined" onClick={onViewRaw} disabled={!onViewRaw}>
                            View Raw XML
                        </Button>
                        <Button size="small" variant="outlined" onClick={onViewContract} disabled={!onViewContract}>
                            View Contract JSON
                        </Button>
                        <Button
                            size="small"
                            variant="outlined"
                            color="error"
                            onClick={handleOpenDelete}
                            disabled={!canDelete}
                        >
                            Delete module
                        </Button>
                    </Stack>
                </Stack>
                <ModuleInfoCards module={module} />
                <Divider />
                <Section title="States">
                    {module.contract.states.map((state) => (
                        <Chip key={state.key} label={`${state.label} (${state.key})`} size="small" />
                    ))}
                </Section>
                <Section title="Providers">
                    {module.contract.providers.map((provider) => (
                        <Stack key={provider.key} spacing={0.25}>
                            <Stack direction="row" alignItems="center" spacing={1}>
                                <Typography variant="body2" fontWeight={600}>
                                    {provider.key}
                                </Typography>
                                <Chip
                                    label={provider.enabled ? "Enabled" : "Disabled"}
                                    size="small"
                                    color={provider.enabled ? "success" : "default"}
                                />
                                <Chip label={`Priority: ${provider.priority}`} size="small" />
                            </Stack>
                            {provider.supportsIdentifiers.length > 0 && (
                                <Typography variant="caption" color="text.secondary">
                                    Supports {provider.supportsIdentifiers.join(", ")}
                                </Typography>
                            )}
                        </Stack>
                    ))}
                </Section>
                <Divider />
                <Section title="Fields">
                    {module.contract.fields.map((field) => (
                        <Paper key={field.key} variant="outlined" sx={{ p: 1 }}>
                            <Stack spacing={0.25}>
                                <Stack direction="row" alignItems="center" spacing={1}>
                                    <Typography variant="body2" fontWeight={600} noWrap>
                                        {field.label}
                                    </Typography>
                                    <Chip label={field.type} size="small" />
                                    {field.required && (
                                        <Chip label="Required" size="small" color="warning" />
                                    )}
                                </Stack>
                                <Typography variant="caption" color="text.secondary" noWrap>
                                    {field.key}
                                </Typography>
                                {(() => {
                                    const status = [
                                        field.searchable ? "Searchable" : null,
                                        field.filterable ? "Filterable" : null,
                                        field.sortable ? "Sortable" : null,
                                    ]
                                        .filter(Boolean)
                                        .join(" • ");
                                    return status ? (
                                        <Typography variant="caption" color="text.secondary">
                                            {status}
                                        </Typography>
                                    ) : null;
                                })()}
                                {field.identifiers.length > 0 && (
                                    <Typography variant="caption" color="text.secondary">
                                        Identifier types: {field.identifiers.join(", ")}
                                    </Typography>
                                )}
                                {field.enumValues.length > 0 && (
                                    <Typography variant="caption" color="text.secondary">
                                        Enum: {field.enumValues.map((value) => value.key).join(", ")}
                                    </Typography>
                                )}
                            </Stack>
                        </Paper>
                    ))}
                </Section>
                <Divider />
                <Section title="Workflows">
                    {module.contract.workflows.map((workflow) => (
                        <Paper key={workflow.key} variant="outlined" sx={{ p: 1 }}>
                            <Stack spacing={0.5}>
                                <Typography variant="body2" fontWeight={600} color={theme.palette.text.primary}>
                                    {workflow.label}
                                </Typography>
                                <Typography variant="caption" color="text.secondary">
                                    {workflow.key}
                                </Typography>
                                <Stack spacing={0.5}>
                                    {workflow.steps.map((step, idx) => (
                                        <Paper key={`${workflow.key}-${idx}`} variant="outlined" sx={{ p: 1 }}>
                                            <Typography variant="caption" color="text.secondary">
                                                Step {idx + 1}: {step.type}
                                            </Typography>
                                            {step.label && (
                                                <Typography variant="body2">{step.label}</Typography>
                                            )}
                                            {step.field && (
                                                <Typography variant="caption" color="text.secondary">
                                                    Field: {step.field}
                                                </Typography>
                                            )}
                                            {step.fields.length > 0 && (
                                                <Typography variant="caption" color="text.secondary">
                                                    Fields: {step.fields.join(", ")}
                                                </Typography>
                                            )}
                                            {step.providers.length > 0 && (
                                                <Typography variant="caption" color="text.secondary">
                                                    Providers: {step.providers.join(", ")}
                                                </Typography>
                                            )}
                                        </Paper>
                                    ))}
                                </Stack>
                            </Stack>
                        </Paper>
                    ))}
                </Section>
            </Stack>
            <DeleteModuleDialog
                open={deleteDialogOpen}
                moduleKey={module.moduleKey}
                moduleName={module.name}
                onClose={handleCloseDelete}
                onModuleDeleted={() => {
                    handleCloseDelete();
                    onModuleDeleted?.(module.moduleKey);
                }}
            />
        </Paper>
    );
}
