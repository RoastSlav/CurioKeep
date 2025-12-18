import { useEffect, useState } from "react";
import { Alert, Box, Button, Card, CardContent, Chip, Dialog, DialogActions, DialogContent, DialogTitle, IconButton, LinearProgress, MenuItem, Select, Stack, TextField, Typography, alpha } from "@mui/material";
import RefreshIcon from "@mui/icons-material/Refresh";
import { Link as RouterLink } from "react-router-dom";
import { createCollection, disableCollectionModule, enableCollectionModule, getModule, listCollectionModules, listCollections, listItems, listModules } from "../api";
import type { Collection, CollectionModule, Item, ModuleDetails, ModuleSummary, Page } from "../types";
import { useToasts } from "../components/Toasts";

export default function CollectionsPage() {
    const [collections, setCollections] = useState<Collection[]>([]);
    const [modules, setModules] = useState<ModuleSummary[]>([]);
    const [selectedCollection, setSelectedCollection] = useState<string | null>(null);
    const [enabledModules, setEnabledModules] = useState<CollectionModule[]>([]);
    const [selectedModuleKey, setSelectedModuleKey] = useState<string | null>(null);
    const [moduleCache, setModuleCache] = useState<Record<string, ModuleDetails>>({});
    const [itemsPage, setItemsPage] = useState<Page<Item> | null>(null);
    const [itemsLoading, setItemsLoading] = useState(false);
    const [itemsError, setItemsError] = useState<string | null>(null);
    const [open, setOpen] = useState(false);
    const [name, setName] = useState("");
    const [description, setDescription] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [moduleLoading, setModuleLoading] = useState(false);
    const [pendingModule, setPendingModule] = useState<string>("");
    const toasts = useToasts();

    const refresh = () => {
        setLoading(true);
        Promise.all([listCollections(), listModules()])
            .then(([cols, mods]) => {
                setCollections(cols);
                setModules(mods);
                if (selectedCollection && cols.some((c) => c.id === selectedCollection)) {
                    void loadCollectionModules(selectedCollection);
                } else if (cols.length > 0) {
                    setSelectedCollection(cols[0].id);
                    void loadCollectionModules(cols[0].id);
                } else {
                    setSelectedModuleKey(null);
                    setEnabledModules([]);
                    setItemsPage(null);
                }
            })
            .catch((err) => setError((err as Error).message))
            .finally(() => setLoading(false));
    };

    const loadCollectionModules = async (collectionId: string) => {
        setModuleLoading(true);
        try {
            const enabled = await listCollectionModules(collectionId);
            setEnabledModules(enabled);
            if (enabled.length === 0) {
                setSelectedModuleKey(null);
                setItemsPage(null);
            } else {
                setSelectedModuleKey((prev) => (prev && enabled.some((m) => m.moduleKey === prev) ? prev : enabled[0].moduleKey));
            }
        } catch (err) {
            setError((err as Error).message);
        } finally {
            setModuleLoading(false);
        }
    };

    useEffect(() => {
        refresh();
    }, []);

    const handleCreate = async () => {
        try {
            setError(null);
            await createCollection(name, description);
            setOpen(false);
            setName("");
            setDescription("");
            refresh();
            toasts.show("Collection created", "success");
        } catch (err) {
            setError((err as Error).message);
        }
    };

    const handleSelectCollection = (id: string) => {
        setSelectedCollection(id);
        void loadCollectionModules(id);
    };

    const handleEnable = async () => {
        if (!selectedCollection || !pendingModule) return;
        try {
            await enableCollectionModule(selectedCollection, pendingModule);
            toasts.show("Module enabled", "success");
            setPendingModule("");
            void loadCollectionModules(selectedCollection);
        } catch (err) {
            toasts.show((err as Error).message, "error");
        }
    };

    const handleDisable = async (moduleKey: string) => {
        if (!selectedCollection) return;
        try {
            await disableCollectionModule(selectedCollection, moduleKey);
            toasts.show("Module disabled", "info");
            void loadCollectionModules(selectedCollection);
        } catch (err) {
            toasts.show((err as Error).message, "error");
        }
    };

    const ensureModuleDetails = async (moduleKey: string) => {
        if (moduleCache[moduleKey]) return moduleCache[moduleKey];
        const mod = await getModule(moduleKey);
        setModuleCache((prev) => ({ ...prev, [moduleKey]: mod }));
        return mod;
    };

    const loadItems = async (moduleKey?: string, page = 0) => {
        if (!selectedCollection || !moduleKey) {
            setItemsPage(null);
            return;
        }
        setItemsLoading(true);
        setItemsError(null);
        try {
            const mod = await ensureModuleDetails(moduleKey);
            if (!mod.id) throw new Error("Module id missing; cannot fetch items.");
            const pageData = await listItems(selectedCollection, mod.id, page, 25);
            setItemsPage(pageData);
        } catch (err) {
            setItemsError((err as Error).message);
            setItemsPage(null);
        } finally {
            setItemsLoading(false);
        }
    };

    useEffect(() => {
        if (!selectedCollection || !selectedModuleKey) {
            setItemsPage(null);
            return;
        }
        void loadItems(selectedModuleKey, 0);
    }, [selectedCollection, selectedModuleKey]);

    const selected = selectedCollection ? collections.find((c) => c.id === selectedCollection) : undefined;
    const canManage = selected ? ["OWNER", "ADMIN"].includes(selected.role) : false;

    const displayModuleName = (moduleKey?: string | null) => {
        if (!moduleKey) return "";
        const enabled = enabledModules.find((m) => m.moduleKey === moduleKey);
        const known = modules.find((m) => m.moduleKey === moduleKey || m.key === moduleKey);
        return enabled?.moduleName || known?.name || moduleKey;
    };

    return (
        <Stack spacing={2}>
            <Stack direction="row" justifyContent="space-between" alignItems="center">
                <Typography variant="h4">Collections</Typography>
                <Button variant="contained" onClick={() => setOpen(true)}>
                    New collection
                </Button>
            </Stack>
            {loading && <LinearProgress />}
            <Stack spacing={2} direction={{ xs: "column", md: "row" }} alignItems={{ md: "flex-start" }}>
                <Stack spacing={1} sx={{ minWidth: 320, flex: "0 0 320px" }}>
                    {collections.map((c) => (
                        <Card key={c.id} variant={c.id === selectedCollection ? "outlined" : undefined}>
                            <CardContent onClick={() => handleSelectCollection(c.id)} style={{ cursor: "pointer" }}>
                                <Stack direction="row" justifyContent="space-between" alignItems="center">
                                    <Typography variant="h6">{c.name}</Typography>
                                    <Chip label={c.role} size="small" color="secondary" variant="outlined" />
                                </Stack>
                                <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                                    {c.description || "No description"}
                                </Typography>
                            </CardContent>
                        </Card>
                    ))}
                    {collections.length === 0 && <Alert severity="info">No collections yet. Create one to get started.</Alert>}
                </Stack>

                <Stack spacing={2} sx={{ flex: 1 }}>
                    <Card>
                        <CardContent>
                            <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
                                <Typography variant="h6">Modules in collection</Typography>
                                <Stack direction="row" spacing={1} alignItems="center">
                                    <Select
                                        value={pendingModule}
                                        onChange={(e) => setPendingModule(e.target.value as string)}
                                        displayEmpty
                                        size="small"
                                        sx={{ minWidth: 200 }}
                                        disabled={!selectedCollection || !canManage}
                                    >
                                        <MenuItem value="">
                                            <em>Select module to enable</em>
                                        </MenuItem>
                                        {modules
                                            .filter((m) => !enabledModules.some((em) => em.moduleKey === m.moduleKey))
                                            .map((m) => (
                                                <MenuItem key={m.moduleKey} value={m.moduleKey}>
                                                    {m.name || m.moduleKey}
                                                </MenuItem>
                                            ))}
                                    </Select>
                                    <Button variant="contained" onClick={handleEnable} disabled={!pendingModule || !selectedCollection || !canManage}>
                                        Enable
                                    </Button>
                                    <IconButton onClick={() => selectedCollection && loadCollectionModules(selectedCollection)} disabled={!selectedCollection}>
                                        <RefreshIcon />
                                    </IconButton>
                                </Stack>
                            </Stack>

                            {moduleLoading && <LinearProgress />}
                            {!moduleLoading && enabledModules.length === 0 && (
                                <Alert severity="info">No modules enabled for this collection.</Alert>
                            )}
                            <Stack spacing={1}>
                                {enabledModules.map((m) => (
                                    <Box
                                        key={m.moduleKey}
                                        sx={{
                                            display: "flex",
                                            alignItems: "center",
                                            justifyContent: "space-between",
                                            border: 1,
                                            borderColor: (t) => alpha(t.palette.secondary.main, 0.3),
                                            borderRadius: 1,
                                            p: 1,
                                            backgroundColor: selectedModuleKey === m.moduleKey ? (t) => alpha(t.palette.secondary.main, 0.05) : undefined,
                                        }}
                                    >
                                        <Box sx={{ cursor: "pointer" }} onClick={() => setSelectedModuleKey(m.moduleKey)}>
                                            <Typography>{m.moduleName || m.moduleKey}</Typography>
                                            <Typography variant="caption" color="text.secondary">{m.moduleKey}</Typography>
                                        </Box>
                                        <Stack direction="row" spacing={1} alignItems="center">
                                            <Button
                                                size="small"
                                                variant={selectedModuleKey === m.moduleKey ? "contained" : "outlined"}
                                                onClick={() => setSelectedModuleKey(m.moduleKey)}
                                            >
                                                View items
                                            </Button>
                                            <Button
                                                variant="outlined"
                                                color="secondary"
                                                disabled={!canManage}
                                                onClick={() => handleDisable(m.moduleKey)}
                                                sx={{
                                                    '&.Mui-disabled': {
                                                        color: (t) => `${t.palette.secondary.main} !important`,
                                                        borderColor: (t) => `${t.palette.secondary.main} !important`,
                                                        opacity: 0.9,
                                                    },
                                                }}
                                            >
                                                Disable
                                            </Button>
                                        </Stack>
                                    </Box>
                                ))}
                            </Stack>
                        </CardContent>
                    </Card>

                    <Card>
                        <CardContent>
                            <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
                                <Typography variant="h6">Items</Typography>
                                <Stack direction="row" spacing={1} alignItems="center">
                                    <Select
                                        value={selectedModuleKey || ""}
                                        onChange={(e) => setSelectedModuleKey((e.target.value as string) || null)}
                                        displayEmpty
                                        size="small"
                                        sx={{ minWidth: 200 }}
                                        disabled={enabledModules.length === 0}
                                    >
                                        <MenuItem value="">
                                            <em>Select module</em>
                                        </MenuItem>
                                        {enabledModules.map((m) => (
                                            <MenuItem key={m.moduleKey} value={m.moduleKey}>
                                                {m.moduleName || m.moduleKey}
                                            </MenuItem>
                                        ))}
                                    </Select>
                                    <Button variant="outlined" onClick={() => void loadItems(selectedModuleKey || undefined)} disabled={!selectedModuleKey || itemsLoading}>
                                        Refresh
                                    </Button>
                                    <Button
                                        variant="contained"
                                        component={RouterLink}
                                        to={selectedCollection && selectedModuleKey ? `/collections/${selectedCollection}/modules/${selectedModuleKey}/add` : "#"}
                                        disabled={!selectedCollection || !selectedModuleKey}
                                    >
                                        Add item
                                    </Button>
                                </Stack>
                            </Stack>
                            {itemsLoading && <LinearProgress />}
                            {itemsError && <Alert severity="error" sx={{ mb: 2 }}>{itemsError}</Alert>}
                            {!itemsLoading && !itemsError && (!itemsPage || itemsPage.content.length === 0) && (
                                <Alert severity="info">No items for this module yet.</Alert>
                            )}
                            <Stack spacing={1}>
                                {itemsPage?.content.map((item) => {
                                    const attrs = item.attributes as Record<string, unknown>;
                                    const attrTitle = typeof attrs?.title === "string" ? attrs.title : undefined;
                                    const attrName = typeof attrs?.name === "string" ? attrs.name : undefined;
                                    const displayTitle = item.title || attrTitle || attrName || "Untitled item";
                                    const savedImageUrl = typeof attrs?.providerImageUrl === "string" && attrs.providerImageUrl ? attrs.providerImageUrl : undefined;
                                    return (
                                        <Box
                                            key={item.id}
                                            sx={{
                                                display: "flex",
                                                alignItems: "center",
                                                justifyContent: "space-between",
                                                border: 1,
                                                borderColor: (t) => alpha(t.palette.divider, 0.8),
                                                borderRadius: 1,
                                                p: 1,
                                            }}
                                        >
                                            <Stack direction="row" alignItems="center" spacing={2} sx={{ flex: 1 }}>
                                                {savedImageUrl ? (
                                                    <Box
                                                        component="img"
                                                        src={savedImageUrl}
                                                        alt={displayTitle}
                                                        sx={{
                                                            width: 48,
                                                            height: 48,
                                                            borderRadius: 1,
                                                            objectFit: "cover",
                                                            border: 1,
                                                            borderColor: (t) => alpha(t.palette.divider, 1),
                                                        }}
                                                    />
                                                ) : (
                                                    <Box
                                                        sx={{
                                                            width: 48,
                                                            height: 48,
                                                            borderRadius: 1,
                                                            border: 1,
                                                            borderColor: (t) => alpha(t.palette.divider, 1),
                                                            backgroundColor: (t) => alpha(t.palette.primary.main, 0.12),
                                                        }}
                                                    />
                                                )}
                                                <Box>
                                                    <Typography fontWeight={600}>{displayTitle}</Typography>
                                                    <Typography variant="caption" color="text.secondary">
                                                        {displayModuleName(selectedModuleKey)}
                                                        {item.updatedAt ? ` · Updated ${new Date(item.updatedAt).toLocaleString()}` : ""}
                                                    </Typography>
                                                </Box>
                                            </Stack>
                                            <Stack direction="row" spacing={1} alignItems="center">
                                                <Chip size="small" label={item.stateKey} />
                                                {selectedCollection && selectedModuleKey && (
                                                    <Button
                                                        size="small"
                                                        component={RouterLink}
                                                        to={`/collections/${selectedCollection}/modules/${selectedModuleKey}/items/${item.id}`}
                                                    >
                                                        Open
                                                    </Button>
                                                )}
                                            </Stack>
                                        </Box>
                                    );
                                })}
                            </Stack>
                        </CardContent>
                    </Card>
                </Stack>
            </Stack>

            <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth="sm">
                <DialogTitle>Create collection</DialogTitle>
                <DialogContent>
                    <Stack spacing={2} sx={{ pt: 1 }}>
                        {error && <Alert severity="error">{error}</Alert>}
                        <TextField label="Name" value={name} onChange={(e) => setName(e.target.value)} fullWidth required />
                        <TextField
                            label="Description"
                            value={description}
                            onChange={(e) => setDescription(e.target.value)}
                            fullWidth
                            multiline
                            minRows={2}
                        />
                    </Stack>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setOpen(false)}>Cancel</Button>
                    <Button onClick={handleCreate} variant="contained" disabled={!name.trim()}>
                        Create
                    </Button>
                </DialogActions>
            </Dialog>
        </Stack>
    );
}
