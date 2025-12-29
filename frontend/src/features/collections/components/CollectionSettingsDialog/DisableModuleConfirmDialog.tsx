"use client"

import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from "../../../../../components/ui/alert-dialog"

type Props = {
    open: boolean
    moduleName?: string
    onCancel: () => void
    onConfirm: () => void
}

export default function DisableModuleConfirmDialog({ open, moduleName, onCancel, onConfirm }: Props) {
    return (
        <AlertDialog open={open} onOpenChange={(isOpen) => !isOpen && onCancel()}>
            <AlertDialogContent>
                <AlertDialogHeader>
                    <AlertDialogTitle>Disable module?</AlertDialogTitle>
                    <AlertDialogDescription>
                        {moduleName
                            ? `Items using the ${moduleName} module will no longer be available until it is re-enabled.`
                            : "Are you sure you want to disable this module?"}
                    </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                    <AlertDialogCancel onClick={onCancel}>Cancel</AlertDialogCancel>
                    <AlertDialogAction onClick={onConfirm} className="bg-destructive text-destructive-foreground">
                        Disable
                    </AlertDialogAction>
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    )
}
