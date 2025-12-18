import {useEffect, useMemo, useState} from "react";
import {useNavigate, useParams} from "react-router-dom";
import {
    Alert,
    Box,
    Button,
    Card,
    CardContent,
    Chip,
    CircularProgress,
    Container,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    Divider,
    FormControl,
    InputLabel,
    MenuItem,
    Paper,
    Select,
    Stack,
    TextField,
    Typography,
} from "@mui/material";
import AssetGallery from "../components/AssetGallery";
import AssetPreviewDialog from "../components/AssetPreviewDialog";
import {changeItemState, deleteItem, getItem, getModule, lookupProviders, updateItem} from "../api";
import type {Item, ModuleDetails, ModuleField, ProviderAsset, ProviderLookupResult} from "../types";

function byOrder(a: { order?: number }, b: { order?: number }) {
    const ao = a.order ?? Number.MAX_SAFE_INTEGER;
    const bo = b.order ?? Number.MAX_SAFE_INTEGER;
    return ao - bo;
}

function formatLabel(key: string) {
    return key
        .replace(/[_-]+/g, " ")
        .split(" ")
        .map((segment) => segment.charAt(0).toUpperCase() + segment.slice(1).toLowerCase())
        .join(" ");
}

function isEmptyValue(v: unknown) {
    if (v === null || v === undefined) return true;
    if (typeof v === "string") return v.trim().length === 0;
    return false;
}

function parseJsonMaybe(value?: string | null): Record<string, unknown> | undefined {
    if (!value) return undefined;
    try {
        const parsed = JSON.parse(value);
        if (parsed && typeof parsed === "object") return parsed as Record<string, unknown>;
    } catch {
        // ignore
    }
    return undefined;
}

