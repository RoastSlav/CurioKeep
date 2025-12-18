import { Add } from "@mui/icons-material";
import Grid from "@mui/material/Grid";
import { Box, Button, Stack, Typography } from "@mui/material";
import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import type { Collection, CreateCollectionRequest } from "../../../api/types";
import EmptyState from "../../../components/EmptyState";
import ErrorState from "../../../components/ErrorState";
import LoadingState from "../../../components/LoadingState";
import ConfirmDialog from "../../../components/ConfirmDialog";
import { useToast } from "../../../components/Toasts";
import CollectionCard from "../components/CollectionCard";
import CreateCollectionDialog from "../components/CreateCollectionDialog";
import EditCollectionDialog from "../components/EditCollectionDialog";
import { createCollection, deleteCollection, listCollections, updateCollection, type UpdateCollectionRequest } from "../api";

export default function CollectionsPage() {
    const navigate = useNavigate();
    const { showToast } = useToast();

    const [collections, setCollections] = useState<Collection[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const [createOpen, setCreateOpen] = useState(false);
    const [editTarget, setEditTarget] = useState<Collection | null>(null);
    const [confirmDelete, setConfirmDelete] = useState<Collection | null>(null);

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

    const hasCollections = useMemo(() => collections.length > 0, [collections]);

    const handleCreate = async (payload: CreateCollectionRequest) => {
        try {
            const created = await createCollection(payload);
            setCollections((prev) => [created, ...prev]);
            setCreateOpen(false);
            showToast("Collection created", "success");
            navigate(`/collections/${created.id}`);
        } catch (err: any) {
            showToast(err?.message || "Failed to create collection", "error");
            throw err;
        }
    };

    const handleEdit = async (payload: UpdateCollectionRequest) => {
        if (!editTarget) return;
        try {
            const updated = await updateCollection(editTarget.id, payload);
            setCollections((prev) => prev.map((c) => (c.id === updated.id ? updated : c)));
            setEditTarget(null);
            showToast("Collection updated", "success");
        } catch (err: any) {
            showToast(err?.message || "Failed to update collection", "error");
            throw err;
        }
    };

    const handleDelete = async () => {
        if (!confirmDelete) return;
        const target = confirmDelete;
        try {
            await deleteCollection(target.id);
            setCollections((prev) => prev.filter((c) => c.id !== target.id));
            showToast("Collection deleted", "success");
        } catch (err: any) {
            showToast(err?.message || "Failed to delete collection", "error");
        } finally {
            setConfirmDelete(null);
        }
    };

    if (loading) return <LoadingState message="Loading collections..." />;
    if (error) return <ErrorState title="Could not load collections" message={error} onRetry={() => window.location.reload()} />;

    return (
        <Stack spacing={3}>
            <Stack direction={{ xs: "column", sm: "row" }} spacing={2} alignItems={{ xs: "flex-start", sm: "center" }} justifyContent="space-between">
                <Stack spacing={0.5}>
                    <Typography variant="h4" fontWeight={700}>
                        Collections
                    </Typography>
                    <Typography color="text.secondary">Collections you own or are shared with you.</Typography>
                </Stack>
                <Button variant="contained" startIcon={<Add />} onClick={() => setCreateOpen(true)}>
                    New collection
                </Button>
            </Stack>

            {hasCollections ? (
                <Grid container spacing={2}>
                    {collections.map((collection) => (
                        <Grid size={{ xs: 12, sm: 6, md: 4 }} key={collection.id}>
                            <CollectionCard
                                collection={collection}
                                onEdit={(c) => setEditTarget(c)}
                                onDelete={(c) => setConfirmDelete(c)}
                            />
                        </Grid>
                    ))}
                </Grid>
            ) : (
                <EmptyState
                    title="No collections yet"
                    description="Create your first collection to start organizing."
                    actionLabel="Create collection"
                    onAction={() => setCreateOpen(true)}
                    secondary={
                        <Box>
                            <Typography variant="caption" color="text.secondary">
                                Tip: you can share collections with teammates once created.
                            </Typography>
                        </Box>
                    }
                />
            )}

            <CreateCollectionDialog open={createOpen} onClose={() => setCreateOpen(false)} onCreate={handleCreate} />
            <EditCollectionDialog open={Boolean(editTarget)} collection={editTarget} onClose={() => setEditTarget(null)} onSave={handleEdit} />
            <ConfirmDialog
                open={Boolean(confirmDelete)}
                title="Delete collection?"
                message="This action cannot be undone. Items inside the collection may also be removed."
                confirmLabel="Delete"
                onConfirm={handleDelete}
                onClose={() => setConfirmDelete(null)}
            />
        </Stack>
    );
}
