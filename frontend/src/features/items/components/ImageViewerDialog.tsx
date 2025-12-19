"use client"

import {X} from "lucide-react"
import {Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle} from "../../../../components/ui/dialog"
import {Button} from "../../../../components/ui/button"

export default function ImageViewerDialog({
                                              open,
                                              src,
                                              title,
                                              onClose,
}: {
    open: boolean
    src: string | null
    title?: string
    onClose: () => void
}) {
    return (
        <Dialog open={open} onOpenChange={(isOpen) => !isOpen && onClose()}>
            <DialogContent className="max-w-4xl max-h-[90vh]">
                <DialogHeader className="flex flex-row items-center justify-between">
                    <DialogTitle className="font-bold">{title || "Image"}</DialogTitle>
                    <DialogDescription className="sr-only">Full size image preview</DialogDescription>
                    <Button size="icon-sm" variant="ghost" onClick={onClose}>
                        <X className="w-4 h-4"/>
                    </Button>
                </DialogHeader>
                <div className="flex items-center justify-center">
                    {src && (
                        <img
                            src={src || "/placeholder.svg"}
                            alt={title || "Image"}
                            className="max-w-full max-h-[75vh] object-contain"
                        />
                    )}
                </div>
            </DialogContent>
        </Dialog>
    )
}
