"use client"

import type {Item, ModuleStateDef} from "../../../api/types"
import {Card, CardContent} from "../../../../components/ui/card"
import {Badge} from "../../../../components/ui/badge"
import {cn} from "../../../../lib/utils"

export default function StatsPanel({
                                       items,
                                       states,
                                       activeState,
                                       onFilterChange,
}: {
    items: Item[]
    states?: ModuleStateDef[]
    activeState?: string | null
    onFilterChange?: (stateKey: string | null) => void
}) {
    const counts = items.reduce<Record<string, number>>((acc, item) => {
        acc[item.stateKey] = (acc[item.stateKey] || 0) + 1
        return acc
    }, {})

    const total = items.length
    const orderedStates = states && states.length ? states : Object.keys(counts).map((key) => ({key}) as ModuleStateDef)

    const handleSelect = (stateKey: string | null) => {
        if (!onFilterChange) return
        onFilterChange(stateKey === activeState ? null : stateKey)
    }

    return (
        <Card className="border-border">
            <CardContent className="p-4">
                <div className="flex items-center gap-3 flex-wrap">
                    <h3 className="text-base font-bold min-w-[120px]">Stats</h3>
                    <Badge
                        variant={activeState ? "outline" : "default"}
                        className={cn(
                            "cursor-pointer transition-colors",
                            !activeState && "bg-primary hover:bg-primary-dark text-white",
                        )}
                        onClick={() => handleSelect(null)}
                    >
                        Total: {total}
                    </Badge>
                    {orderedStates.map((state) => {
                        const selected = activeState === state.key
                        return (
                            <Badge
                                key={state.key}
                                variant={selected ? "default" : "outline"}
                                className={cn(
                                    "cursor-pointer transition-colors",
                                    selected && "bg-primary hover:bg-primary-dark text-white",
                                )}
                                onClick={() => handleSelect(state.key)}
                            >
                                {state.label || state.key}: {counts[state.key] || 0}
                            </Badge>
                        )
                    })}
                </div>
            </CardContent>
        </Card>
    )
}
