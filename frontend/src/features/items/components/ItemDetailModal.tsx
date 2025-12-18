import { useEffect, useMemo, useState } from "react";
import {
    Box,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    Divider,
    IconButton,
    Stack,
    Typography,
} from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import type { Item, ModuleDefinition } from "../../../api/types";
import { useToast } from "../../../components/Toasts";
import ItemForm from "./ItemForm";
import ItemActions from "./ItemActions";
import { changeItemState, deleteItem, getItem, updateItem } from "../api";
import LoadingState from "../../../components/LoadingState";
import ErrorState from "../../../components/ErrorState";
import ImageViewerDialog from "./ImageViewerDialog";

function formatValue(value: unknown): string {
    if (value === null || value === undefined) return "-";
    if (Array.isArray(value)) return value.join(", ");
    if (typeof value === "object") return JSON.stringify(value, null, 2);
    return String(value);
}

export default function ItemDetailModal({
    open,
    collectionId,
    itemId,
    moduleDefinition,
    role,
    onClose,
    onUpdated,
    onDeleted,
}: {
    open: boolean;
    collectionId: string;
    itemId: string;
    moduleDefinition: ModuleDefinition | null | undefined;
    role?: string;
    onClose: () => void;
    onUpdated?: (item: Item) => void;
    onDeleted?: () => void;
}) {
    const { showToast } = useToast();
    const [item, setItem] = useState<Item | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [editing, setEditing] = useState(false);
    const [changingState, setChangingState] = useState(false);
    const [deleting, setDeleting] = useState(false);
    const [reloadKey, setReloadKey] = useState(0);
    const [viewerOpen, setViewerOpen] = useState(false);

    useEffect(() => {
        if (!open) return;
        const fetchItem = async () => {
            setLoading(true);
            setError(null);
            try {
                const data = await getItem(collectionId, itemId);
                setItem(data);
            } catch (err: any) {
                setError(err?.message || "Failed to load item");
            } finally {
                setLoading(false);
            }
        };
        void fetchItem();
    }, [open, collectionId, itemId, reloadKey]);

    const fields = useMemo(() => moduleDefinition?.fields || [], [moduleDefinition]);
    const states = useMemo(() => moduleDefinition?.states || [], [moduleDefinition]);

    const handleStateChange = async (stateKey: string) => {
        if (!item) return;
        const prev = item;
        setItem({ ...item, stateKey });
        setChangingState(true);
        try {
            const updated = await changeItemState(collectionId, item.id, stateKey);
            setItem(updated);
            onUpdated?.(updated);
            showToast("State updated", "success");
        } catch (err: any) {
            setItem(prev);
            showToast(err?.message || "Failed to change state", "error");
        } finally {
            setChangingState(false);
        }
    };

    const handleSubmit = async (attributes: Record<string, any>) => {
        if (!item) return;
        const payload = { attributes } as Partial<Item>;
        const updated = await updateItem(collectionId, item.id, payload);
        setItem(updated);
        onUpdated?.(updated);
        setEditing(false);
        showToast("Item saved", "success");
    };

    const handleDelete = async () => {
        if (!item) return;
        const confirmed = window.confirm("Delete this item? This cannot be undone.");
        if (!confirmed) return;
        setDeleting(true);
        try {
            await deleteItem(collectionId, item.id);
            showToast("Item deleted", "success");
            onDeleted?.();
            onClose();
        } catch (err: any) {
            showToast(err?.message || "Failed to delete item", "error");
        } finally {
            setDeleting(false);
        }
    };

    const stateLabel = states.find((s) => s.key === item?.stateKey)?.label || item?.stateKey;
    const imageUrl = (item?.attributes?.providerImageUrl as string) || null;
    const title = (item?.attributes?.title as string) || item?.id || "";

    return (
        <Dialog open={open} onClose={onClose} fullWidth maxWidth="md">
            <DialogTitle sx={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                <Stack spacing={0.5}>
                    <Typography variant="h6" fontWeight={700}>
                        Item {item?.id || ""}
                    </Typography>
                    {item && (
                        <Typography variant="body2" color="text.secondary">
                            State: {stateLabel}
                        </Typography>
                    )}
                </Stack>
                <IconButton onClick={onClose}>
                    <CloseIcon />
                </IconButton>
            </DialogTitle>
            <Divider />
            <DialogContent dividers>
                {loading ? (
                    <LoadingState message="Loading item..." />
                ) : error ? (
                    <ErrorState
                        title="Could not load item"
                        message={error}
                        onRetry={() => setReloadKey((k) => k + 1)}
                    />
                ) : item && !editing ? (
                    <Stack spacing={2}>
                        {imageUrl && (
                            <Box
                                sx={{
                                    width: "100%",
                                    borderRadius: 1.5,
                                    overflow: "hidden",
                                    border: 1,
                                    borderColor: "divider",
                                    cursor: "pointer",
                                }}
                                onClick={() => setViewerOpen(true)}
                            >
                                <Box
                                    component="img"
                                    src={imageUrl}
                                    alt={title}
                                    sx={{ width: "100%", maxHeight: 360, objectFit: "contain", backgroundColor: "black" }}
                                />
                            </Box>
                        )}
                        <Stack direction="row" justifyContent="space-between" alignItems="center">
                            <Typography variant="subtitle1" fontWeight={700}>
                                Details
                            </Typography>
                            <ItemActions
                                role={role}
                                stateKey={item.stateKey}
                                states={states}
                                onChangeState={handleStateChange}
                                onEdit={() => setEditing(true)}
                                onDelete={handleDelete}
                                disabled={changingState || deleting}
                            />
                        </Stack>
                        <Stack spacing={1.25}>
                            {(fields.length ? fields : Object.keys(item.attributes || {}).map((key) => ({ key, label: key }))).map(
                                (field) => (
                                    <Stack key={field.key} spacing={0.25}>
                                        <Typography variant="body2" fontWeight={700}>
                                            {field.label || field.key}
                                        </Typography>
                                        <Box sx={{ whiteSpace: "pre-wrap" }}>{formatValue(item.attributes?.[field.key])}</Box>
                                    </Stack>
                                )
                            )}
                        </Stack>
                    </Stack>
                ) : item && editing ? (
                    <ItemForm
                        moduleDefinition={moduleDefinition}
                        initialAttributes={item.attributes}
                        onSubmit={handleSubmit}
                        onCancel={() => setEditing(false)}
                        disabled={changingState || deleting}
                        submitLabel="Save"
                    />
                ) : null}
            </DialogContent>
            <DialogActions>
                {!editing && (
                    <ItemActions
                        role={role}
                        stateKey={item?.stateKey || ""}
                        states={states}
                        onChangeState={item ? handleStateChange : undefined}
                        onEdit={item ? () => setEditing(true) : undefined}
                        onDelete={item ? handleDelete : undefined}
                        disabled={changingState || deleting}
                        compact
                    />
                )}
            </DialogActions>
            <ImageViewerDialog open={viewerOpen} src={imageUrl} title={title || "Image"} onClose={() => setViewerOpen(false)} />
        </Dialog>
    );
}
