import {
    Alert,
    Box,
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogContentText,
    DialogTitle,
    Stack,
    Typography,
} from "@mui/material";
import { useCallback, useEffect, useState } from "react";
import { isApiError } from "../../../api/errors";
import { deleteImportedModule } from "../api/modulesApi";

type Props = {
    open: boolean;
    moduleKey?: string;
    moduleName?: string;
    onClose: () => void;
    onModuleDeleted: () => void;
};

export default function DeleteModuleDialog({ open, moduleKey, moduleName, onClose, onModuleDeleted }: Props) {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        if (!open) {
            setLoading(false);
            setError(null);
        }
    }, [open]);

    const handleConfirm = useCallback(async () => {
        if (!moduleKey) return;
        setLoading(true);
        setError(null);
        try {
            await deleteImportedModule(moduleKey);
            onModuleDeleted();
            onClose();
        } catch (err) {
            const message = isApiError(err) ? err.message : "Failed to delete module";
            setError(message);
        } finally {
            setLoading(false);
        }
    }, [moduleKey, onClose, onModuleDeleted]);

    const title = moduleName ? `Delete ${moduleName}` : "Delete module";

    return (
        <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
            <DialogTitle>{title}</DialogTitle>
            <DialogContent dividers>
                <Stack spacing={1}>
                    <Typography variant="body2" color="text.secondary">
                        Deleting a module may break collections/items that depend on it.
                    </Typography>
                    <DialogContentText>
                        This action cannot be undone. The module definition and its imported XML will be removed from the system.
                    </DialogContentText>
                    {error && <Alert severity="error">{error}</Alert>}
                    <Box>
                        <Typography variant="caption" color="text.secondary">
                            {moduleKey}
                        </Typography>
                    </Box>
                </Stack>
            </DialogContent>
            <DialogActions sx={{ px: 3, py: 2 }}>
                <Button variant="outlined" onClick={onClose} disabled={loading}>
                    Cancel
                </Button>
                <Button variant="contained" color="error" onClick={handleConfirm} disabled={loading || !moduleKey}>
                    {loading ? "Deleting..." : "Delete module"}
                </Button>
            </DialogActions>
        </Dialog>
    );
}
