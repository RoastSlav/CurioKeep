"use client"

import {useCallback, useEffect, useState} from "react"
import {isApiError} from "../../../api/errors"
import {deleteImportedModule} from "../api/modulesApi"
import {
    AlertDialog,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from "../../../../components/ui/alert-dialog"
import {Button} from "../../../../components/ui/button"
import {Alert, AlertDescription} from "../../../../components/ui/alert"
import {AlertTriangle} from "lucide-react"

type Props = {
    open: boolean
    moduleKey?: string
    moduleName?: string
    onClose: () => void
    onModuleDeleted: () => void
}

export default function DeleteModuleDialog({ open, moduleKey, moduleName, onClose, onModuleDeleted }: Props) {
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        if (!open) {
            setLoading(false)
            setError(null)
        }
    }, [open])

    const handleConfirm = useCallback(async () => {
        if (!moduleKey) return
        setLoading(true)
        setError(null)
        try {
            await deleteImportedModule(moduleKey)
            onModuleDeleted()
            onClose()
        } catch (err) {
            const message = isApiError(err) ? err.message : "Failed to delete module"
            setError(message)
        } finally {
            setLoading(false)
        }
    }, [moduleKey, onClose, onModuleDeleted])

    const title = moduleName ? `Delete ${moduleName}` : "Delete module"

    return (
        <AlertDialog open={open} onOpenChange={onClose}>
            <AlertDialogContent className="brutal-border brutal-shadow-sm bg-card">
                <AlertDialogHeader className="border-b-4 border-border pb-4">
                    <AlertDialogTitle className="flex items-center gap-2 text-card-foreground uppercase">
                        <AlertTriangle className="h-5 w-5 text-destructive"/>
                        {title}
                    </AlertDialogTitle>
                    <AlertDialogDescription className="flex flex-col gap-3 mt-3">
            <span className="text-muted-foreground">
              Deleting a module may break collections/items that depend on it.
            </span>
                        <span className="font-medium">
              This action cannot be undone. The module definition and its imported XML will be removed from the system.
            </span>
                        {error && (
                            <Alert variant="destructive" className="brutal-border">
                                <AlertDescription>{error}</AlertDescription>
                            </Alert>
                        )}
                        <span className="text-xs text-muted-foreground font-mono">{moduleKey}</span>
                    </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter className="gap-2 border-t-4 border-border pt-4">
                    <Button
                        variant="outline"
                        onClick={onClose}
                        disabled={loading}
                        className="brutal-border brutal-shadow-sm bg-card hover:bg-muted uppercase"
                    >
                        Cancel
                    </Button>
                    <Button
                        onClick={handleConfirm}
                        disabled={loading || !moduleKey}
                        className="brutal-border brutal-shadow-sm bg-destructive text-destructive-foreground hover:bg-destructive/90 uppercase"
                    >
                        {loading ? "Deleting..." : "Delete Module"}
                    </Button>
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    )
}
