import { Add, Launch } from "@mui/icons-material";
import {
    Button,
    Card,
    CardContent,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    Skeleton,
    Stack,
    TextField,
    Typography,
} from "@mui/material";
import Grid from "@mui/material/Grid";
import { useEffect, useMemo, useRef, useState } from "react";
import { createCollection, listCollections } from "../api/collections";
import type { Collection } from "../api/types";
import CollectionCard from "../components/CollectionCard";
import EmptyState from "../components/EmptyState";
import ErrorState from "../components/ErrorState";
import StatCard from "../components/StatCard";
import { useToast } from "../components/Toasts";

export default function DashboardPage() {
    const { showToast } = useToast();
    const [collections, setCollections] = useState<Collection[]>([]);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);

    const [createOpen, setCreateOpen] = useState<boolean>(false);
    const [createName, setCreateName] = useState<string>("");
    const [createDescription, setCreateDescription] = useState<string>("");
    const [creating, setCreating] = useState<boolean>(false);

    const loggedMissingStats = useRef<boolean>(false);

    const hasItemsCount = useMemo(() => collections.some((c) => c.itemsCount !== undefined), [collections]);

    useEffect(() => {
        const load = async () => {
            setLoading(true);
            setError(null);
            try {
                const data = await listCollections();
                setCollections(data);
            } catch (err: any) {
                setError(err?.message || "Failed to load collections");
            } finally {
                setLoading(false);
            }
        };

        void load();
    }, []);

    useEffect(() => {
        if (!hasItemsCount && !loggedMissingStats.current && !loading) {
            loggedMissingStats.current = true;
            // eslint-disable-next-line no-console
            console.info("Item counts are not provided by the backend yet. Showing placeholders in the dashboard stats.");
        }
    }, [hasItemsCount, loading]);

    const collectionsCount = collections.length;
    const itemsCount = hasItemsCount ? collections.reduce((sum, c) => sum + (c.itemsCount ?? 0), 0) : undefined;

    const openCreateDialog = () => {
        setCreateOpen(true);
        setCreateName("");
        setCreateDescription("");
    };

    const handleCreate = async () => {
        if (!createName.trim()) {
            showToast("Collection name is required", "warning");
            return;
        }
        setCreating(true);
        try {
            const created = await createCollection({ name: createName.trim(), description: createDescription.trim() || undefined });
            setCollections((prev) => [created, ...prev]);
            setCreateOpen(false);
            showToast("Collection created", "success");
        } catch (err: any) {
            showToast(err?.message || "Failed to create collection", "error");
        } finally {
            setCreating(false);
        }
    };

    if (error) {
        return <ErrorState title="Could not load dashboard" message={error} onRetry={() => window.location.reload()} />;
    }

    return (
        <Stack spacing={3}>
            <Stack direction={{ xs: "column", sm: "row" }} spacing={2} alignItems={{ xs: "flex-start", sm: "center" }} justifyContent="space-between">
                <Stack spacing={0.5}>
                    <Typography variant="h4" fontWeight={700}>
                        Dashboard
                    </Typography>
                    <Typography color="text.secondary">Overview of your collections</Typography>
                </Stack>
                <Stack direction="row" spacing={1}>
                    <Button variant="contained" startIcon={<Add />} onClick={openCreateDialog}>
                        Create collection
                    </Button>
                    <Button
                        variant="text"
                        endIcon={<Launch />}
                        href="https://github.com/RoastSlav/CurioKeep"
                        target="_blank"
                        rel="noreferrer"
                    >
                        Learn about modules
                    </Button>
                </Stack>
            </Stack>

            <Grid container spacing={2}>
                <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                    <StatCard label="Collections" value={collectionsCount} loading={loading} />
                </Grid>
                <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                    <StatCard
                        label="Items"
                        value={itemsCount ?? "—"}
                        loading={loading}
                        hint={!hasItemsCount ? "Needs backend support" : undefined}
                    />
                </Grid>
                <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                    <StatCard label="Owned vs Wishlist" value="—" loading={loading} hint="Needs backend support" />
                </Grid>
            </Grid>

            {loading ? (
                <Grid container spacing={2}>
                    {[1, 2, 3].map((key) => (
                        <Grid key={key} size={{ xs: 12, sm: 6, md: 4 }}>
                            <Card variant="outlined">
                                <CardContent>
                                    <Skeleton variant="text" width="60%" height={32} />
                                    <Skeleton variant="text" width="40%" />
                                    <Skeleton variant="rectangular" height={80} sx={{ mt: 1 }} />
                                </CardContent>
                            </Card>
                        </Grid>
                    ))}
                </Grid>
            ) : collections.length === 0 ? (
                <EmptyState
                    title="No collections yet"
                    description="Create your first collection to start organizing your items."
                    actionLabel="Create collection"
                    onAction={openCreateDialog}
                    secondary={
                        <Button
                            variant="text"
                            endIcon={<Launch />}
                            href="https://github.com/RoastSlav/CurioKeep"
                            target="_blank"
                            rel="noreferrer"
                        >
                            Learn about modules
                        </Button>
                    }
                />
            ) : (
                <Grid container spacing={2}>
                    {collections.map((collection) => (
                        <Grid key={collection.id} size={{ xs: 12, sm: 6, md: 4 }}>
                            <CollectionCard collection={collection} />
                        </Grid>
                    ))}
                </Grid>
            )}

            <Dialog open={createOpen} onClose={() => !creating && setCreateOpen(false)} fullWidth maxWidth="sm">
                <DialogTitle>Create collection</DialogTitle>
                <DialogContent sx={{ pt: 1 }}>
                    <Stack spacing={2}>
                        <TextField
                            autoFocus
                            fullWidth
                            label="Name"
                            value={createName}
                            onChange={(e) => setCreateName(e.target.value)}
                            required
                        />
                        <TextField
                            fullWidth
                            label="Description"
                            value={createDescription}
                            onChange={(e) => setCreateDescription(e.target.value)}
                            multiline
                            minRows={2}
                        />
                    </Stack>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setCreateOpen(false)} disabled={creating}>
                        Cancel
                    </Button>
                    <Button onClick={handleCreate} variant="contained" disabled={creating}>
                        {creating ? "Creating..." : "Create"}
                    </Button>
                </DialogActions>
            </Dialog>
        </Stack>
    );
}
