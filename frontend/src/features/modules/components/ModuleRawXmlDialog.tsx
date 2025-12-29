"use client"

import {useCallback, useMemo, useState} from "react"
import LoadingState from "../../../components/LoadingState"
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "../../../../components/ui/dialog"
import {Button} from "../../../../components/ui/button"
import {Alert, AlertDescription} from "../../../../components/ui/alert"
import {ScrollArea} from "../../../../components/ui/scroll-area"

type Props = {
    open: boolean
    moduleKey?: string
    xml?: string
    error?: string
    loading?: boolean
    onClose: () => void
}

export default function ModuleRawXmlDialog({ open, moduleKey, xml, error, loading = false, onClose }: Props) {
    const [copied, setCopied] = useState(false)
    const bodyText = useMemo(() => error ?? xml ?? "<no XML available>", [error, xml])
    const canCopy = Boolean(xml && !loading && !error)

    const handleCopy = useCallback(async () => {
        if (!xml) return
        try {
            await navigator.clipboard.writeText(xml)
            setCopied(true)
            setTimeout(() => setCopied(false), 1500)
        } catch (copyError) {
            console.error("Copy failed", copyError)
        }
    }, [xml])

    return (
        <Dialog open={open} onOpenChange={onClose}>
            <DialogContent className="brutal-border brutal-shadow-sm bg-card max-w-4xl max-h-[90vh]">
                <DialogHeader className="border-b-4 border-border pb-4">
                    <DialogTitle className="text-card-foreground uppercase text-xl">
                        {moduleKey ? `${moduleKey} XML` : "Module XML"}
                    </DialogTitle>
                    <DialogDescription className="font-mono text-xs">{moduleKey}</DialogDescription>
                </DialogHeader>
                {loading ? (
                    <LoadingState message="Loading raw XML..."/>
                ) : error ? (
                    <Alert variant="destructive" className="brutal-border">
                        <AlertDescription>{error}</AlertDescription>
                    </Alert>
                ) : (
                    <ScrollArea className="max-h-[60vh]">
            <pre
                className="whitespace-pre-wrap break-words text-xs font-mono p-4 bg-muted rounded-none brutal-border text-card-foreground">
              {bodyText}
            </pre>
                    </ScrollArea>
                )}
                <DialogFooter className="gap-2 border-t-4 border-border pt-4">
                    <Button
                        variant="outline"
                        onClick={handleCopy}
                        disabled={!canCopy}
                        className="brutal-border brutal-shadow-sm bg-card hover:bg-muted uppercase"
                    >
                        {copied ? "Copied!" : "Copy"}
                    </Button>
                    <Button
                        onClick={onClose}
                        className="brutal-border brutal-shadow-sm bg-primary text-primary-foreground hover:bg-primary/90 uppercase"
                    >
                        Close
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    )
}
