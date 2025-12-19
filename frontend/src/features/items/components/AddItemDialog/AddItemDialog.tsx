"use client"

import {useEffect, useMemo, useState} from "react"
import type {Item, ModuleDefinition, WorkflowDef} from "../../../../api/types"
import WorkflowRunner from "./WorkflowRunner"
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
} from "../../../../../components/ui/dialog"
import {cn} from "../../../../../lib/utils"

function buildFallbackWorkflow(moduleDefinition: ModuleDefinition | null | undefined): WorkflowDef {
    const fields = moduleDefinition?.fields ?? []
    const fieldKeys = fields.map((f) => f.key)
    return {
        key: "manual_add_fallback",
        label: "Manual add",
        steps: [{type: "PROMPT_ANY", fields: fieldKeys}, {type: "SAVE_ITEM"}],
    }
}

export default function AddItemDialog({
                                          open,
                                          onClose,
                                          moduleDefinition,
                                          moduleId,
                                          collectionId,
                                          defaultState,
                                          onCreated,
}: {
    open: boolean
    onClose: () => void
    moduleDefinition: ModuleDefinition | null | undefined
    moduleId: string
    collectionId: string
    defaultState?: string
    onCreated?: (item: Item) => void
}) {
    const workflows = useMemo(() => {
        const base = moduleDefinition?.workflows && moduleDefinition.workflows.length ? moduleDefinition.workflows : []
        if (base.length) return base
        return [buildFallbackWorkflow(moduleDefinition)]
    }, [moduleDefinition])

    const [selectedKey, setSelectedKey] = useState(workflows[0]?.key)

    useEffect(() => {
        setSelectedKey(workflows[0]?.key)
    }, [workflows])
    const selectedWorkflow = workflows.find((w) => w.key === selectedKey) || workflows[0]

    const handleCreated = (item: Item) => {
        onCreated?.(item)
        onClose()
    }

    return (
        <Dialog open={open} onOpenChange={(isOpen) => !isOpen && onClose()}>
            <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
                <DialogHeader>
                    <DialogTitle className="text-xl font-bold uppercase">Add Item</DialogTitle>
                    <DialogDescription>Choose a workflow and follow the steps to add a new item</DialogDescription>
                </DialogHeader>

                <div className="space-y-6 mt-4">
                    <div className="space-y-2">
                        <h4 className="text-sm font-bold uppercase">Choose workflow</h4>
                        <div className="space-y-1">
                            {workflows.map((wf) => (
                                <button
                                    key={wf.key}
                                    onClick={() => setSelectedKey(wf.key)}
                                    className={cn(
                                        "w-full text-left px-4 py-2 border-2 border-border transition-colors",
                                        wf.key === selectedKey ? "bg-secondary text-secondary-foreground" : "bg-card hover:bg-muted",
                                    )}
                                >
                                    {wf.label || wf.key}
                                </button>
                            ))}
                        </div>
                    </div>

                    {selectedWorkflow ? (
                        <WorkflowRunner
                            workflow={selectedWorkflow}
                            moduleDefinition={moduleDefinition}
                            moduleId={moduleId}
                            collectionId={collectionId}
                            defaultState={defaultState}
                            onComplete={handleCreated}
                            onCancel={onClose}
                        />
                    ) : (
                        <p className="text-muted-foreground">No workflow available.</p>
                    )}
                </div>
            </DialogContent>
        </Dialog>
    )
}
