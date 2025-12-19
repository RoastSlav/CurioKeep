import { Box, Button, Chip, Divider, List, ListItemButton, Stack, TextField, Typography } from "@mui/material";
import { useMemo, useState } from "react";
import type { ModuleDetails, ModuleSummary } from "../api/modulesApi";

type Props = {
    modules: ModuleSummary[];
    details: Record<string, ModuleDetails>;
    selectedKey?: string;
    onSelect: (moduleKey: string) => void;
    onViewXml: (moduleKey: string) => void;
};

const sourceColors: Record<ModuleSummary["source"], "default" | "primary" | "success"> = {
    BUILTIN: "primary",
    IMPORTED: "success",
    USER: "default",
};

export default function ModuleList({ modules, details, selectedKey, onSelect, onViewXml }: Props) {
    const [search, setSearch] = useState("");

    const filtered = useMemo(() => {
        const term = search.trim().toLowerCase();
        if (!term) return modules;
        return modules.filter((module) => {
            const target = `${module.name} ${module.moduleKey} ${module.version}`.toLowerCase();
            if (target.includes(term)) return true;
            const tags = details[module.moduleKey]?.contract.meta?.tags ?? [];
            return tags.some((tag) => tag.toLowerCase().includes(term));
        });
    }, [modules, search, details]);

    const formatUpdated = (value?: string) => {
        if (!value) return null;
        const date = new Date(value);
        return date.toLocaleString(undefined, { dateStyle: "medium", timeStyle: "short" });
    };

    return (
        <Stack spacing={1} sx={{ width: "100%" }}>
            <TextField
                label="Search modules"
                size="small"
                value={search}
                onChange={(event) => setSearch(event.target.value)}
                placeholder="Filter by name, key, or tag"
            />
            <List component="nav" sx={{ p: 0 }}>
                {filtered.map((module) => {
                    const updatedLabel = formatUpdated(module.updatedAt);
                    const tags = details[module.moduleKey]?.contract.meta?.tags ?? [];
                    return (
                        <Box key={module.moduleKey}>
                            <ListItemButton
                                selected={module.moduleKey === selectedKey}
                                onClick={() => onSelect(module.moduleKey)}
                                sx={{ flexWrap: "wrap" }}
                            >
                                <Stack flexGrow={1} spacing={0.5} sx={{ minWidth: 0 }}>
                                    <Stack direction="row" alignItems="center" spacing={1} justifyContent="space-between">
                                        <Stack>
                                            <Typography variant="subtitle1" fontWeight={600} noWrap>
                                                {module.name}
                                            </Typography>
                                            <Typography variant="caption" color="text.secondary" noWrap>
                                                {module.moduleKey}
                                            </Typography>
                                        </Stack>
                                        <Chip label={module.source} color={sourceColors[module.source] || "default"} size="small" />
                                    </Stack>
                                    <Stack direction="row" alignItems="center" spacing={1} justifyContent="space-between">
                                        <Typography variant="body2" color="text.secondary">
                                            v{module.version}
                                        </Typography>
                                        {updatedLabel && (
                                            <Typography variant="body2" color="text.secondary">
                                                Updated {updatedLabel}
                                            </Typography>
                                        )}
                                    </Stack>
                                    {tags.length > 0 && (
                                        <Stack direction="row" spacing={0.5} flexWrap="wrap" pt={0.25}>
                                            {tags.slice(0, 3).map((tag) => (
                                                <Chip key={`${module.moduleKey}-${tag}`} label={tag} size="small" />
                                            ))}
                                            {tags.length > 3 && <Chip label={`+${tags.length - 3}`} size="small" />}
                                        </Stack>
                                    )}
                                </Stack>
                                <Button
                                    size="small"
                                    variant="outlined"
                                    onClick={(event) => {
                                        event.stopPropagation();
                                        onViewXml(module.moduleKey);
                                    }}
                                >
                                    View XML
                                </Button>
                            </ListItemButton>
                            <Divider component="li" />
                        </Box>
                    );
                })}
                {filtered.length === 0 && (
                    <Box p={2} textAlign="center">
                        <Typography color="text.secondary">No modules match that search.</Typography>
                    </Box>
                )}
            </List>
        </Stack>
    );
}
