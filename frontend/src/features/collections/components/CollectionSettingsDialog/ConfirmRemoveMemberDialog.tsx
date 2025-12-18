import { Button, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle, Typography } from "@mui/material";

type Props = {
    open: boolean;
    memberName?: string;
    onCancel: () => void;
    onConfirm: () => void;
};

export default function ConfirmRemoveMemberDialog({ open, memberName, onCancel, onConfirm }: Props) {
    return (
        <Dialog open={open} onClose={onCancel} fullWidth maxWidth="xs">
            <DialogTitle>Remove member?</DialogTitle>
            <DialogContent>
                <DialogContentText>
                    {memberName ? (
                        <Typography component="span">
                            Remove <strong>{memberName}</strong> from this collection?
                        </Typography>
                    ) : (
                        "Remove this member from the collection?"
                    )}
                </DialogContentText>
            </DialogContent>
            <DialogActions>
                <Button onClick={onCancel}>Cancel</Button>
                <Button color="error" variant="contained" onClick={onConfirm}>
                    Remove
                </Button>
            </DialogActions>
        </Dialog>
    );
}
