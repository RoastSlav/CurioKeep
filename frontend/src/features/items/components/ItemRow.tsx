"use client"

import type {Item, ModuleDefinition, ModuleStateDef} from "../../../api/types"
import StateDropdown from "./StateDropdown"
import {TableCell, TableRow} from "../../../../components/ui/table"
import {Checkbox} from "../../../../components/ui/checkbox"
import {Badge} from "../../../../components/ui/badge"

function formatValue(value: unknown): string {
    if (value === null || value === undefined) return ""
    if (Array.isArray(value)) return value.join(", ")
    if (typeof value === "object") return JSON.stringify(value)
    return String(value)
}

export default function ItemRow({
                                    item,
                                    moduleDefinition,
                                    onClick,
                                    states,
                                    onChangeState,
                                    canChangeState,
                                    showSelection,
                                    selected,
                                    onToggleSelect,
}: {
    item: Item
    moduleDefinition?: ModuleDefinition | null
    onClick?: (item: Item) => void
    states?: ModuleStateDef[]
    onChangeState?: (item: Item, next: string) => void
    canChangeState?: boolean
    showSelection?: boolean
    selected?: boolean
    onToggleSelect?: (itemId: string, checked: boolean) => void
}) {
    const identifierField = (moduleDefinition?.fields || []).find((field) => {
        const val = formatValue((item.attributes || {})[field.key])
        return field.identifiers && field.identifiers.length > 0 && val.trim().length > 0
    })
    const identifierDisplay = identifierField
        ? `${identifierField.identifiers?.[0] || identifierField.label || identifierField.key}: ${formatValue(
            (item.attributes || {})[identifierField.key],
        )}`
        : null
    const imageUrl = item.attributes?.providerImageUrl as string | undefined
    const displayTitle =
        (item.attributes?.title as string) || (item.attributes?.name as string) || identifierDisplay || item.id
    const fieldsToShow = (moduleDefinition?.fields || [])
        .slice()
        .sort((a, b) => (a.flags?.order ?? Number.MAX_SAFE_INTEGER) - (b.flags?.order ?? Number.MAX_SAFE_INTEGER))
        .slice(0, 3)
    const stateLabel = moduleDefinition?.states?.find((s) => s.key === item.stateKey)?.label

    return (
        <TableRow
            className={onClick ? "cursor-pointer hover:bg-muted" : ""}
            onClick={onClick ? () => onClick(item) : undefined}
        >
            {showSelection ? (
                <TableCell className="w-10">
                    <Checkbox
                        checked={Boolean(selected)}
                        onCheckedChange={(checked) => onToggleSelect?.(item.id, Boolean(checked))}
                        onClick={(e) => e.stopPropagation()}
                    />
                </TableCell>
            ) : null}
            <TableCell>
                <div className="flex items-center gap-3">
                    {imageUrl ? (
                        <img
                            src={imageUrl || "/placeholder.svg"}
                            alt={displayTitle}
                            className="w-16 h-16 object-contain border-2 border-border"
                            onClick={(e) => e.stopPropagation()}
                        />
                    ) : null}
                    <div className="min-w-0 space-y-0.5">
                        <p className="font-bold truncate">{displayTitle}</p>
                        {identifierDisplay ?
                            <p className="text-xs text-muted-foreground truncate">{identifierDisplay}</p> : null}
                    </div>
                </div>
            </TableCell>
            <TableCell>
                {canChangeState && states?.length ? (
                    <StateDropdown states={states} value={item.stateKey}
                                   onChange={(next) => onChangeState?.(item, next)}/>
                ) : (
                    <Badge variant="secondary">{stateLabel || item.stateKey}</Badge>
                )}
            </TableCell>
            {fieldsToShow.length ? (
                <TableCell className="max-w-[420px]">
                    <div className="space-y-0.5">
                        {fieldsToShow.map((field) => (
                            <div key={field.key} className="flex items-baseline gap-1.5">
                                <span
                                    className="text-xs text-muted-foreground truncate">{field.label || field.key}:</span>
                                <span
                                    className="text-sm truncate">{formatValue((item.attributes || {})[field.key]) || "-"}</span>
                            </div>
                        ))}
                    </div>
                </TableCell>
            ) : null}
            <TableCell className="whitespace-nowrap">
                <span className="text-sm text-muted-foreground truncate">{identifierDisplay || "-"}</span>
            </TableCell>
        </TableRow>
    )
}
