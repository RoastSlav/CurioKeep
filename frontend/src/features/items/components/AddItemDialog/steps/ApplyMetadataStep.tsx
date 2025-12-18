import { Alert, Button, Stack, Typography } from "@mui/material";
import type { ModuleDefinition, ProviderLookupResponse } from "../../../../../api/types";

export default function ApplyMetadataStep({
    moduleDefinition,
    lookup,
    attributes,
    onApply,
    onSkip,
}: {
    moduleDefinition: ModuleDefinition | null | undefined;
    lookup: ProviderLookupResponse | null;
    attributes: Record<string, any>;
    onApply: (attrs: Record<string, any>) => void;
    onSkip: () => void;
}) {
    if (!lookup) {
        return (
            <Stack spacing={2}>
                <Alert severity="info">Run metadata lookup first.</Alert>
                <Button variant="outlined" onClick={onSkip}>
                    Skip
                </Button>
            </Stack>
        );
    }

    const suggestions = lookup.mergedAttributes || {};
    const fields = moduleDefinition?.fields ?? [];

    const diffEntries = Object.entries<Record<string, unknown>>(suggestions).filter(([key, val]) => {
        const current = attributes[key];
        return current !== val;
    });

    const handleApply = () => {
        const merged = { ...attributes, ...suggestions };
        onApply(merged);
    };

    return (
        <Stack spacing={2}>
            <Typography variant="subtitle1" fontWeight={700}>
                Apply provider data
            </Typography>
            {!diffEntries.length ? (
                <Alert severity="info">No new data to apply.</Alert>
            ) : (
                <Stack spacing={1}>
                    {diffEntries.map(([key, val]) => {
                        const field = fields.find((f) => f.key === key);
                        return (
                            <Stack key={key} spacing={0.25}>
                                <Typography variant="body2" fontWeight={700}>
                                    {field?.label || key}
                                </Typography>
                                <Typography variant="body2" color="text.secondary">
                                    Current: {String(attributes[key] ?? "-")}
                                </Typography>
                                <Typography variant="body2">Suggested: {String(val)}</Typography>
                            </Stack>
                        );
                    })}
                </Stack>
            )}

            <Stack direction="row" spacing={1}>
                <Button variant="outlined" onClick={onSkip}>
                    Skip
                </Button>
                <Button variant="contained" onClick={handleApply} disabled={!diffEntries.length}>
                    Apply all
                </Button>
            </Stack>
        </Stack>
    );
}
