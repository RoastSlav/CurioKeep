import {Download, Plus, Settings, Upload} from "lucide-react"
import {Button} from "@/components/ui/button"
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from "@/components/ui/dropdown-menu"
import type {ExportFormat} from "../../items/api"

export default function CollectionActionsMenu({
                                                  role,
                                                  moduleName,
                                                  onAddItem,
                                                  onOpenSettings,
                                                  onExport,
                                                  onImport,
}: {
    role?: string
    /** Name of the module on screen, which a CSV export is limited to. */
    moduleName?: string
    onAddItem?: () => void
    onOpenSettings?: () => void
    onExport?: (format: ExportFormat) => void
    onImport?: () => void
}) {
    const roleUpper = role?.toUpperCase()
    const canAdd = roleUpper === "OWNER" || roleUpper === "ADMIN" || roleUpper === "EDITOR"
    const canManage = roleUpper === "OWNER" || roleUpper === "ADMIN"

    if (!canAdd && !canManage && !onExport) return null

    return (
        <div className="flex gap-3 flex-shrink-0">
            {canAdd && (
                <Button onClick={onAddItem} className="bg-secondary hover:bg-secondary-dark">
                    <Plus className="w-4 h-4 mr-2"/>
                    Add Item
                </Button>
            )}
            {onExport && (
                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <Button variant="outline">
                            <Download className="w-4 h-4 mr-2"/>
                            Export
                        </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end">
                        <DropdownMenuItem onSelect={() => onExport("json")}>All items as JSON</DropdownMenuItem>
                        <DropdownMenuItem onSelect={() => onExport("csv")}>
                            {moduleName ? `${moduleName} as CSV` : "This module as CSV"}
                        </DropdownMenuItem>
                    </DropdownMenuContent>
                </DropdownMenu>
            )}
            {canAdd && onImport && (
                <Button variant="outline" onClick={onImport}>
                    <Upload className="w-4 h-4 mr-2"/>
                    Import
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
