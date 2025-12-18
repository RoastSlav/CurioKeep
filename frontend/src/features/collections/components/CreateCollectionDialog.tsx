import { Button, Dialog, DialogActions, DialogContent, DialogTitle, Stack, TextField } from "@mui/material";
import { useEffect, useState } from "react";
import type { CreateCollectionRequest } from "../../../api/types";

export default function CreateCollectionDialog({ open, onClose, onCreate }: { open: boolean; onClose: () => void; onCreate: (payload: CreateCollectionRequest) => Promise<void>; }) {
    const [name, setName] = useState("");
    const [description, setDescription] = useState("");
    const [saving, setSaving] = useState(false);

    useEffect(() => {
        if (open) {
            setName("");
            setDescription("");
            setSaving(false);
        }
    }, [open]);

    const handleSubmit = async () => {
        if (!name.trim()) return;
        setSaving(true);
        try {
            await onCreate({ name: name.trim(), description: description.trim() || undefined });
        } finally {
            setSaving(false);
        }
    };

    const disabled = saving || !name.trim();

    return (
        <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
            <DialogTitle>New collection</DialogTitle>
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
                    {saving ? "Creating..." : "Create"}
                </Button>
            </DialogActions>
        </Dialog>
    );
}
