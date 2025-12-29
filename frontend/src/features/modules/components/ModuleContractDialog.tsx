"use client"

import {useMemo, useState} from "react"
import type {ModuleContract} from "../api/modulesApi"
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "../../../../components/ui/dialog"
import {Button} from "../../../../components/ui/button"
import {ScrollArea} from "../../../../components/ui/scroll-area"

export default function ModuleContractDialog({
                                                 open,
                                                 moduleKey,
                                                 contract,
                                                 onClose,
}: {
    open: boolean
    moduleKey?: string
    contract?: ModuleContract
    onClose: () => void
}) {
    const [copied, setCopied] = useState(false)
    const rawContract = useMemo(() => {
        if (!contract) return ""
        // @ts-ignore
        return (contract as any).rawContract ?? JSON.stringify(contract, null, 2)
    }, [contract])
    const canCopy = !!rawContract

    const handleCopy = async () => {
        if (!rawContract) return
        try {
            await navigator.clipboard.writeText(rawContract)
            setCopied(true)
            setTimeout(() => setCopied(false), 1500)
        } catch (error) {
            console.error("Copy failed", error)
        }
    }

    return (
        <Dialog open={open} onOpenChange={onClose}>
            <DialogContent className="brutal-border brutal-shadow-sm bg-card max-w-4xl max-h-[90vh]">
                <DialogHeader className="border-b-4 border-border pb-4">
                    <DialogTitle className="text-card-foreground uppercase text-xl">
                        {moduleKey ? `${moduleKey} Contract` : "Contract"}
                    </DialogTitle>
                    <DialogDescription>
                        The raw contract is generated directly from the XML and may contain private values.
                    </DialogDescription>
                </DialogHeader>
                <ScrollArea className="max-h-[60vh]">
          <pre
              className="whitespace-pre-wrap break-words text-xs font-mono p-4 bg-muted rounded-none brutal-border text-card-foreground">
            {rawContract || "<no contract available>"}
          </pre>
                </ScrollArea>
                <DialogFooter className="gap-2 border-t-4 border-border pt-4">
                    <Button
                        variant="outline"
                        onClick={onClose}
                        className="brutal-border brutal-shadow-sm bg-card hover:bg-muted uppercase"
                    >
                        Close
                    </Button>
                    <Button
                        onClick={handleCopy}
                        disabled={!canCopy}
                        className="brutal-border brutal-shadow-sm bg-primary text-primary-foreground hover:bg-primary/90 uppercase"
                    >
                        {copied ? "Copied!" : "Copy"}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    )
}
