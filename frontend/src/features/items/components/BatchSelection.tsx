"use client"

import {Checkbox} from "../../../../components/ui/checkbox"

export default function BatchSelection({
                                           total,
                                           selected,
                                           onToggleAll,
                                       }: {
    total: number
    selected: number
    onToggleAll: (checked: boolean) => void
}) {
    const allChecked = total > 0 && selected === total
    const indeterminate = selected > 0 && selected < total

    return (
        <Checkbox
            checked={indeterminate ? "indeterminate" : allChecked}
            onCheckedChange={(checked) => onToggleAll(Boolean(checked))}
            aria-label="Select all items on page"
        />
    )
}
