import { Button, Paper, Stack, Typography } from "@mui/material";
import DeleteIcon from "@mui/icons-material/Delete";
import SwapHorizIcon from "@mui/icons-material/SwapHoriz";
import ClearIcon from "@mui/icons-material/Clear";

export default function BatchActionsBar({
    selectedCount,
    onClear,
    onChangeState,
    onDelete,
    disabled,
}: {
    selectedCount: number;
    onClear: () => void;
    onChangeState: () => void;
    onDelete: () => void;
    disabled?: boolean;
}) {
    return (
        <Paper variant="outlined" sx={{ p: 1, borderStyle: "dashed" }}>
            <Stack direction="row" justifyContent="space-between" alignItems="center" spacing={2}>
                <Stack direction="row" spacing={1.5} alignItems="center">
                    <Typography fontWeight={700}>{selectedCount} selected</Typography>
                    <Button size="small" startIcon={<ClearIcon />} onClick={onClear} disabled={disabled}>
                        Clear
                    </Button>
                </Stack>
                <Stack direction="row" spacing={1}>
                    <Button
                        size="small"
                        variant="outlined"
                        startIcon={<SwapHorizIcon />}
                        onClick={onChangeState}
                        disabled={disabled}
                    >
                        Change state
                    </Button>
                    <Button
                        size="small"
                        color="error"
                        variant="contained"
                        startIcon={<DeleteIcon />}
                        onClick={onDelete}
                        disabled={disabled}
                    >
                        Delete
                    </Button>
                </Stack>
            </Stack>
        </Paper>
    );
}
