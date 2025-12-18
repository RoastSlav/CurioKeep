import { Button, Dialog, DialogActions, DialogContent, DialogTitle, FormControl, InputLabel, MenuItem, Select, Stack } from "@mui/material";
import { useState } from "react";
import type { ModuleStateDef } from "../../../api/types";

export default function BatchStateDialog({
    open,
    states,
    onClose,
    onConfirm,
}: {
    open: boolean;
    states?: ModuleStateDef[];
    onClose: () => void;
    onConfirm: (stateKey: string) => void;
}) {
    const [stateKey, setStateKey] = useState<string>(states?.[0]?.key || "");

    const handleConfirm = () => {
        if (stateKey) onConfirm(stateKey);
    };

    return (
        <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
            <DialogTitle>Change state for selected</DialogTitle>
            <DialogContent>
                <Stack spacing={2} mt={1}>
                    <FormControl fullWidth>
                        <InputLabel id="batch-state-select-label">State</InputLabel>
                        <Select
                            labelId="batch-state-select-label"
                            value={stateKey}
                            label="State"
                            onChange={(e) => setStateKey(e.target.value)}
                        >
                            {states?.map((s) => (
                                <MenuItem key={s.key} value={s.key}>
                                    {s.label || s.key}
                                </MenuItem>
                            ))}
                        </Select>
                    </FormControl>
                </Stack>
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose}>Cancel</Button>
                <Button onClick={handleConfirm} disabled={!stateKey} variant="contained">
                    Apply
                </Button>
            </DialogActions>
        </Dialog>
    );
}
