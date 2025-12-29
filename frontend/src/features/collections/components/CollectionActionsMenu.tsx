"use client"

import {Plus, Settings} from "lucide-react"
import {Button} from "../../../../components/ui/button"

export default function CollectionActionsMenu({
                                                  role,
                                                  onAddItem,
                                                  onOpenSettings,
}: {
    role?: string
    onAddItem?: () => void
    onOpenSettings?: () => void
}) {
    const roleUpper = role?.toUpperCase()
    const canAdd = roleUpper === "OWNER" || roleUpper === "ADMIN" || roleUpper === "EDITOR"
    const canManage = roleUpper === "OWNER" || roleUpper === "ADMIN"

    if (!canAdd && !canManage) return null

    return (
        <div className="flex gap-3 flex-shrink-0">
            {canAdd && (
                <Button onClick={onAddItem} className="bg-secondary hover:bg-secondary-dark">
                    <Plus className="w-4 h-4 mr-2"/>
                    Add Item
                </Button>
            )}
            {canManage && (
                <Button variant="outline" onClick={onOpenSettings}>
                    <Settings className="w-4 h-4 mr-2"/>
                    Collection Settings
                </Button>
            )}
        </div>
    )
}
