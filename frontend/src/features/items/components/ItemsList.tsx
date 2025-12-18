import { Box, Paper, Stack, Table, TableBody, TableCell, TableHead, TableRow, Typography } from "@mui/material";
import type { Item, ModuleDefinition } from "../../../api/types";
import EmptyState from "../../../components/EmptyState";
import ErrorState from "../../../components/ErrorState";
import LoadingState from "../../../components/LoadingState";
import ItemRow from "./ItemRow";

export default function ItemsList({
    items,
    loading,
    error,
    moduleName,
    moduleDefinition,
    canAdd,
    onAdd,
    onRetry,
    onItemClick,
    role,
    onChangeState,
}: {
    items: Item[];
    loading?: boolean;
    error?: string | null;
    moduleName?: string;
    moduleDefinition?: ModuleDefinition | null;
    canAdd?: boolean;
    onAdd?: () => void;
    onRetry?: () => void;
    onItemClick?: (item: Item) => void;
    role?: string;
    onChangeState?: (item: Item, stateKey: string) => void;
}) {
    const upperRole = role?.toUpperCase();
    const canChangeState = upperRole === "OWNER" || upperRole === "ADMIN" || upperRole === "EDITOR";
    if (loading) return <LoadingState message="Loading items..." />;
    if (error) return <ErrorState title="Could not load items" message={error} onRetry={onRetry} />;

    if (!items.length) {
        return (
            <EmptyState
                title={moduleName ? `No items in ${moduleName}` : "No items yet"}
                description="Items you add will show up here."
                actionLabel={canAdd ? "Add item" : undefined}
                onAction={canAdd ? onAdd : undefined}
            />
        );
    }

    return (
        <Paper variant="outlined">
            <Stack spacing={1.5} p={2}>
                <Stack direction="row" justifyContent="space-between" alignItems="center">
                    <Typography variant="h6" fontWeight={700}>
                        Items{moduleName ? ` · ${moduleName}` : ""}
                    </Typography>
                    {canAdd && (
                        <Typography
                            variant="body2"
                            color="primary"
                            sx={{ cursor: "pointer" }}
                            onClick={onAdd}
                        >
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
                                <ItemRow
                                    key={item.id}
                                    item={item}
                                    moduleDefinition={moduleDefinition}
                                    onClick={onItemClick}
                                    states={moduleDefinition?.states}
                                    onChangeState={onChangeState}
                                    canChangeState={canChangeState}
                                />
                            ))}
                        </TableBody>
                    </Table>
                </Box>
            </Stack>
        </Paper>
    );
}
