import { Button, Dialog, DialogActions, DialogContent, DialogTitle, Typography } from "@mui/material";

export default function BatchDeleteDialog({
    open,
    count,
    onClose,
    onConfirm,
}: {
    open: boolean;
    count: number;
    onClose: () => void;
    onConfirm: () => void;
}) {
    return (
        <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
            <DialogTitle>Delete {count} item{count === 1 ? "" : "s"}?</DialogTitle>
            <DialogContent>
                <Typography>Batch delete will remove the selected item(s). This cannot be undone.</Typography>
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose}>Cancel</Button>
                <Button color="error" variant="contained" onClick={onConfirm}>
                    Delete
                </Button>
            </DialogActions>
        </Dialog>
    );
}
