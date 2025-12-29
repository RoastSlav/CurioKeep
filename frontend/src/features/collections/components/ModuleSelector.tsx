"use client"

import type {CollectionModule} from "../../../api/types"
import {Badge} from "../../../../components/ui/badge"
import {cn} from "../../../../lib/utils"

type Props = {
    modules: CollectionModule[]
    activeModuleKey: string | null
    onChange: (moduleKey: string) => void
    itemCounts?: Record<string, number>
}

export default function ModuleSelector({modules, activeModuleKey, onChange, itemCounts = {}}: Props) {
    if (!modules.length) {
        return <p className="text-sm text-text-secondary">No modules are enabled for this collection yet.</p>
    }

    const currentValue = activeModuleKey || modules[0].moduleKey

    return (
        <div className="flex items-center gap-3 flex-wrap border-4 border-border bg-card p-4 brutal-shadow-sm">
            <h3 className="text-base font-bold uppercase min-w-[120px]">Modules</h3>
            {modules.map((module) => {
                const isActive = currentValue === module.moduleKey
                const count = itemCounts[module.moduleKey] || 0

        return (
            <Badge
                key={module.moduleKey}
                variant={isActive ? "default" : "outline"}
                className={cn(
                    "cursor-pointer transition-colors text-sm px-4 py-2 border-2",
                    isActive && "bg-primary hover:bg-primary-dark text-white border-primary",
                    !isActive && "hover:bg-muted",
                )}
                onClick={() => onChange(module.moduleKey)}
            >
                <div className="flex items-center gap-2">
                    <span className="font-semibold">{module.name || module.moduleKey}</span>
                    {module.version && <span className="text-xs opacity-70">v{module.version}</span>}
                    <span className="font-bold">({count})</span>
                </div>
            </Badge>
        )
            })}
        </div>
    )
}
