import { Box, IconButton, MenuItem, Stack, TextField, Tooltip } from "@mui/material";
import ArrowDownwardIcon from "@mui/icons-material/ArrowDownward";
import ArrowUpwardIcon from "@mui/icons-material/ArrowUpward";
import type { ItemSort } from "../api";

export default function SortControl({
    value,
    options,
    onChange,
}: {
    value: ItemSort;
    options: { value: string; label: string }[];
    onChange: (sort: ItemSort) => void;
}) {
    const toggleDirection = () => {
        const nextDirection = value.direction === "asc" ? "desc" : "asc";
        onChange({ ...value, direction: nextDirection });
    };

    return (
        <Stack direction="row" spacing={1} alignItems="center">
            <Box width={180}>
                <TextField
                    select
                    fullWidth
                    label="Sort by"
                    size="small"
                    value={value.field}
                    onChange={(e) => onChange({ ...value, field: e.target.value })}
                >
                    {options.map((option) => (
                        <MenuItem key={option.value} value={option.value}>
                            {option.label}
                        </MenuItem>
                    ))}
                </TextField>
            </Box>
            <Tooltip title={value.direction === "asc" ? "Ascending" : "Descending"}>
                <IconButton color="primary" size="small" onClick={toggleDirection}>
                    {value.direction === "asc" ? <ArrowUpwardIcon /> : <ArrowDownwardIcon />}
                </IconButton>
            </Tooltip>
        </Stack>
    );
}
