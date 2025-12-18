import { Button, Dialog, DialogActions, DialogContent, DialogTitle, Stack, TextField } from "@mui/material";
import { useEffect, useState } from "react";
import type { Collection } from "../../../api/types";
import type { UpdateCollectionRequest } from "../api";

export default function EditCollectionDialog({
    open,
    collection,
    onClose,
    onSave,
}: {
    open: boolean;
    collection: Collection | null;
    onClose: () => void;
    onSave: (payload: UpdateCollectionRequest) => Promise<void>;
}) {
    const [name, setName] = useState("");
    const [description, setDescription] = useState("");
    const [saving, setSaving] = useState(false);

    useEffect(() => {
        if (collection && open) {
            setName(collection.name ?? "");
            setDescription(collection.description ?? "");
            setSaving(false);
        }
    }, [collection, open]);

    if (!collection) return null;

    const handleSubmit = async () => {
        if (!name.trim()) return;
        setSaving(true);
        try {
            await onSave({ name: name.trim(), description: description.trim() || undefined });
        } finally {
            setSaving(false);
        }
    };

    const disabled = saving || !name.trim();

    return (
        <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
            <DialogTitle>Edit collection</DialogTitle>
            <DialogContent sx={{ pt: 1 }}>
                <Stack spacing={2} sx={{ pt: 1 }}>
                    <TextField
                        label="Name"
                        value={name}
                        onChange={(e) => setName(e.target.value)}
                        required
                        autoFocus
                    />
                    <TextField
                        label="Description"
                        value={description}
                        onChange={(e) => setDescription(e.target.value)}
                        multiline
                        minRows={2}
                    />
                </Stack>
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose} disabled={saving}>Cancel</Button>
                <Button onClick={handleSubmit} variant="contained" disabled={disabled}>
                    {saving ? "Saving..." : "Save"}
                </Button>
            </DialogActions>
        </Dialog>
    );
}
