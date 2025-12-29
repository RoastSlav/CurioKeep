"use client"

import {Trash2, ArrowLeftRight, X} from "lucide-react"
import {Button} from "../../../../components/ui/button"

export default function BatchActionsBar({
                                            selectedCount,
                                            onClear,
                                            onChangeState,
                                            onDelete,
                                            disabled,
}: {
    selectedCount: number
    onClear: () => void
    onChangeState: () => void
    onDelete: () => void
    disabled?: boolean
}) {
    return (
        <div className="border-2 border-dashed border-border p-3 bg-muted">
            <div className="flex flex-wrap items-center justify-between gap-3">
                <div className="flex items-center gap-3">
                    <span className="font-bold">{selectedCount} selected</span>
                    <Button size="sm" variant="ghost" onClick={onClear} disabled={disabled}>
                        <X className="w-4 h-4 mr-1"/>
                        Clear
                    </Button>
                </div>
                <div className="flex items-center gap-2">
                    <Button size="sm" variant="outline" onClick={onChangeState} disabled={disabled}>
                        <ArrowLeftRight className="w-4 h-4 mr-1"/>
                        Change state
                    </Button>
                    <Button size="sm" variant="destructive" onClick={onDelete} disabled={disabled}>
                        <Trash2 className="w-4 h-4 mr-1"/>
                        Delete
                    </Button>
                </div>
            </div>
        </div>
    )
}
