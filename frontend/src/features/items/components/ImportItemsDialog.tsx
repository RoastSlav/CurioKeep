import { useState } from "react"
import type { FormEvent } from "react"
import { getErrorMessage } from "@/api/errors"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { importItems, type ImportResult } from "../api"

type ImportItemsDialogProps = {
    open: boolean
    onOpenChange: (open: boolean) => void
    collectionId: string
    /** Called once items have been added, so the list and counts can be refreshed. */
    onImported: () => void
}

export function ImportItemsDialog({ open, onOpenChange, collectionId, onImported }: ImportItemsDialogProps) {
    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="sm:max-w-lg">
                <DialogHeader>
                    <DialogTitle>Import items</DialogTitle>
                    <DialogDescription>
                        Choose a JSON file exported from CurioKeep. Items are added to this collection; nothing is changed or removed, and importing the same file twice adds every item twice. Cover images are not part of the file.
                    </DialogDescription>
                </DialogHeader>
                <ImportForm collectionId={collectionId} onImported={onImported} onClose={() => onOpenChange(false)} />
            </DialogContent>
        </Dialog>
    )
}

// Mounted only while the dialog is open, so every opening starts without the previous file or result.
function ImportForm({ collectionId, onImported, onClose }: { collectionId: string; onImported: () => void; onClose: () => void }) {
    const [file, setFile] = useState<File | null>(null)
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [result, setResult] = useState<ImportResult | null>(null)

    const submit = async (event: FormEvent) => {
        event.preventDefault()
        if (!file) return
        setBusy(true)
        setError(null)
        try {
            const outcome = await importItems(collectionId, file)
            setResult(outcome)
            if (outcome.imported > 0) onImported()
        } catch (err) {
            setError(getErrorMessage(err, "The import failed"))
        } finally {
            setBusy(false)
        }
    }

    if (result) {
        return (
            <div className="space-y-4">
                <Alert>
                    <AlertDescription>
                        Imported {result.imported} {result.imported === 1 ? "item" : "items"}
                        {result.failed > 0 ? `, skipped ${result.failed}` : ""}.
                    </AlertDescription>
                </Alert>
                {result.errors.length > 0 && (
                    <div className="max-h-40 overflow-y-auto border-2 border-border p-3 text-sm">
                        <p className="font-bold">Skipped items</p>
                        <ul>
                            {result.errors.map((item) => (
                                <li key={item.index}>
                                    Item {item.index + 1}: {item.reason}
                                </li>
                            ))}
                        </ul>
                        {result.failed > result.errors.length && <p>…and {result.failed - result.errors.length} more.</p>}
                    </div>
                )}
                <DialogFooter>
                    <Button onClick={onClose}>Done</Button>
                </DialogFooter>
            </div>
        )
    }

    return (
        <form onSubmit={submit} className="space-y-4">
            {error && (
                <Alert variant="destructive">
                    <AlertDescription>{error}</AlertDescription>
                </Alert>
            )}
            <div className="space-y-2">
                <Label htmlFor="import-file">Export file</Label>
                <Input id="import-file" type="file" accept=".json,application/json" onChange={(event) => setFile(event.target.files?.[0] ?? null)} />
            </div>
            <DialogFooter>
                <Button type="button" variant="outline" onClick={onClose}>
                    Cancel
                </Button>
                <Button type="submit" disabled={!file || busy}>
                    {busy ? "Importing..." : "Import"}
                </Button>
            </DialogFooter>
        </form>
    )
}
