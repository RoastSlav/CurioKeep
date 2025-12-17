import { useEffect, useState } from "react";
import { Accordion, AccordionDetails, AccordionSummary, Chip, Divider, Stack, Typography } from "@mui/material";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import { getModule, listModules } from "../api";
import type { ModuleDetails, ModuleSummary } from "../types";

export default function ModulesPage() {
    const [modules, setModules] = useState<ModuleSummary[]>([]);
    const [details, setDetails] = useState<Record<string, ModuleDetails>>({});

    useEffect(() => {
        void listModules().then(setModules).catch(() => setModules([]));
    }, []);

    const loadDetails = async (moduleKey: string) => {
        if (details[moduleKey]) return;
        try {
            const d = await getModule(moduleKey);
            setDetails((prev) => ({ ...prev, [moduleKey]: d }));
        } catch (e) {
            console.error(e);
        }
    };

    return (
        <Stack spacing={2}>
            <Typography variant="h4">Modules</Typography>
            {modules.map((m) => (
                <Accordion key={m.moduleKey} onChange={(_, expanded) => expanded && void loadDetails(m.moduleKey)}>
                    <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                        <Stack direction="row" spacing={2} alignItems="center" sx={{ width: "100%" }}>
                            <Typography sx={{ flexGrow: 1 }}>{m.name || m.moduleKey}</Typography>
                            {m.version && <Chip size="small" label={`v${m.version}`} />}
                        </Stack>
                    </AccordionSummary>
                    <AccordionDetails>
                        <Typography color="text.secondary" sx={{ mb: 1 }}>
                            {details[m.moduleKey]?.description ?? m.description ?? "No description"}
                        </Typography>
                        <ModuleDetailView details={details[m.moduleKey]} />
                    </AccordionDetails>
                </Accordion>
            ))}
        </Stack>
    );
}

function ModuleDetailView({ details }: { details?: ModuleDetails }) {
    if (!details) return <Typography variant="body2">Loading details…</Typography>;

    return (
        <Stack spacing={2}>
            {details.providers && details.providers.length > 0 && (
                <Stack spacing={1}>
                    <Typography variant="subtitle2">Providers</Typography>
                    <Stack direction="row" spacing={1} flexWrap="wrap">
                        {details.providers.map((p) => (
                            <Chip key={p.key} label={p.label || p.key} color="secondary" variant="outlined" />
                        ))}
                    </Stack>
                </Stack>
            )}
            {details.states && details.states.length > 0 && (
                <Stack spacing={1}>
                    <Typography variant="subtitle2">States</Typography>
                    <Stack direction="row" spacing={1} flexWrap="wrap">
                        {details.states.map((s) => (
                            <Chip key={s.key} label={s.label || s.key} color={s.key === "OWNED" ? "primary" : "default"} />
                        ))}
                    </Stack>
                </Stack>
            )}
            {details.fields && details.fields.length > 0 && (
                <Stack spacing={1}>
                    <Typography variant="subtitle2">Fields</Typography>
                    <Divider />
                    <Stack spacing={1}>
                        {details.fields.map((f) => (
                            <Stack key={f.fieldKey} spacing={1}>
                                <Stack direction="row" justifyContent="space-between" alignItems="center">
                                    <div>
                                        <Typography>{f.label || f.fieldKey}</Typography>
                                        <Typography variant="caption" color="text.secondary">{f.fieldKey}</Typography>
                                    </div>
                                    <Typography color="text.secondary" variant="body2">
                                        {f.fieldType} {f.required ? "• required" : ""}
                                    </Typography>
                                </Stack>
                                <Stack direction="row" spacing={1} flexWrap="wrap">
                                    <Chip label={`order: ${f.order ?? "-"}`} size="small" />
                                    {f.searchable && <Chip label="searchable" size="small" />}
                                    {f.filterable && <Chip label="filterable" size="small" />}
                                    {f.sortable && <Chip label="sortable" size="small" />}
                                    {f.active === false && <Chip label="inactive" color="warning" size="small" />}
                                    {f.deprecated && <Chip label="deprecated" color="error" size="small" />}
                                    {Array.isArray(f.identifiers) && f.identifiers.length > 0 && (
                                        <Chip label={`${f.identifiers.length} id(s)`} size="small" />
                                    )}
                                    {Array.isArray(f.enumValues) && f.enumValues.length > 0 && (
                                        <Chip label={`${f.enumValues.length} enum`} size="small" />
                                    )}
                                    {Array.isArray(f.providerMappings) && f.providerMappings.length > 0 && (
                                        <Chip label={`${f.providerMappings.length} provider map(s)`} size="small" />
                                    )}
                                </Stack>
                            </Stack>
                        ))}
                    </Stack>
                </Stack>
            )}
        </Stack>
    );
}
