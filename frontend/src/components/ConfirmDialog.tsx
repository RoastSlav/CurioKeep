import { Button, Dialog, DialogActions, DialogContent, DialogTitle, Typography } from "@mui/material";
import type { ReactNode } from "react";

type Props = {
    open: boolean;
    title?: string;
    message?: ReactNode;
    confirmLabel?: string;
    cancelLabel?: string;
    onConfirm: () => void;
    onClose: () => void;
};

export default function ConfirmDialog({ open, title = "Are you sure?", message, confirmLabel = "Confirm", cancelLabel = "Cancel", onConfirm, onClose }: Props) {
    return (
        <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
            <DialogTitle>{title}</DialogTitle>
            {message && (
                <DialogContent>
                    <Typography variant="body2" color="text.secondary">{message}</Typography>
                </DialogContent>
            )}
            <DialogActions>
                <Button onClick={onClose}>{cancelLabel}</Button>
                <Button variant="contained" color="error" onClick={onConfirm}>{confirmLabel}</Button>
            </DialogActions>
        </Dialog>
    );
}
