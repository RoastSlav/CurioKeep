import { useEffect, useState } from "react";
import { Alert, Box, Button, Card, CardContent, Chip, Dialog, DialogActions, DialogContent, DialogTitle, IconButton, LinearProgress, MenuItem, Select, Stack, TextField, Typography } from "@mui/material";
import RefreshIcon from "@mui/icons-material/Refresh";
import { createCollection, disableCollectionModule, enableCollectionModule, listCollectionModules, listCollections, listModules } from "../api";
import type { Collection, CollectionModule, ModuleSummary } from "../types";
import { useToasts } from "../components/Toasts";

export default function CollectionsPage() {
    const [collections, setCollections] = useState<Collection[]>([]);
    const [modules, setModules] = useState<ModuleSummary[]>([]);
    const [selectedCollection, setSelectedCollection] = useState<string | null>(null);
    const [enabledModules, setEnabledModules] = useState<CollectionModule[]>([]);
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

    const selected = selectedCollection ? collections.find((c) => c.id === selectedCollection) : undefined;
    const canManage = selected ? ["OWNER", "ADMIN"].includes(selected.role) : false;

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
                                    <Chip label={c.role} size="small" />
                                </Stack>
                                <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                                    {c.description || "No description"}
                                </Typography>
                            </CardContent>
                        </Card>
                    ))}
                    {collections.length === 0 && <Alert severity="info">No collections yet. Create one to get started.</Alert>}
                </Stack>

                <Card sx={{ flex: 1 }}>
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
                                    {modules.map((m) => (
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
                                <Box key={m.moduleKey} sx={{ display: "flex", alignItems: "center", justifyContent: "space-between", border: "1px solid #eee", borderRadius: 1, p: 1 }}>
                                    <div>
                                        <Typography>{m.moduleName || m.moduleKey}</Typography>
                                        <Typography variant="caption" color="text.secondary">{m.moduleKey}</Typography>
                                    </div>
                                    <Button color="secondary" disabled={!canManage} onClick={() => handleDisable(m.moduleKey)}>
                                        Disable
                                    </Button>
                                </Box>
                            ))}
                        </Stack>
                    </CardContent>
                </Card>
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
