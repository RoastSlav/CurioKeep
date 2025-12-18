import React from 'react';
import { Dialog, DialogTitle, DialogContent, DialogActions, Button, Typography } from '@mui/material';

export default function ConfirmRemoveMemberDialog({ open, onClose, onConfirm, memberName }:
  { open: boolean; onClose: () => void; onConfirm: () => void; memberName: string }) {
  return (
    <Dialog open={open} onClose={onClose}>
      <DialogTitle>Remove member</DialogTitle>
      <DialogContent>
        <Typography>Remove {memberName} from the collection? This cannot be undone.</Typography>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button color="error" onClick={() => { onConfirm(); onClose(); }}>Remove</Button>
      </DialogActions>
    </Dialog>
  );
}
