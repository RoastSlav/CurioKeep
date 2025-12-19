"use client"

import {useMemo, useState} from "react"
import type {ModuleDetails, ModuleSummary} from "../api/modulesApi"
import {Input} from "../../../../components/ui/input"
import {Badge} from "../../../../components/ui/badge"
import {cn} from "../../../../lib/utils"

type Props = {
    modules: ModuleSummary[]
    details: Record<string, ModuleDetails>
    selectedKey?: string
    onSelect: (moduleKey: string) => void
    onViewXml: (moduleKey: string) => void
}

const sourceColors: Record<ModuleSummary["source"], string> = {
    BUILTIN: "bg-primary text-primary-foreground brutal-border",
    IMPORTED: "bg-secondary text-secondary-foreground brutal-border",
    USER: "bg-accent text-accent-foreground brutal-border",
}

export default function ModuleList({ modules, details, selectedKey, onSelect, onViewXml }: Props) {
    const [search, setSearch] = useState("")

    const filtered = useMemo(() => {
        const term = search.trim().toLowerCase()
        if (!term) return modules
        return modules.filter((module) => {
            const target = `${module.name} ${module.moduleKey} ${module.version}`.toLowerCase()
            if (target.includes(term)) return true
            const tags = details[module.moduleKey]?.contract.meta?.tags ?? []
            return tags.some((tag) => tag.toLowerCase().includes(term))
        })
    }, [modules, search, details])

    const formatUpdated = (value?: string) => {
        if (!value) return null
        const date = new Date(value)
        return date.toLocaleString(undefined, {dateStyle: "medium", timeStyle: "short"})
    }

    return (
        <div className="flex flex-col gap-3 w-full">
            <Input
                placeholder="Filter by name, key, or tag"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="brutal-border shadow-sm bg-background text-foreground"
            />
            <nav className="flex flex-col gap-2">
                {filtered.map((module) => {
                    const updatedLabel = formatUpdated(module.updatedAt)
                    const tags = details[module.moduleKey]?.contract.meta?.tags ?? []
                    const isSelected = module.moduleKey === selectedKey
                    return (
                        <div key={module.moduleKey}>
                            <div
                                onClick={() => onSelect(module.moduleKey)}
                                className={cn(
                                    "w-full p-4 transition-all duration-200 flex flex-col gap-3 cursor-pointer",
                                    "brutal-border brutal-shadow-sm",
                                    isSelected ? "bg-muted border-primary" : "bg-card hover:translate-x-1 hover:-translate-y-1",
                                )}
                            >
                                <div className="flex-1 min-w-0 flex flex-col gap-2">
                                    <div className="flex items-center justify-between gap-2">
                                        <div className="min-w-0">
                                            <p className="font-bold text-card-foreground truncate uppercase">{module.name}</p>
                                            <p className="text-xs text-muted-foreground truncate font-mono">{module.moduleKey}</p>
                                        </div>
                                        <Badge className={sourceColors[module.source]}>{module.source}</Badge>
                                    </div>
                                    <div
                                        className="flex items-center justify-between gap-2 text-sm text-muted-foreground">
                                        <span className="font-medium">v{module.version}</span>
                                        {updatedLabel && <span className="text-xs">Updated {updatedLabel}</span>}
                                    </div>
                                    {tags.length > 0 && (
                                        <div className="flex flex-wrap gap-1 pt-1">
                                            {tags.slice(0, 3).map((tag) => (
                                                <Badge key={`${module.moduleKey}-${tag}`} variant="outline"
                                                       className="text-xs brutal-border">
                                                    {tag}
                                                </Badge>
                                            ))}
                                            {tags.length > 3 && (
                                                <Badge variant="outline" className="text-xs brutal-border">
                                                    +{tags.length - 3}
                                                </Badge>
                                            )}
                                        </div>
                                    )}
                                </div>
                            </div>
                        </div>
                    )
                })}
                {filtered.length === 0 && (
                    <div className="p-6 text-center brutal-border bg-muted">
                        <p className="text-muted-foreground font-medium">No modules match that search.</p>
                    </div>
                )}
            </nav>
        </div>
    )
}
