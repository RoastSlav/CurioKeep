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
import AssetGallery from "../components/AssetGallery";
import AssetPreviewDialog from "../components/AssetPreviewDialog";
import { createItem, getModule, lookupProviders } from "../api";
import type { ItemIdentifier, ModuleDetails, ProviderAsset, ProviderLookupResult } from "../types";

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

function pickDefaultIdentifierType(mod?: ModuleDetails | null): string {
    if (!mod?.providers) return "CUSTOM";
    const supported = mod.providers.flatMap((p) => p.supportsIdentifiers || []);
    if (supported.length === 0) return "CUSTOM";
    const preferred = supported.find((t) => t !== "CUSTOM");
    return preferred || supported[0] || "CUSTOM";
}

function parseJsonMaybe(value?: string | null): Record<string, unknown> | undefined {
    if (!value) return undefined;
    try {
        const parsed = JSON.parse(value);
        if (parsed && typeof parsed === "object") return parsed as Record<string, unknown>;
    } catch {
        // swallow
    }
    return undefined;
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
    const [assets, setAssets] = useState<ProviderAsset[]>([]);
    const [selectedAsset, setSelectedAsset] = useState<ProviderAsset | null>(null);
    const [previewAsset, setPreviewAsset] = useState<ProviderAsset | null>(null);

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

                // Prefill identifier type from module providers if they declare supported identifiers.
                const defaultIdType = pickDefaultIdentifierType(mod);
                setIdentifiers([{ idType: defaultIdType, idValue: "" }]);
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

    const applyMergedAttributes = (merged?: Record<string, unknown>) => {
        if (!merged) return [] as string[];
        const applied: string[] = [];
        setAttributes((prev) => {
            const next: Record<string, any> = { ...prev };
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

    const handleUsePreviewAsset = () => {
        if (!previewAsset) return;
        setSelectedAsset(previewAsset);
    };

    const closePreview = () => {
        setPreviewAsset(null);
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
        setAssets([]);
        setSelectedAsset(null);
        try {
            const result = await lookupProviders({ moduleId: module.id, identifiers: ids });
            setLookupResult(result);
            const mergedApplied = applyMergedAttributes(result.mergedAttributes as Record<string, unknown> | undefined);
            if (mergedApplied.length > 0) {
                setAppliedKeys(mergedApplied);
            } else {
                const fromMapped = result.best?.mappedAttributes as Record<string, unknown> | undefined;
                const fromNormalized = result.best?.normalized as Record<string, unknown> | undefined;
                const fromNormalizedFields = parseJsonMaybe(result.best?.normalizedFields?.json);
                const fallback = fromMapped || fromNormalized || fromNormalizedFields;
                const fallbackApplied = applyMergedAttributes(fallback);
                setAppliedKeys(fallbackApplied);
            }
            const fetchedAssets = result.assets ?? [];
            setAssets(fetchedAssets);
            setSelectedAsset((prev) => {
                if (fetchedAssets.length === 0) return null;
                const preserved = prev && fetchedAssets.find((asset) => asset.url === prev.url);
                return preserved ?? fetchedAssets[0];
            });
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
            const payloadAttributes = { ...attributes };
            if (selectedAsset) {
                payloadAttributes.providerImageUrl = selectedAsset.url;
                if (selectedAsset.type) {
                    payloadAttributes.providerImageType = selectedAsset.type;
                }
            } else {
                delete payloadAttributes.providerImageUrl;
                delete payloadAttributes.providerImageType;
            }
            const payload = {
                moduleId: module.id,
                stateKey,
                title: title.trim() || undefined,
                attributes: payloadAttributes,
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

    const addIdentifier = () => setIdentifiers((prev) => [...prev, { idType: pickDefaultIdentifierType(module), idValue: "" }]);
    const removeIdentifier = (idx: number) => setIdentifiers((prev) => prev.filter((_, i) => i !== idx));

    const handleFieldChange = (key: string, value: string) => {
        setAttributes((prev) => ({ ...prev, [key]: value }));
        setTouched((prev) => ({ ...prev, [key]: true }));
    };

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
                {assets.length > 0 && (
                    <Card variant="outlined">
                        <CardContent>
                            <Stack spacing={1}>
                                <AssetGallery
                                    assets={assets}
                                    label="Suggested images"
                                    selectable
                                    selectedUrl={selectedAsset?.url}
                                    onPreview={(asset) => setPreviewAsset(asset)}
                                    onSelect={(asset) => setSelectedAsset(asset)}
                                />
                                <Stack direction="row" alignItems="center" justifyContent="space-between">
                                    <Typography variant="body2" color="text.secondary">
                                        {selectedAsset
                                            ? `Selected image (${selectedAsset.type || "primary"})`
                                            : "Selected image will be updated from the preview dialog."
                                        }
                                    </Typography>
                                    {selectedAsset && (
                                        <Button size="small" onClick={() => setSelectedAsset(null)}>
                                            Clear selection
                                        </Button>
                                    )}
                                </Stack>
                            </Stack>
                        </CardContent>
                    </Card>
                )}

                <AssetPreviewDialog
                    open={Boolean(previewAsset)}
                    asset={previewAsset}
                    title="Suggested image"
                    useLabel="Keep this image"
                    onClose={closePreview}
                    onUse={handleUsePreviewAsset}
                />
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
                                        name={field.fieldKey}
                                        label={field.label || field.fieldKey}
                                        value={attributes[field.fieldKey] ?? ""}
                                        onChange={(e) => handleFieldChange(field.fieldKey, e.target.value)}
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
