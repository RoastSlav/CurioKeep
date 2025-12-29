"use client"

import type {ReactNode} from "react"
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from "../../components/ui/alert-dialog"

type Props = {
    open: boolean
    title?: string
    message?: ReactNode
    confirmLabel?: string
    cancelLabel?: string
    onConfirm: () => void
    onClose: () => void
}

export default function ConfirmDialog({
                                          open,
                                          title = "Are you sure?",
                                          message,
                                          confirmLabel = "Confirm",
                                          cancelLabel = "Cancel",
                                          onConfirm,
                                          onClose,
                                      }: Props) {
    return (
        <AlertDialog open={open} onOpenChange={(isOpen) => !isOpen && onClose()}>
            <AlertDialogContent>
                <AlertDialogHeader>
                    <AlertDialogTitle>{title}</AlertDialogTitle>
                    {message &&
                        <AlertDialogDescription className="text-text-secondary">{message}</AlertDialogDescription>}
                </AlertDialogHeader>
                <AlertDialogFooter>
                    <AlertDialogCancel onClick={onClose}>{cancelLabel}</AlertDialogCancel>
                    <AlertDialogAction onClick={onConfirm} className="bg-red-600 hover:bg-red-700 text-white">
                        {confirmLabel}
                    </AlertDialogAction>
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    )
}
