import { Button, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle } from "@mui/material";

type Props = {
    open: boolean;
    moduleName?: string;
    onCancel: () => void;
    onConfirm: () => void;
};

export default function DisableModuleConfirmDialog({ open, moduleName, onCancel, onConfirm }: Props) {
    return (
        <Dialog open={open} onClose={onCancel} fullWidth maxWidth="xs">
            <DialogTitle>Disable module?</DialogTitle>
            <DialogContent>
                <DialogContentText>
                    {moduleName
                        ? `Items using the ${moduleName} module will no longer be available until it is re-enabled.`
                        : "Are you sure you want to disable this module?"}
                </DialogContentText>
            </DialogContent>
            <DialogActions>
                <Button onClick={onCancel}>Cancel</Button>
                <Button color="error" variant="contained" onClick={onConfirm}>
                    Disable
                </Button>
            </DialogActions>
        </Dialog>
    );
}
