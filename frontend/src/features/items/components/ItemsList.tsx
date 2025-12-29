"use client"

import {useState} from "react"
import type {Item, ModuleDefinition} from "../../../api/types"
import EmptyState from "../../../components/EmptyState"
import ErrorState from "../../../components/ErrorState"
import LoadingState from "../../../components/LoadingState"
import BatchActionsBar from "./BatchActionsBar"
import BatchDeleteDialog from "./BatchDeleteDialog"
import BatchSelection from "./BatchSelection"
import BatchStateDialog from "./BatchStateDialog"
import ItemRow from "./ItemRow"
import {Card, CardContent} from "../../../../components/ui/card"
import {Table, TableBody, TableHead, TableHeader, TableRow} from "../../../../components/ui/table"

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
    items: Item[]
    loading?: boolean
    error?: string | null
    moduleName?: string
    moduleDefinition?: ModuleDefinition | null
    canAdd?: boolean
    onAdd?: () => void
    onRetry?: () => void
    onItemClick?: (item: Item) => void
    role?: string
    onChangeState?: (item: Item, stateKey: string) => void
    selectedIds?: string[]
    onToggleItem?: (itemId: string, checked: boolean) => void
    onToggleAll?: (itemIds: string[]) => void
    onClearSelection?: () => void
    onBatchChangeState?: (stateKey: string) => Promise<void> | void
    onBatchDelete?: () => Promise<void> | void
    batchBusy?: boolean
}) {
    const upperRole = role?.toUpperCase()
    const canChangeState = upperRole === "OWNER" || upperRole === "ADMIN" || upperRole === "EDITOR"
    const selectionEnabled = Boolean(onToggleItem && onToggleAll && selectedIds)
    const selectedCount = selectedIds?.length ?? 0
    const allIds = items.map((i) => i.id)
    const [stateDialogOpen, setStateDialogOpen] = useState(false)
    const [deleteDialogOpen, setDeleteDialogOpen] = useState(false)

    const handleToggleAll = (checked: boolean) => {
        if (!selectionEnabled || !onToggleAll) return
        onToggleAll(checked ? allIds : [])
    }

    const handleOpenStateDialog = () => setStateDialogOpen(true)
    const handleOpenDeleteDialog = () => setDeleteDialogOpen(true)

    const handleStateConfirm = async (stateKey: string) => {
        await onBatchChangeState?.(stateKey)
        setStateDialogOpen(false)
    }

    const handleDeleteConfirm = async () => {
        await onBatchDelete?.()
        setDeleteDialogOpen(false)
    }

    if (loading) return <LoadingState message="Loading items..."/>
    if (error) return <ErrorState title="Could not load items" message={error} onRetry={onRetry}/>

    if (!items.length) {
        return (
            <EmptyState
                title={moduleName ? `No items in ${moduleName}` : "No items yet"}
                description="Items you add will show up here."
                actionLabel={canAdd ? "Add item" : undefined}
                onAction={canAdd ? onAdd : undefined}
            />
        )
    }

    return (
        <Card>
            <CardContent className="p-4 space-y-4">
                <div className="flex items-center justify-between">
                    <h3 className="text-lg font-bold uppercase">Items{moduleName ? ` · ${moduleName}` : ""}</h3>
                    {canAdd && (
                        <button onClick={onAdd} className="text-sm font-bold text-secondary hover:underline uppercase">
                            Add item
                        </button>
                    )}
                </div>

                {selectionEnabled && selectedCount > 0 ? (
                    <BatchActionsBar
                        selectedCount={selectedCount}
                        onClear={() => onClearSelection?.()}
                        onChangeState={handleOpenStateDialog}
                        onDelete={handleOpenDeleteDialog}
                        disabled={batchBusy}
                    />
                ) : null}

                <div className="border-2 border-border overflow-x-auto">
                    <Table>
                        <TableHeader>
                            <TableRow>
                                {selectionEnabled ? (
                                    <TableHead className="w-10">
                                        <BatchSelection total={items.length} selected={selectedCount}
                                                        onToggleAll={handleToggleAll}/>
                                    </TableHead>
                                ) : null}
                                <TableHead className="font-bold uppercase">Title</TableHead>
                                <TableHead className="font-bold uppercase">State</TableHead>
                                {moduleDefinition?.fields?.length ? (
                                    <TableHead className="font-bold uppercase">Fields</TableHead>
                                ) : null}
                                <TableHead className="font-bold uppercase">Identifier</TableHead>
                            </TableRow>
                        </TableHeader>
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
                </div>

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
            </CardContent>
        </Card>
    )
}
