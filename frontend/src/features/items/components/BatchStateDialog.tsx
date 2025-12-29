"use client"

import {useState} from "react"
import type {ModuleStateDef} from "../../../api/types"
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "../../../../components/ui/dialog"
import {Button} from "../../../../components/ui/button"
import {Label} from "../../../../components/ui/label"
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from "../../../../components/ui/select"

export default function BatchStateDialog({
                                             open,
                                             states,
                                             onClose,
                                             onConfirm,
}: {
    open: boolean
    states?: ModuleStateDef[]
    onClose: () => void
    onConfirm: (stateKey: string) => void
}) {
    const [stateKey, setStateKey] = useState<string>(states?.[0]?.key || "")

    const handleConfirm = () => {
        if (stateKey) onConfirm(stateKey)
    }

    return (
        <Dialog open={open} onOpenChange={(isOpen) => !isOpen && onClose()}>
            <DialogContent>
                <DialogHeader>
                    <DialogTitle className="font-bold uppercase">Change state for selected</DialogTitle>
                    <DialogDescription>Select a state to apply to all selected items</DialogDescription>
                </DialogHeader>
                <div className="space-y-3 py-4">
                    <Label htmlFor="batch-state-select">State</Label>
                    <Select value={stateKey} onValueChange={setStateKey}>
                        <SelectTrigger>
                            <SelectValue placeholder="Select state"/>
                        </SelectTrigger>
                        <SelectContent>
                            {states?.map((s) => (
                                <SelectItem key={s.key} value={s.key}>
                                    {s.label || s.key}
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                </div>
                <DialogFooter>
                    <Button variant="outline" onClick={onClose}>
                        Cancel
                    </Button>
                    <Button onClick={handleConfirm} disabled={!stateKey}>
                        Apply
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    )
}
