import { useMemo, useState } from "react";
import { Alert, Button, List, ListItem, ListItemText, Stack, Typography } from "@mui/material";
import type { ItemIdentifier, ModuleDefinition, ProviderLookupResponse } from "../../../../../api/types";
import { lookupProviders } from "../../../../providers/api";
import BarcodeScanner from "../BarcodeScanner";

function collectIdentifiers(moduleDefinition: ModuleDefinition | null | undefined, attributes: Record<string, any>): ItemIdentifier[] {
    if (!moduleDefinition) return [];
    const list: ItemIdentifier[] = [];
    const fields = moduleDefinition.fields ?? [];
    fields.forEach((field) => {
        if (!field.identifiers || !field.identifiers.length) return;
        const val = attributes[field.key];
        if (!val) return;
        field.identifiers.forEach((type) => {
            list.push({ type: type as ItemIdentifier["type"], value: String(val) });
        });
    });
    return list;
}

export default function LookupMetadataStep({
    moduleDefinition,
    moduleId,
    attributes,
    providers,
    onComplete,
    onBack,
    onAttributesChange,
}: {
    moduleDefinition: ModuleDefinition | null | undefined;
    moduleId: string;
    attributes: Record<string, any>;
    providers?: string[];
    onComplete: (response: ProviderLookupResponse) => void;
    onBack?: () => void;
    onAttributesChange: (next: Record<string, any>) => void;
}) {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [result, setResult] = useState<ProviderLookupResponse | null>(null);
    const identifiers = useMemo(() => collectIdentifiers(moduleDefinition, attributes), [moduleDefinition, attributes]);

    const runLookup = async () => {
        if (!identifiers.length) {
            setError("No identifiers provided yet (e.g., ISBN). Add one to lookup.");
            return;
        }
        setLoading(true);
        setError(null);
        try {
            const response = await lookupProviders({ moduleId, identifiers, providers });
            setResult(response);
            onComplete(response);
        } catch (err: any) {
            setError(err?.message || "Lookup failed");
        } finally {
            setLoading(false);
        }
    };

    const handleBarcode = (code: string) => {
        if (!moduleDefinition) return;
        const trimmed = code.trim();
        const idFields = (moduleDefinition.fields || []).filter((f) => f.identifiers?.length);
        let target = idFields.find((f) => f.identifiers?.includes(trimmed.length === 13 ? "ISBN13" : "ISBN10"));
        if (!target) target = idFields[0];
        if (!target) return;
        onAttributesChange({ ...attributes, [target.key]: trimmed });
    };

    return (
        <Stack spacing={2}>
            <Stack direction="row" justifyContent="space-between" alignItems="center">
                <Typography variant="subtitle1" fontWeight={700}>
                    Lookup metadata
                </Typography>
                {onBack && (
                    <Button onClick={onBack} disabled={loading}>
                        Back
                    </Button>
                )}
            </Stack>

            <BarcodeScanner onDetected={handleBarcode} />

            <Button variant="contained" onClick={runLookup} disabled={loading}>
                {loading ? "Looking up..." : "Run lookup"}
            </Button>

            {error && <Alert severity="error">{error}</Alert>}

            {result && (
                <Stack spacing={1}>
                    <Alert severity="success">Metadata found. You can apply it next.</Alert>
                    {result.providerResults?.length ? (
                        <List dense>
                            {result.providerResults.map((r) => (
                                <ListItem key={r.providerKey}>
                                    <ListItemText
                                        primary={r.providerKey}
                                        secondary={r.error ? `Error: ${r.error}` : "OK"}
                                        primaryTypographyProps={{ fontWeight: 700 }}
                                    />
                                </ListItem>
                            ))}
                        </List>
                    ) : null}
                </Stack>
            )}
        </Stack>
    );
}
