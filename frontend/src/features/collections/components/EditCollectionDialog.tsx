"use client"

import type React from "react"

import {useEffect, useState} from "react"
import type {Collection} from "../../../api/types"
import type {UpdateCollectionRequest} from "../api"
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "../../../../components/ui/dialog"
import {Button} from "../../../../components/ui/button"
import {Input} from "../../../../components/ui/input"
import {Label} from "../../../../components/ui/label"
import {Textarea} from "../../../../components/ui/textarea"

export default function EditCollectionDialog({
                                                 open,
                                                 collection,
                                                 onClose,
                                                 onSave,
}: {
    open: boolean
    collection: Collection | null
    onClose: () => void
    onSave: (payload: UpdateCollectionRequest) => Promise<void>
}) {
    const [name, setName] = useState("")
    const [description, setDescription] = useState("")
    const [saving, setSaving] = useState(false)

    useEffect(() => {
        if (collection && open) {
            setName(collection.name ?? "")
            setDescription(collection.description ?? "")
            setSaving(false)
        }
    }, [collection, open])

    if (!collection) return null

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault()
        if (!name.trim()) return
        setSaving(true)
        try {
            await onSave({name: name.trim(), description: description.trim() || undefined})
        } finally {
            setSaving(false)
        }
    }

    const disabled = saving || !name.trim()

    return (
        <Dialog open={open} onOpenChange={(isOpen) => !isOpen && onClose()}>
            <DialogContent className="sm:max-w-[500px]">
                <form onSubmit={handleSubmit}>
                    <DialogHeader>
            <DialogTitle>Edit collection</DialogTitle>
                        <DialogDescription>Update your collection details</DialogDescription>
                    </DialogHeader>
                    <div className="space-y-4 py-4">
                        <div className="space-y-2">
                            <Label htmlFor="name">Name *</Label>
                            <Input
                                id="name"
                                value={name}
                                onChange={(e) => setName(e.target.value)}
                                placeholder="Enter collection name"
                                autoFocus
                                required
                            />
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="description">Description</Label>
                            <Textarea
                                id="description"
                                value={description}
                                onChange={(e) => setDescription(e.target.value)}
                                placeholder="Enter collection description"
                                rows={3}
                            />
                        </div>
                    </div>
                    <DialogFooter>
                        <Button type="button" variant="outline" onClick={onClose} disabled={saving}>
                            Cancel
                        </Button>
                        <Button type="submit" disabled={disabled} className="bg-secondary hover:bg-secondary-dark">
                            {saving ? "Saving..." : "Save"}
                        </Button>
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    )
}
