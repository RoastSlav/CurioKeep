import {
    Box,
    Paper,
    Stack,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TablePagination,
    TableRow,
    Typography,
} from "@mui/material";
import type { Item, ModuleDefinition } from "../../../api/types";
import EmptyState from "../../../components/EmptyState";
import ErrorState from "../../../components/ErrorState";
import LoadingState from "../../../components/LoadingState";
import type { ItemSort } from "../api";
import type { FieldFilter } from "../hooks/useItemsQuery";
import FiltersPanel from "./FiltersPanel";
import ItemRow from "./ItemRow";
import ItemsToolbar from "./ItemsToolbar";

export default function ItemsTable({
    moduleDefinition,
    items,
    total,
    page,
    size,
    search,
    states,
    sort,
    filters,
    loading,
    error,
    showFilters,
    canAdd,
    moduleName,
    onSearchChange,
    onStatesChange,
    onSortChange,
    onFiltersChange,
    onToggleFilters,
    onPageChange,
    onSizeChange,
    onRetry,
    onAdd,
}: {
    moduleDefinition: ModuleDefinition | null | undefined;
    items: Item[];
    total: number;
    page: number;
    size: number;
    search: string;
    states: string[];
    sort: ItemSort;
    filters?: Record<string, FieldFilter>;
    loading?: boolean;
    error?: string | null;
    showFilters: boolean;
    canAdd?: boolean;
    moduleName?: string;
    onSearchChange: (value: string) => void;
    onStatesChange: (next: string[]) => void;
    onSortChange: (next: ItemSort) => void;
    onFiltersChange: (next: Record<string, FieldFilter> | undefined) => void;
    onToggleFilters: () => void;
    onPageChange: (page: number) => void;
    onSizeChange: (size: number) => void;
    onRetry?: () => void;
    onAdd?: () => void;
}) {
    if (loading && !items.length) return <LoadingState message="Loading items..." />;
    if (error) return <ErrorState title="Could not load items" message={error} onRetry={onRetry} />;

    const noResults = !items.length && (Boolean(search) || Boolean(states.length) || Boolean(filters));
    const showEmpty = !items.length && !search && !states.length && !filters && !loading;

    return (
        <Stack spacing={2}>
            <Paper variant="outlined" sx={{ p: 2 }}>
                <ItemsToolbar
                    moduleDefinition={moduleDefinition}
                    search={search}
                    onSearchChange={onSearchChange}
                    states={states}
                    onStatesChange={onStatesChange}
                    sort={sort}
                    onSortChange={onSortChange}
                    showFilters={showFilters}
                    onToggleFilters={onToggleFilters}
                />
                {showFilters && (
                    <Box mt={2}>
                        <FiltersPanel
                            moduleDefinition={moduleDefinition}
                            filters={filters}
                            onChange={onFiltersChange}
                            onClear={() => onFiltersChange(undefined)}
                        />
                    </Box>
                )}
            </Paper>

            {showEmpty ? (
                <EmptyState
                    title={moduleName ? `No items in ${moduleName}` : "No items yet"}
                    description="Items you add will show up here."
                    actionLabel={canAdd ? "Add item" : undefined}
                    onAction={canAdd ? onAdd : undefined}
                />
            ) : noResults ? (
                <EmptyState
                    title="No matching items"
                    description="Try adjusting search, states, or filters."
                    actionLabel={canAdd ? "Add item" : undefined}
                    onAction={canAdd ? onAdd : undefined}
                />
            ) : (
                <Paper variant="outlined">
                    <Stack spacing={1.5} p={2}>
                        <Stack direction="row" justifyContent="space-between" alignItems="center">
                            <Typography variant="h6" fontWeight={700}>
                                Items{moduleName ? ` · ${moduleName}` : ""}
                            </Typography>
                            {canAdd && (
                                <Typography variant="body2" color="primary" sx={{ cursor: "pointer" }} onClick={onAdd}>
                                    Add item
                                </Typography>
                            )}
                        </Stack>
                        <Box sx={{ overflowX: "auto" }}>
                            <Table size="small">
                                <TableHead>
                                    <TableRow>
                                        <TableCell>ID</TableCell>
                                        <TableCell>State</TableCell>
                                        {moduleDefinition?.fields?.length ? <TableCell>Fields</TableCell> : null}
                                        <TableCell>Created</TableCell>
                                        <TableCell>Updated</TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {items.map((item) => (
                                        <ItemRow key={item.id} item={item} moduleDefinition={moduleDefinition} />
                                    ))}
                                </TableBody>
                            </Table>
                        </Box>
                        <TablePagination
                            component="div"
                            count={total}
                            page={page}
                            onPageChange={(_, nextPage) => onPageChange(nextPage)}
                            rowsPerPage={size}
                            onRowsPerPageChange={(e) => onSizeChange(parseInt(e.target.value, 10))}
                            rowsPerPageOptions={[10, 20, 50]}
                        />
                    </Stack>
                </Paper>
            )}
        </Stack>
    );
}
