"use client"

import {Trash2, Edit} from "lucide-react"
import type {ModuleStateDef} from "../../../api/types"
import StateDropdown from "./StateDropdown"
import {Button} from "../../../../components/ui/button"
import {Tooltip, TooltipContent, TooltipProvider, TooltipTrigger} from "../../../../components/ui/tooltip"

export default function ItemActions({
                                        role,
                                        stateKey,
                                        states,
                                        onChangeState,
                                        onEdit,
                                        onDelete,
                                        compact,
                                        disabled,
}: {
    role?: string
    stateKey: string
    states: ModuleStateDef[]
    onChangeState?: (stateKey: string) => void
    onEdit?: () => void
    onDelete?: () => void
    compact?: boolean
    disabled?: boolean
}) {
    const upperRole = role?.toUpperCase()
    const canEdit = upperRole === "OWNER" || upperRole === "ADMIN" || upperRole === "EDITOR"
    const canDelete = upperRole === "OWNER" || upperRole === "ADMIN"

    return (
        <div className="flex items-center gap-2">
            {onChangeState && states.length ? (
                <StateDropdown states={states} value={stateKey} onChange={onChangeState}
                               disabled={!canEdit || disabled}/>
            ) : null}
            {canEdit && onEdit && (
                <TooltipProvider>
                    <Tooltip>
                        <TooltipTrigger asChild>
                            {compact ? (
                                <Button size="icon-sm" variant="ghost" onClick={onEdit} disabled={disabled}>
                                    <Edit className="w-4 h-4"/>
                                </Button>
                            ) : (
                                <Button variant="outline" onClick={onEdit} disabled={disabled}>
                                    <Edit className="w-4 h-4 mr-2"/>
                                    Edit
                                </Button>
                            )}
                        </TooltipTrigger>
                        <TooltipContent>Edit</TooltipContent>
                    </Tooltip>
                </TooltipProvider>
            )}
            {canDelete && onDelete && (
                <TooltipProvider>
                    <Tooltip>
                        <TooltipTrigger asChild>
                            {compact ? (
                                <Button
                                    size="icon-sm"
                                    variant="ghost"
                                    className="text-destructive hover:text-destructive"
                                    onClick={onDelete}
                                    disabled={disabled}
                                >
                                    <Trash2 className="w-4 h-4"/>
                                </Button>
                            ) : (
                                <Button
                                    variant="outline"
                                    className="text-destructive bg-transparent"
                                    onClick={onDelete}
                                    disabled={disabled}
                                >
                                    <Trash2 className="w-4 h-4 mr-2"/>
                                    Delete
                                </Button>
                            )}
                        </TooltipTrigger>
                        <TooltipContent>Delete</TooltipContent>
                    </Tooltip>
                </TooltipProvider>
            )}
        </div>
    )
}
