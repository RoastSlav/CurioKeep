import FilterListIcon from "@mui/icons-material/FilterList";
import SearchIcon from "@mui/icons-material/Search";
import { Box, Divider, IconButton, InputAdornment, Stack, TextField, Tooltip } from "@mui/material";
import type { ModuleDefinition } from "../../../api/types";
import type { ItemSort } from "../api";
import StateChips from "./StateChips";
import SortControl from "./SortControl";

export default function ItemsToolbar({
    moduleDefinition,
    search,
    onSearchChange,
    states,
    onStatesChange,
    sort,
    onSortChange,
    showFilters,
    onToggleFilters,
}: {
    moduleDefinition: ModuleDefinition | null | undefined;
    search: string;
    onSearchChange: (value: string) => void;
    states: string[];
    onStatesChange: (next: string[]) => void;
    sort: ItemSort;
    onSortChange: (next: ItemSort) => void;
    showFilters: boolean;
    onToggleFilters: () => void;
}) {
    const sortOptions = [
        { value: "createdAt", label: "Created" },
        { value: "updatedAt", label: "Updated" },
        ...(moduleDefinition?.fields || [])
            .filter((f) => f.flags?.sortable)
            .map((f) => ({ value: f.key, label: f.label || f.key })),
    ];

    return (
        <Stack spacing={2} divider={<Divider flexItem />}>
            <Stack direction="row" spacing={2} alignItems="center" justifyContent="space-between">
                <Box flex={1}>
                    <TextField
                        fullWidth
                        size="small"
                        placeholder="Search items"
                        value={search}
                        onChange={(e) => onSearchChange(e.target.value)}
                        InputProps={{
                            startAdornment: (
                                <InputAdornment position="start">
                                    <SearchIcon fontSize="small" />
                                </InputAdornment>
                            ),
                        }}
                    />
                </Box>
                <Tooltip title={showFilters ? "Hide filters" : "Show filters"}>
                    <IconButton onClick={onToggleFilters} color={showFilters ? "primary" : "default"}>
                        <FilterListIcon />
                    </IconButton>
                </Tooltip>
            </Stack>
            <Stack direction={{ xs: "column", sm: "row" }} spacing={2} alignItems={{ xs: "flex-start", sm: "center" }}>
                <StateChips states={moduleDefinition?.states || []} selected={states} onChange={onStatesChange} />
                <Box flexGrow={1} />
                <SortControl value={sort} options={sortOptions} onChange={onSortChange} />
            </Stack>
        </Stack>
    );
}
