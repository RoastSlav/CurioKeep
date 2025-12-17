import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
    Alert,
    Box,
    Button,
    Card,
    CardContent,
    CircularProgress,
    Container,
    Divider,
    MenuItem,
    Stack,
    TextField,
    Typography,
} from "@mui/material";
import { createItem, getModule, lookupProviders } from "../api";
import type { ItemIdentifier, ModuleDetails, ProviderLookupResult } from "../types";

function byOrder(a: { order?: number }, b: { order?: number }) {
    const ao = a.order ?? Number.MAX_SAFE_INTEGER;
    const bo = b.order ?? Number.MAX_SAFE_INTEGER;
    return ao - bo;
}

function isEmptyValue(v: unknown) {
    if (v === null || v === undefined) return true;
    if (typeof v === "string") return v.trim().length === 0;
    return false;
}

export default function AddItemPage() {
    const { collectionId, moduleKey } = useParams();
    const navigate = useNavigate();

    const [module, setModule] = useState<ModuleDetails | null>(null);
    const [attributes, setAttributes] = useState<Record<string, any>>({});
    const [touched, setTouched] = useState<Record<string, boolean>>({});
    const [stateKey, setStateKey] = useState<string>("");
    const [title, setTitle] = useState<string>("");
    const [identifiers, setIdentifiers] = useState<ItemIdentifier[]>([{ idType: "CUSTOM", idValue: "" }]);

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | undefined>();
    const [saving, setSaving] = useState(false);
    const [lookupLoading, setLookupLoading] = useState(false);
    const [lookupResult, setLookupResult] = useState<ProviderLookupResult | null>(null);
    const [appliedKeys, setAppliedKeys] = useState<string[]>([]);

    useEffect(() => {
        let mounted = true;
        async function load() {
            if (!moduleKey) return;
            setLoading(true);
            setError(undefined);
            try {
                const mod = await getModule(moduleKey);
                if (!mounted) return;
                setModule(mod);
                const initialState = mod.states?.[0]?.key ?? "OWNED";
                setStateKey(initialState);
                const nextAttrs: Record<string, any> = {};
                (mod.fields ?? []).forEach((f) => {
                    nextAttrs[f.fieldKey] = "";
                });
                setAttributes(nextAttrs);
            } catch (e) {
                if (!mounted) return;
                setError((e as Error).message);
            } finally {
                if (mounted) setLoading(false);
            }
        }
        void load();
        return () => {
            mounted = false;
        };
    }, [moduleKey]);

    const sortedFields = useMemo(() => (module?.fields ?? []).slice().sort(byOrder), [module?.fields]);

    const setAttr = (key: string, value: any) => {
        setAttributes((prev) => ({ ...prev, [key]: value }));
        setTouched((prev) => ({ ...prev, [key]: true }));
    };

    const applyMergedAttributes = (merged?: Record<string, unknown>) => {
        if (!merged) return [] as string[];
        const applied: string[] = [];
        setAttributes((prev) => {
            const next = { ...prev };
            Object.entries(merged).forEach(([key, value]) => {
                const alreadyTouched = touched[key];
                const empty = isEmptyValue(prev[key]);
                if (!alreadyTouched && empty) {
                    next[key] = value as any;
                    applied.push(key);
                }
            });
            return next;
        });
        return applied;
    };

    const handleLookup = async () => {
        if (!module?.id) {
            setError("Module id missing; cannot run provider lookup.");
            return;
        }
        const ids = identifiers.filter((i) => i.idValue && i.idValue.trim().length > 0);
        if (ids.length === 0) {
            setError("Add at least one identifier to fetch metadata.");
            return;
        }
        setLookupLoading(true);
        setError(undefined);
        setAppliedKeys([]);
        try {
            const result = await lookupProviders({ moduleId: module.id, identifiers: ids });
            setLookupResult(result);
            const mergedApplied = applyMergedAttributes(result.mergedAttributes as Record<string, unknown> | undefined);
            if (mergedApplied.length > 0) {
                setAppliedKeys(mergedApplied);
            } else {
                const fallback = (result.best?.mappedAttributes as Record<string, unknown> | undefined) || (result.best?.normalized as Record<string, unknown> | undefined);
                const fallbackApplied = applyMergedAttributes(fallback);
                setAppliedKeys(fallbackApplied);
            }
        } catch (e) {
            setError((e as Error).message);
        } finally {
            setLookupLoading(false);
        }
    };

    const handleSubmit = async () => {
        if (!collectionId || !module?.id) {
            setError("Missing collection or module id.");
            return;
        }
        setSaving(true);
        setError(undefined);
        try {
            const payload = {
                moduleId: module.id,
                stateKey,
                title: title.trim() || undefined,
                attributes,
                identifiers: identifiers.filter((i) => i.idValue.trim().length > 0),
            };
            await createItem(collectionId, payload);
            navigate(`/collections/${collectionId}`);
        } catch (e) {
            setError((e as Error).message);
        } finally {
            setSaving(false);
        }
    };

    const updateIdentifier = (idx: number, key: keyof ItemIdentifier, value: string) => {
        setIdentifiers((prev) => prev.map((id, i) => (i === idx ? { ...id, [key]: value } : id)));
    };

    const addIdentifier = () => setIdentifiers((prev) => [...prev, { idType: "CUSTOM", idValue: "" }]);
    const removeIdentifier = (idx: number) => setIdentifiers((prev) => prev.filter((_, i) => i !== idx));

    if (loading) {
        return (
            <Container maxWidth="md" sx={{ py: 4, textAlign: "center" }}>
                <CircularProgress />
                <Typography variant="body2" sx={{ mt: 2 }}>
                    Loading module…
                </Typography>
            </Container>
        );
    }

    if (error) {
        return (
            <Container maxWidth="md" sx={{ py: 4 }}>
                <Alert severity="error" sx={{ mb: 2 }}>
                    {error}
                </Alert>
            </Container>
        );
    }

    if (!module) return null;

    return (
        <Container maxWidth="md" sx={{ py: 4 }}>
            <Stack spacing={2}>
                <Typography variant="h5">Add Item – {module.name}</Typography>
                {lookupResult?.best && (
                    <Alert severity="info">
                        Fetched metadata from {lookupResult.best.providerKey}
                        {appliedKeys.length > 0 ? ` · Applied ${appliedKeys.length} field${appliedKeys.length === 1 ? "" : "s"}` : ""}
                    </Alert>
                )}
                <Card>
                    <CardContent>
                        <Stack spacing={2}>
                            <TextField
                                label="Title"
                                value={title}
                                onChange={(e) => setTitle(e.target.value)}
                                fullWidth
                            />
                            <TextField
                                select
                                label="State"
                                value={stateKey}
                                onChange={(e) => setStateKey(e.target.value)}
                                fullWidth
                            >
                                {(module.states ?? [{ key: "OWNED", label: "Owned" }]).map((s) => (
                                    <MenuItem key={s.key} value={s.key}>
                                        {s.label || s.key}
                                    </MenuItem>
                                ))}
                            </TextField>
                            <Divider>Identifiers</Divider>
                            <Stack spacing={1}>
                                {identifiers.map((id, idx) => (
                                    <Box key={idx} display="flex" gap={1} alignItems="center">
                                        <TextField
                                            label="ID Type"
                                            value={id.idType}
                                            onChange={(e) => updateIdentifier(idx, "idType", e.target.value)}
                                            fullWidth
                                        />
                                        <TextField
                                            label="ID Value"
                                            value={id.idValue}
                                            onChange={(e) => updateIdentifier(idx, "idValue", e.target.value)}
                                            fullWidth
                                        />
                                        {identifiers.length > 1 && (
                                            <Button color="error" onClick={() => removeIdentifier(idx)}>
                                                Remove
                                            </Button>
                                        )}
                                    </Box>
                                ))}
                                <Button onClick={addIdentifier}>Add identifier</Button>
                            </Stack>

                            <Divider>Fields</Divider>
                            <Box display="grid" gridTemplateColumns="repeat(auto-fit, minmax(240px, 1fr))" gap={2}>
                                {sortedFields.map((field) => (
                                    <TextField
                                        key={field.fieldKey}
                                        label={field.label || field.fieldKey}
                                        value={attributes[field.fieldKey] ?? ""}
                                        onChange={(e) => setAttr(field.fieldKey, e.target.value)}
                                        required={field.required}
                                        fullWidth
                                    />
                                ))}
                            </Box>

                            <Stack direction="row" spacing={1}>
                                <Button
                                    variant="outlined"
                                    onClick={handleLookup}
                                    disabled={lookupLoading}
                                >
                                    {lookupLoading ? "Fetching…" : "Fetch from providers"}
                                </Button>
                                <Box flexGrow={1} />
                                <Button
                                    variant="contained"
                                    onClick={handleSubmit}
                                    disabled={saving}
                                >
                                    {saving ? "Saving…" : "Create item"}
                                </Button>
                            </Stack>
                        </Stack>
                    </CardContent>
                </Card>
            </Stack>
        </Container>
    );
}
