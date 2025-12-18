import { Box, Paper, Stack, Table, TableBody, TableCell, TableHead, TableRow, Typography } from "@mui/material";
import { useState } from "react";
import type { Item, ModuleDefinition } from "../../../api/types";
import EmptyState from "../../../components/EmptyState";
import ErrorState from "../../../components/ErrorState";
import LoadingState from "../../../components/LoadingState";
import BatchActionsBar from "./BatchActionsBar";
import BatchDeleteDialog from "./BatchDeleteDialog";
import BatchSelection from "./BatchSelection";
import BatchStateDialog from "./BatchStateDialog";
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
    selectedIds,
    onToggleItem,
    onToggleAll,
    onClearSelection,
    onBatchChangeState,
    onBatchDelete,
    batchBusy,
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
    selectedIds?: string[];
    onToggleItem?: (itemId: string, checked: boolean) => void;
    onToggleAll?: (itemIds: string[]) => void;
    onClearSelection?: () => void;
    onBatchChangeState?: (stateKey: string) => Promise<void> | void;
    onBatchDelete?: () => Promise<void> | void;
    batchBusy?: boolean;
}) {
    const upperRole = role?.toUpperCase();
    const canChangeState = upperRole === "OWNER" || upperRole === "ADMIN" || upperRole === "EDITOR";
    const selectionEnabled = Boolean(onToggleItem && onToggleAll && selectedIds);
    const selectedCount = selectedIds?.length ?? 0;
    const allIds = items.map((i) => i.id);
    const [stateDialogOpen, setStateDialogOpen] = useState(false);
    const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);

    const handleToggleAll = (checked: boolean) => {
        if (!selectionEnabled || !onToggleAll) return;
        onToggleAll(checked ? allIds : []);
    };

    const handleOpenStateDialog = () => setStateDialogOpen(true);
    const handleOpenDeleteDialog = () => setDeleteDialogOpen(true);

    const handleStateConfirm = async (stateKey: string) => {
        await onBatchChangeState?.(stateKey);
        setStateDialogOpen(false);
    };

    const handleDeleteConfirm = async () => {
        await onBatchDelete?.();
        setDeleteDialogOpen(false);
    };
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
                {selectionEnabled && selectedCount > 0 ? (
                    <BatchActionsBar
                        selectedCount={selectedCount}
                        onClear={() => onClearSelection?.()}
                        onChangeState={handleOpenStateDialog}
                        onDelete={handleOpenDeleteDialog}
                        disabled={batchBusy}
                    />
                ) : null}

                <Box sx={{ overflowX: "auto" }}>
                    <Table size="small">
                        <TableHead>
                            <TableRow>
                                {selectionEnabled ? (
                                    <TableCell padding="checkbox">
                                        <BatchSelection
                                            total={items.length}
                                            selected={selectedCount}
                                            onToggleAll={handleToggleAll}
                                        />
                                    </TableCell>
                                ) : null}
                                <TableCell>Title</TableCell>
                                <TableCell>State</TableCell>
                                {moduleDefinition?.fields?.length ? <TableCell>Fields</TableCell> : null}
                                <TableCell>Identifier</TableCell>
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
                                        showSelection={selectionEnabled}
                                        selected={selectedIds?.includes(item.id)}
                                        onToggleSelect={(itemId, checked) => onToggleItem?.(itemId, checked)}
                                    />
                            ))}
                        </TableBody>
                    </Table>
                </Box>
            </Stack>

            {selectionEnabled ? (
                <>
                    <BatchStateDialog
                        open={stateDialogOpen}
                        states={moduleDefinition?.states}
                        onClose={() => setStateDialogOpen(false)}
                        onConfirm={handleStateConfirm}
                    />
                    <BatchDeleteDialog
                        open={deleteDialogOpen}
                        count={selectedCount}
                        onClose={() => setDeleteDialogOpen(false)}
                        onConfirm={handleDeleteConfirm}
                    />
                </>
            ) : null}
        </Paper>
    );
}
