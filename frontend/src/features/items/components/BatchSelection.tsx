import { Checkbox, Stack, Typography } from "@mui/material";

export default function BatchSelection({ total, selected, onToggleAll }: { total: number; selected: number; onToggleAll: (checked: boolean) => void }) {
    const allChecked = total > 0 && selected === total;
    const indeterminate = selected > 0 && selected < total;

    return (
        <Stack direction="row" spacing={1} alignItems="center">
            <Checkbox
                size="small"
                checked={allChecked}
                indeterminate={indeterminate}
                onChange={(e) => onToggleAll(e.target.checked)}
                inputProps={{ "aria-label": "Select all items on page" }}
            />
            <Typography variant="body2" color="text.secondary">
                {selected ? `${selected} selected` : "Select all"}
            </Typography>
        </Stack>
    );
}
