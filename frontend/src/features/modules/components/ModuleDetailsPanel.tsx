"use client"

import type {ReactNode} from "react"
import {useState} from "react"
import ErrorState from "../../../components/ErrorState"
import type {ModuleDetails, ModuleSummary} from "../api/modulesApi"
import DeleteModuleDialog from "./DeleteModuleDialog"
import ModuleInfoCards from "./ModuleInfoCards"
import {useAuth} from "../../../auth/useAuth"
import {Skeleton} from "../../../../components/ui/skeleton"
import {Button} from "../../../../components/ui/button"
import {Badge} from "../../../../components/ui/badge"

type Props = {
    module?: ModuleDetails
    summary?: ModuleSummary
    loading?: boolean
    error?: string
    onViewRaw?: () => void
    onViewContract?: () => void
    onModuleDeleted?: (moduleKey: string) => void
}

const Section = ({ title, children }: { title: string; children: ReactNode }) => (
    <div>
        <p className="text-sm font-medium text-muted-foreground mb-2">{title}</p>
        <div className="flex flex-col gap-2">{children}</div>
    </div>
)

export default function ModuleDetailsPanel({
                                               module,
                                               summary,
                                               loading,
                                               error,
                                               onViewRaw,
                                               onViewContract,
                                               onModuleDeleted,
}: Props) {
    const {user} = useAuth()
    const [deleteDialogOpen, setDeleteDialogOpen] = useState(false)
    const canDelete = Boolean(user?.isAdmin && module?.source === "IMPORTED")

    console.log("[v0] ModuleDetailsPanel render:", {
        hasModule: !!module,
        hasSummary: !!summary,
        loading,
        error,
        moduleKey: module?.moduleKey || summary?.moduleKey,
    })

    const showSkeleton = loading && !module

    if (showSkeleton) {
        console.log("[v0] Showing skeleton")
        return (
            <div className="brutal-border brutal-shadow-sm bg-card p-6 min-h-[280px]">
                <div className="flex flex-col gap-4">
                    <Skeleton className="h-8 w-[45%]"/>
                    <Skeleton className="h-5 w-[30%]"/>
                    <Skeleton className="h-40 w-full rounded-md"/>
                </div>
            </div>
        )
    }

    if (error) {
        return (
            <div className="brutal-border brutal-shadow-sm bg-card p-6">
                <ErrorState message={error}/>
            </div>
        )
    }

    if (!module) {
        if (summary) {
            return (
                <div className="brutal-border brutal-shadow-sm bg-card p-6 min-h-[220px]">
                    <div className="flex flex-col gap-2">
                        <div className="flex flex-wrap justify-between gap-2">
                            <h2 className="text-xl font-bold text-primary">{summary.name}</h2>
                            <Badge className="brutal-border bg-primary text-primary-foreground">{summary.source}</Badge>
                        </div>
                        <p className="text-muted-foreground">
                            Version {summary.version} · {summary.moduleKey}
                        </p>
                        <p className="text-secondary-foreground">
                            Module details are syncing. The detailed contract metadata will appear shortly.
                        </p>
                    </div>
                </div>
            )
        }

        return (
            <div className="brutal-border brutal-shadow-sm bg-card p-6 min-h-[220px]">
                <div className="flex flex-col items-center justify-center h-full gap-3">
                    <h3 className="text-lg font-bold text-primary uppercase">Select a module</h3>
                    <p className="text-muted-foreground text-center">
                        Choose a module from the left column to view its contract and associated metadata.
                    </p>
                </div>
            </div>
        )
    }

    const description = module.contract.description || "No description provided."

    const handleOpenDelete = () => setDeleteDialogOpen(true)
    const handleCloseDelete = () => setDeleteDialogOpen(false)

    return (
        <div className="brutal-border brutal-shadow-sm bg-card p-6 min-h-[320px]">
            <div className="flex flex-col gap-6">
                <div className="flex flex-wrap items-start justify-between gap-3 pb-4 border-b-4 border-border">
                    <div className="min-w-0 flex-1">
                        <h2 className="text-2xl font-bold text-card-foreground truncate uppercase">{module.name}</h2>
                        <p className="text-sm text-muted-foreground mt-1">{description}</p>
                    </div>
                    <div className="flex flex-wrap gap-2">
                        <Button
                            size="sm"
                            variant="outline"
                            onClick={onViewRaw}
                            disabled={!onViewRaw}
                            className="brutal-border brutal-shadow-sm bg-card hover:bg-muted uppercase"
                        >
                            View XML
                        </Button>
                        <Button
                            size="sm"
                            variant="outline"
                            onClick={onViewContract}
                            disabled={!onViewContract}
                            className="brutal-border brutal-shadow-sm bg-card hover:bg-muted uppercase"
                        >
                            Contract
                        </Button>
                        {canDelete && (
                            <Button
                                size="sm"
                                variant="outline"
                                onClick={handleOpenDelete}
                                className="brutal-border brutal-shadow-sm text-destructive hover:bg-destructive hover:text-destructive-foreground uppercase bg-transparent"
                            >
                                Delete
                            </Button>
                        )}
                    </div>
                </div>

                <ModuleInfoCards module={module}/>

                <Section title="STATES">
                    <div className="flex flex-wrap gap-2">
                        {module.contract.states.map((state) => (
                            <Badge key={state.key} variant="outline" className="brutal-border uppercase">
                                {state.label} ({state.key})
                            </Badge>
                        ))}
                    </div>
                </Section>

                <Section title="PROVIDERS">
                    {module.contract.providers.map((provider) => (
                        <div key={provider.key} className="flex flex-col gap-2 p-3 brutal-border bg-muted/30">
                            <div className="flex items-center gap-2 flex-wrap">
                                <span className="font-bold text-card-foreground uppercase">{provider.key}</span>
                                <Badge
                                    className={`brutal-border uppercase ${provider.enabled ? "bg-green-600 text-white" : "bg-muted text-muted-foreground"}`}
                                >
                                    {provider.enabled ? "Enabled" : "Disabled"}
                                </Badge>
                                <Badge variant="outline" className="brutal-border">
                                    Priority: {provider.priority}
                                </Badge>
                            </div>
                            {provider.supportsIdentifiers.length > 0 && (
                                <p className="text-xs text-muted-foreground">Supports: {provider.supportsIdentifiers.join(", ")}</p>
                            )}
                        </div>
                    ))}
                </Section>

                <Section title="FIELDS">
                    <div className="grid gap-3">
                        {module.contract.fields.map((field) => (
                            <div key={field.key} className="brutal-border brutal-shadow-sm bg-muted/50 p-4">
                                <div className="flex flex-col gap-2">
                                    <div className="flex items-center gap-2 flex-wrap">
                                        <span
                                            className="font-bold text-card-foreground truncate uppercase">{field.label}</span>
                                        <Badge variant="outline" className="brutal-border uppercase">
                                            {field.type}
                                        </Badge>
                                        {field.required && (
                                            <Badge
                                                className="brutal-border bg-amber-500 text-white uppercase">Required</Badge>
                                        )}
                                    </div>
                                    <p className="text-xs text-muted-foreground truncate font-mono">{field.key}</p>
                                    {(() => {
                                        const status = [
                                            field.searchable ? "Searchable" : null,
                                            field.filterable ? "Filterable" : null,
                                            field.sortable ? "Sortable" : null,
                                        ]
                                            .filter(Boolean)
                                            .join(" · ")
                                        return status ? <p className="text-xs text-muted-foreground">{status}</p> : null
                                    })()}
                                    {field.identifiers.length > 0 && (
                                        <p className="text-xs text-muted-foreground">Identifiers: {field.identifiers.join(", ")}</p>
                                    )}
                                    {field.enumValues.length > 0 && (
                                        <div className="flex flex-wrap gap-1 pt-1">
                                            {field.enumValues.map((value) => (
                                                <Badge key={value.key} variant="outline"
                                                       className="text-xs brutal-border">
                                                    {value.key}
                                                </Badge>
                                            ))}
                                        </div>
                                    )}
                                </div>
                            </div>
                        ))}
                    </div>
                </Section>

                <Section title="WORKFLOWS">
                    <div className="grid gap-3">
                        {module.contract.workflows.map((workflow) => (
                            <div key={workflow.key} className="brutal-border brutal-shadow-sm bg-muted/50 p-4">
                                <div className="flex flex-col gap-3">
                                    <div>
                                        <p className="font-bold text-card-foreground uppercase">{workflow.label}</p>
                                        <p className="text-xs text-muted-foreground font-mono">{workflow.key}</p>
                                    </div>
                                    <div className="flex flex-col gap-2">
                                        {workflow.steps.map((step, idx) => (
                                            <div key={`${workflow.key}-${idx}`} className="brutal-border bg-card p-3">
                                                <div className="flex flex-col gap-1">
                                                    <p className="text-sm font-bold text-card-foreground uppercase">
                                                        Step {idx + 1}: {step.type}
                                                    </p>
                                                    {step.label &&
                                                        <p className="text-sm text-muted-foreground">{step.label}</p>}
                                                    {step.field &&
                                                        <p className="text-xs text-muted-foreground font-mono">Field: {step.field}</p>}
                                                    {step.fields.length > 0 && (
                                                        <p className="text-xs text-muted-foreground">Fields: {step.fields.join(", ")}</p>
                                                    )}
                                                    {step.providers.length > 0 && (
                                                        <p className="text-xs text-muted-foreground">Providers: {step.providers.join(", ")}</p>
                                                    )}
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                </Section>
            </div>

            <DeleteModuleDialog
                open={deleteDialogOpen}
                moduleKey={module.moduleKey}
                moduleName={module.name}
                onClose={handleCloseDelete}
                onModuleDeleted={() => {
                    handleCloseDelete()
                    onModuleDeleted?.(module.moduleKey)
                }}
            />
        </div>
    )
}
