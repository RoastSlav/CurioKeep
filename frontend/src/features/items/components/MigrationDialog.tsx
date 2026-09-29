import { useEffect, useState } from "react"
import { getErrorMessage } from "@/api/errors"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { applyMigration, previewMigration, type MigrationFieldChange, type MigrationPreview, type MigrationResult } from "../api"

type MigrationDialogProps = {
    open: boolean
    onOpenChange: (open: boolean) => void
    collectionId: string
    moduleId: string
    /** Called once items have been migrated, so the list and counts can be refreshed. */
    onMigrated: () => void
}

export function MigrationDialog({ open, onOpenChange, collectionId, moduleId, onMigrated }: MigrationDialogProps) {
    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="sm:max-w-2xl">
                <DialogHeader>
                    <DialogTitle>Bring items up to date</DialogTitle>
                    <DialogDescription>
                        The module's author described how to move items saved under an earlier version to the current one. Check what would change, then
                        accept.
                    </DialogDescription>
                </DialogHeader>
                <MigrationContent collectionId={collectionId} moduleId={moduleId} onMigrated={onMigrated} onClose={() => onOpenChange(false)} />
            </DialogContent>
        </Dialog>
    )
}

// Mounted only while the dialog is open, so every opening reads a fresh preview.
function MigrationContent({ collectionId, moduleId, onMigrated, onClose }: Omit<MigrationDialogProps, "open" | "onOpenChange"> & { onClose: () => void }) {
    const [preview, setPreview] = useState<MigrationPreview | null>(null)
    const [result, setResult] = useState<MigrationResult | null>(null)
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        const controller = new AbortController()
        previewMigration(collectionId, moduleId, { signal: controller.signal })
            .then(setPreview)
            .catch((err) => {
                if (!controller.signal.aborted) setError(getErrorMessage(err, "Could not read what would change"))
            })
        return () => controller.abort()
    }, [collectionId, moduleId])

    const accept = async () => {
        setBusy(true)
        setError(null)
        try {
            setResult(await applyMigration(collectionId, moduleId))
            onMigrated()
        } catch (err) {
            setError(getErrorMessage(err, "The migration failed"))
        } finally {
            setBusy(false)
        }
    }

    if (result) {
        return (
            <div className="space-y-4">
                <Alert>
                    <AlertDescription>
                        Brought {result.migrated} {result.migrated === 1 ? "item" : "items"} up to date; {result.changed} had values changed
                        {(result.skipped ?? 0) > 0 ? `, ${result.skipped} could not be read and were left alone` : ""}.
                    </AlertDescription>
                </Alert>
                <DialogFooter>
                    <Button onClick={onClose}>Done</Button>
                </DialogFooter>
            </div>
        )
    }

    return (
        <div className="space-y-4">
            {error && (
                <Alert variant="destructive">
                    <AlertDescription>{error}</AlertDescription>
                </Alert>
            )}
            {!preview && !error && <p>Checking your items...</p>}
            {preview && <PreviewDetails preview={preview} />}
            <DialogFooter>
                <Button type="button" variant="outline" onClick={onClose}>
                    Cancel
                </Button>
                <Button type="button" onClick={accept} disabled={!preview || preview.behind === 0 || busy}>
                    {busy ? "Migrating..." : "Accept and migrate"}
                </Button>
            </DialogFooter>
        </div>
    )
}

function PreviewDetails({ preview }: { preview: MigrationPreview }) {
    const samples = preview.samples ?? []
    return (
        <div className="space-y-3">
            <p>
                {preview.behind} {preview.behind === 1 ? "item is" : "items are"} on an earlier version
                {preview.versions?.length ? ` (${preview.versions.map((v) => `${v.version}: ${v.items}`).join(", ")})` : ""}. Bringing them to version{" "}
                <strong>{preview.targetVersion}</strong> would change values on {preview.changed}; the rest only move to the new version.
            </p>
            {samples.length > 0 && (
                <div className="max-h-64 space-y-3 overflow-y-auto border-2 border-border p-3 text-sm">
                    <p className="font-bold">Examples</p>
                    {samples.map((sample) => (
                        <div key={sample.itemId}>
                            <p className="font-bold">{sample.title || sample.itemId}</p>
                            <ul>
                                {sample.changes.map((change) => (
                                    <li key={change.field}>
                                        <code>{change.field}</code>: {describe(change.before)} → {describe(change.after)}
                                    </li>
                                ))}
                            </ul>
                        </div>
                    ))}
                </div>
            )}
            <p className="text-sm font-bold">This cannot be undone. Export the collection first if you want a copy of the current data.</p>
        </div>
    )
}

function describe(value: MigrationFieldChange["before"]): string {
    return value === undefined || value === null ? "(empty)" : typeof value === "string" ? `"${value}"` : JSON.stringify(value)
}