export default function ItemDetailPage() {
    const { collectionId, moduleKey, itemId } = useParams();
    const navigate = useNavigate();

    const [item, setItem] = useState<Item | null>(null);
    const [module, setModule] = useState<ModuleDetails | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [actionError, setActionError] = useState<string | null>(null);
    const [stateChanging, setStateChanging] = useState(false);
    const [deleteLoading, setDeleteLoading] = useState(false);
    const [editing, setEditing] = useState(false);
    const [editTitle, setEditTitle] = useState("");
    const [editAttributes, setEditAttributes] = useState<Record<string, unknown>>({});
    const [savingEdit, setSavingEdit] = useState(false);
    const [dialogError, setDialogError] = useState<string | null>(null);
    const [editLookupLoading, setEditLookupLoading] = useState(false);
    const [editLookupError, setEditLookupError] = useState<string | null>(null);
    const [editLookupResult, setEditLookupResult] = useState<ProviderLookupResult | null>(null);
    const [editAppliedKeys, setEditAppliedKeys] = useState<string[]>([]);
    const [editTouched, setEditTouched] = useState<Record<string, boolean>>({});
    const [detailAssets, setDetailAssets] = useState<ProviderAsset[]>([]);
    const [detailAssetsLoading, setDetailAssetsLoading] = useState(false);
    const [detailAssetsError, setDetailAssetsError] = useState<string | null>(null);
    const [editAssets, setEditAssets] = useState<ProviderAsset[]>([]);
    const [previewAsset, setPreviewAsset] = useState<ProviderAsset | null>(null);
    const [previewUseAction, setPreviewUseAction] = useState<(() => void) | null>(null);
    const [previewTitle, setPreviewTitle] = useState<string | undefined>(undefined);
    const [previewUseLabel, setPreviewUseLabel] = useState<string | undefined>(undefined);
    const [editSelectedAsset, setEditSelectedAsset] = useState<ProviderAsset | null>(null);

    useEffect(() => {
        let cancelled = false;
        async function load() {
            if (!collectionId || !itemId) {
                setError("Missing collection or item id.");
                setLoading(false);
                return;
            }
            setLoading(true);
            setError(null);
            try {
                const [fetchedItem, mod] = await Promise.all([
                    getItem(collectionId, itemId),
                    moduleKey ? getModule(moduleKey) : Promise.resolve(null),
                ]);
                if (cancelled) return;
                setItem(fetchedItem);
                if (mod) setModule(mod);
            } catch (err) {
                if (cancelled) return;
                setError((err as Error).message);
            } finally {
                if (!cancelled) setLoading(false);
            }
        }
        void load();
        return () => { cancelled = true; };
    }, [collectionId, itemId, moduleKey]);

    useEffect(() => {
        if (!item || !module?.id) {
            setDetailAssets([]);
            return;
        }
        const ids = (item.identifiers ?? [])
            .map((id) => ({ idType: id.idType, idValue: (id.idValue ?? "").trim() }))
            .filter((id) => id.idValue.length > 0);
        if (ids.length === 0) {
            setDetailAssets([]);
            return;
        }
        let cancelled = false;
        setDetailAssets([]);
        setDetailAssetsLoading(true);
        setDetailAssetsError(null);
        void lookupProviders({ moduleId: module.id, identifiers: ids })
            .then((result) => {
                if (cancelled) return;
                setDetailAssets(result.assets ?? []);
            })
            .catch((err) => {
                if (cancelled) return;
                setDetailAssets([]);
                setDetailAssetsError((err as Error).message);
            })
            .finally(() => {
                if (cancelled) return;
                setDetailAssetsLoading(false);
            });
        return () => {
            cancelled = true;
        };
    }, [item, module?.id]);

    const attributes = useMemo(() => (item?.attributes ?? {}) as Record<string, unknown>, [item]);
    const savedProviderAsset = useMemo<ProviderAsset | null>(() => {
        const url = typeof attributes.providerImageUrl === "string" ? attributes.providerImageUrl : undefined;
        if (!url) return null;
        const type = typeof attributes.providerImageType === "string" ? attributes.providerImageType : undefined;
        return { url, type };
    }, [attributes]);
    const sortedFields = useMemo<ModuleField[]>(() => (module?.fields ?? []).slice().sort(byOrder), [module?.fields]);
    const attributeEntries = useMemo(() => {
        const seen = new Set<string>();
        const primary = sortedFields.map((f) => {
            seen.add(f.fieldKey);
            return { key: f.fieldKey, value: attributes[f.fieldKey] };
        });
        const extras = Object.keys(attributes)
            .filter((key) => !seen.has(key))
            .map((key) => ({ key, value: attributes[key] }));
        return [...primary, ...extras];
    }, [attributes, sortedFields]);

    useEffect(() => {
        if (!item) return;
        const attrs = { ...(item.attributes ?? {}) };
        setEditTitle(item.title || "");
        setEditAttributes(attrs);
        const savedUrl = typeof attrs.providerImageUrl === "string" ? attrs.providerImageUrl : undefined;
        const savedType = typeof attrs.providerImageType === "string" ? attrs.providerImageType : undefined;
        setEditSelectedAsset(savedUrl ? { url: savedUrl, type: savedType } : null);
        const touchedMap: Record<string, boolean> = {};
        Object.entries(attrs).forEach(([key, value]) => {
            touchedMap[key] = !isEmptyValue(value);
        });
        setEditTouched(touchedMap);
    }, [item]);

    if (loading) {
        return (
            <Container maxWidth="md" sx={{ py: 4, textAlign: "center" }}>
                <CircularProgress />
                <Typography variant="body2" sx={{ mt: 2 }}>
                    Loading item…
                </Typography>
            </Container>
        );
    }

    if (error) {
        return (
            <Container maxWidth="md" sx={{ py: 4 }}>
                <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>
                <Button variant="outlined" onClick={() => navigate(-1)}>Back</Button>
            </Container>
        );
    }

    if (!item) return null;

    const displayTitle = item.title || (typeof attributes?.title === "string" ? attributes.title : undefined) || (typeof attributes?.name === "string" ? attributes.name : undefined) || "Untitled item";

    const handleStateChange = async (nextState: string) => {
        if (!collectionId) return;
        setStateChanging(true);
        setActionError(null);
        try {
            const updated = await changeItemState(collectionId, item.id, nextState);
            setItem(updated);
        } catch (err) {
            setActionError((err as Error).message);
        } finally {
            setStateChanging(false);
        }
    };

    const handleDelete = async () => {
        if (!collectionId) return;
        if (!window.confirm("Delete this item and all identifiers?")) return;
        setDeleteLoading(true);
        setActionError(null);
        try {
            await deleteItem(collectionId, item.id);
            navigate(`/collections/${collectionId}`);
        } catch (err) {
            setActionError((err as Error).message);
        } finally {
            setDeleteLoading(false);
        }
    };

    const handleEditFieldChange = (key: string, value: string) => {
        setEditAttributes((prev) => ({ ...prev, [key]: value }));
        setEditTouched((prev) => ({ ...prev, [key]: true }));
    };

    const applyEditMergedAttributes = (merged?: Record<string, unknown>) => {
        if (!merged) return [] as string[];
        const applied: string[] = [];
        setEditAttributes((prev) => {
            const next = { ...prev };
            Object.entries(merged).forEach(([key, value]) => {
                const alreadyTouched = editTouched[key];
                const empty = isEmptyValue(prev[key]);
                if (!alreadyTouched && empty) {
                    next[key] = value as any;
                    applied.push(key);
                }
            });
            return next;
        });
        if (applied.length > 0) {
            setEditTouched((prev) => {
                const next = { ...prev };
                applied.forEach((key) => {
                    next[key] = true;
                });
                return next;
            });
        }
        return applied;
    };

    const openPreview = (asset: ProviderAsset, options?: { title?: string; onUse?: () => void; useLabel?: string }) => {
        setPreviewAsset(asset);
        setPreviewTitle(options?.title);
        setPreviewUseLabel(options?.useLabel);
        setPreviewUseAction(options?.onUse ?? null);
    };

    const closePreview = () => {
        setPreviewAsset(null);
        setPreviewTitle(undefined);
        setPreviewUseLabel(undefined);
        setPreviewUseAction(null);
    };

    const handleEditLookup = async () => {
        if (!module?.id) {
            setEditLookupError("Module id missing; cannot run provider lookup.");
            return;
        }
        const ids = (item?.identifiers ?? [])
            .map((id) => ({ idType: id.idType, idValue: id.idValue?.trim() ?? "" }))
            .filter((id) => id.idValue.length > 0);
        if (ids.length === 0) {
            setEditLookupError("Add at least one identifier to fetch metadata.");
            return;
        }
        setEditLookupLoading(true);
        setEditLookupError(null);
        setEditLookupResult(null);
        setEditAssets([]);
        setEditSelectedAsset(null);
        setEditAppliedKeys([]);
        try {
            const result = await lookupProviders({ moduleId: module.id, identifiers: ids });
            setEditLookupResult(result);
            const fetchedAssets = result.assets ?? [];
            setEditAssets(fetchedAssets);
            setEditSelectedAsset((current) => {
                if (fetchedAssets.length === 0) return null;
                const preserved = current && fetchedAssets.find((asset) => asset.url === current.url);
                return preserved ?? fetchedAssets[0];
            });
            const mergedApplied = applyEditMergedAttributes(result.mergedAttributes as Record<string, unknown> | undefined);
            if (mergedApplied.length > 0) {
                setEditAppliedKeys(mergedApplied);
            } else {
                const fromMapped = result.best?.mappedAttributes as Record<string, unknown> | undefined;
                const fromNormalized = result.best?.normalized as Record<string, unknown> | undefined;
                const fromNormalizedFields = parseJsonMaybe(result.best?.normalizedFields?.json);
                const fallback = fromMapped || fromNormalized || fromNormalizedFields;
                const fallbackApplied = applyEditMergedAttributes(fallback);
                setEditAppliedKeys(fallbackApplied);
            }
        } catch (err) {
            setEditLookupError((err as Error).message);
        } finally {
            setEditLookupLoading(false);
        }
    };

    const handleSaveEdit = async () => {
        if (!collectionId) return;
        setSavingEdit(true);
        setDialogError(null);
        try {
            const payloadAttributes = { ...editAttributes };
            if (editSelectedAsset) {
                payloadAttributes.providerImageUrl = editSelectedAsset.url;
                if (editSelectedAsset.type) {
                    payloadAttributes.providerImageType = editSelectedAsset.type;
                } else {
                    delete payloadAttributes.providerImageType;
                }
            } else {
                delete payloadAttributes.providerImageUrl;
                delete payloadAttributes.providerImageType;
            }
            const payload = {
                title: editTitle.trim() || null,
                attributes: payloadAttributes,
            };
            const updated = await updateItem(collectionId, item.id, payload);
            setItem(updated);
            setEditing(false);
        } catch (err) {
            setDialogError((err as Error).message);
        } finally {
            setSavingEdit(false);
        }
    };

    const attributeSection = attributeEntries.length > 0 ? (
        <Box component={Paper} variant="outlined" sx={{ p: 3, bgcolor: (t) => t.palette.background.paper, boxShadow: (t) => t.shadows[1] }}>
            <Box sx={{ display: "grid", gap: 2, gridTemplateColumns: { xs: "1fr", sm: "repeat(2, minmax(0, 1fr))" } }}>
                {attributeEntries.map(({ key, value }) => (
                    <Box key={key} sx={{ display: "flex", flexDirection: "column", gap: 0.5 }}>
                        <Typography variant="subtitle2" color="text.secondary" sx={{ textTransform: "capitalize" }}>
                            {formatLabel(key)}
                        </Typography>
                        <Typography variant="body1" fontWeight={500} sx={{ wordBreak: "break-word" }}>
                            {renderValue(value)}
                        </Typography>
                    </Box>
                ))}
            </Box>
        </Box>
    ) : (
        <Typography color="text.secondary">No attributes set yet.</Typography>
    );

    const handleDialogClose = () => {
        if (!savingEdit) setEditing(false);
    };

    return (
        <Container maxWidth="md" sx={{ py: 4 }}>
            <Stack spacing={2}>
                <Stack direction="row" alignItems="center" spacing={1} flexWrap="wrap">
                    <Typography variant="h5" fontWeight={600}>{displayTitle}</Typography>
                    <Chip label={item.stateKey} color="secondary" />
                </Stack>
                <Typography variant="body2" color="text.secondary">
                    {module?.name || moduleKey || ""}
                </Typography>
                <Stack direction={{ xs: "column", sm: "row" }} spacing={2} alignItems="flex-start">
                    <FormControl size="small" sx={{ minWidth: 220 }}>
                        <InputLabel id="state-select-label">State</InputLabel>
                        <Select
                            labelId="state-select-label"
                            label="State"
                            value={item.stateKey}
                            onChange={(e) => handleStateChange(e.target.value as string)}
                            disabled={stateChanging || !module?.states?.length}
                        >
                            {(module?.states ?? [{ key: "OWNED", label: "Owned" }]).map((state) => (
                                <MenuItem key={state.key} value={state.key}>
                                    {state.label || state.key}
                                </MenuItem>
                            ))}
                        </Select>
                    </FormControl>
                    {stateChanging && <CircularProgress size={24} />}
                    <Stack direction="row" spacing={1}>
                        <Button variant="outlined" onClick={() => navigate(collectionId ? `/collections/${collectionId}` : "/collections")}>Back to collection</Button>
                        <Button variant="outlined" onClick={() => setEditing(true)}>Edit info</Button>
                        <Button variant="outlined" color="error" onClick={handleDelete} disabled={deleteLoading}>
                            {deleteLoading ? "Deleting…" : "Delete item"}
                        </Button>
                    </Stack>
                </Stack>
                {actionError && <Alert severity="error">{actionError}</Alert>}
                <Card>
                    <CardContent>
                        <Stack spacing={2}>
                            <Typography variant="h6">Details</Typography>
                            <Stack direction="row" spacing={3} flexWrap="wrap">
                                <Detail label="State" value={item.stateKey} />
                                {item.createdAt && <Detail label="Created" value={new Date(item.createdAt).toLocaleString()} />}
                                {item.updatedAt && <Detail label="Updated" value={new Date(item.updatedAt).toLocaleString()} />}
                            </Stack>
                            <Divider />
                            <Typography variant="subtitle1" color="text.secondary">
                                Attributes
                            </Typography>
                            {attributeSection}
                        </Stack>
                    </CardContent>
                </Card>
                {savedProviderAsset && (
                    <Card variant="outlined">
                        <CardContent>
                            <AssetGallery
                                assets={[savedProviderAsset]}
                                label="Saved image"
                                selectable
                                selectedUrl={savedProviderAsset.url}
                                onPreview={(asset) => openPreview(asset, { title: "Saved image" })}
                            />
                        </CardContent>
                    </Card>
                )}
                {detailAssetsLoading && (
                    <Card variant="outlined">
                        <CardContent>
                            <Stack direction="row" alignItems="center" spacing={1}>
                                <CircularProgress size={20} />
                                <Typography variant="body2" color="text.secondary">
                                    Loading provider images…
                                </Typography>
                            </Stack>
                        </CardContent>
                    </Card>
                )}
                {detailAssetsError && <Alert severity="error">{detailAssetsError}</Alert>}
                {detailAssets.length > 0 && (
                    <Card variant="outlined">
                        <CardContent>
                            <AssetGallery
                                assets={detailAssets}
                                label="Provider images"
                                onPreview={(asset) => openPreview(asset, { title: "Provider image" })}
                            />
                        </CardContent>
                    </Card>
                )}
            </Stack>

            <Dialog open={editing} onClose={handleDialogClose} fullWidth maxWidth="md">
                <DialogTitle>Edit item</DialogTitle>
                <DialogContent>
                    {dialogError && <Alert severity="error" sx={{ mb: 2 }}>{dialogError}</Alert>}
                    <Stack spacing={2} sx={{ mt: 1 }}>
                        <Stack direction="row" spacing={1} alignItems="center">
                            <Button
                                variant="outlined"
                                onClick={handleEditLookup}
                                disabled={editLookupLoading}
                            >
                                {editLookupLoading ? "Fetching…" : "Fetch from providers"}
                            </Button>
                            {editLookupLoading && <CircularProgress size={20} />}
                        </Stack>
                        {editLookupError && <Alert severity="error">{editLookupError}</Alert>}
                        {editLookupResult?.best && (
                            <Alert severity="info">
                                Fetched metadata from {editLookupResult.best.providerKey}
                                {editAppliedKeys.length > 0 ? ` · Applied ${editAppliedKeys.length} field${editAppliedKeys.length === 1 ? "" : "s"}` : ""}
                            </Alert>
                        )}
                        {editAssets.length > 0 && (
                            <Card variant="outlined" sx={{ mt: 1 }}>
                                <CardContent>
                                    <Stack spacing={1}>
                                        <AssetGallery
                                            assets={editAssets}
                                            label="Fetched images"
                                            selectable
                                            selectedUrl={editSelectedAsset?.url}
                                            onPreview={(asset) =>
                                                openPreview(asset, {
                                                    title: "Fetched image",
                                                    useLabel: "Keep this image",
                                                    onUse: () => setEditSelectedAsset(asset),
                                                })
                                            }
                                            onSelect={(asset) => setEditSelectedAsset(asset)}
                                        />
                                        <Stack direction="row" alignItems="center" justifyContent="space-between">
                                            <Typography variant="body2" color="text.secondary">
                                                {editSelectedAsset
                                                    ? `Selected image (${editSelectedAsset.type || "primary"})`
                                                    : "Tap an image to keep it as the primary picture."
                                                }
                                            </Typography>
                                            {editSelectedAsset && (
                                                <Button size="small" onClick={() => setEditSelectedAsset(null)}>
                                                    Clear selection
                                                </Button>
                                            )}
                                        </Stack>
                                    </Stack>
                                </CardContent>
                            </Card>
                        )}
                        <TextField
                            label="Title"
                            value={editTitle}
                            onChange={(e) => setEditTitle(e.target.value)}
                            fullWidth
                        />
                        <Box display="grid" gridTemplateColumns="repeat(auto-fit, minmax(240px, 1fr))" gap={2}>
                            {sortedFields.map((field) => (
                                <TextField
                                    key={field.fieldKey}
                                    label={field.label || formatLabel(field.fieldKey)}
                                    value={(editAttributes[field.fieldKey] ?? "") as string}
                                    onChange={(e) => handleEditFieldChange(field.fieldKey, e.target.value)}
                                />
                            ))}
                        </Box>
                    </Stack>
                </DialogContent>
                <DialogActions>
                    <Button onClick={handleDialogClose} disabled={savingEdit}>Cancel</Button>
                    <Button onClick={handleSaveEdit} variant="contained" disabled={savingEdit}>
                        {savingEdit ? "Saving…" : "Save changes"}
                    </Button>
                </DialogActions>
            </Dialog>
            <AssetPreviewDialog
                open={Boolean(previewAsset)}
                asset={previewAsset}
                title={previewTitle}
                useLabel={previewUseLabel}
                onClose={closePreview}
                onUse={previewUseAction ?? undefined}
            />
        </Container>
    );
}

function Detail({ label, value }: { label: string; value?: string }) {
    if (!value) return null;
    return (
        <Stack spacing={0.5}>
            <Typography variant="caption" color="text.secondary">{label}</Typography>
            <Typography>{value}</Typography>
        </Stack>
    );
}

function renderValue(value: unknown): string {
    if (value === null || value === undefined) return "";
    if (Array.isArray(value)) return value.map(renderValue).join(", ");
    if (typeof value === "string") return value;
    if (typeof value === "number" || typeof value === "boolean") return String(value);
    try {
        return JSON.stringify(value);
    } catch {
        return String(value);
    }
}
