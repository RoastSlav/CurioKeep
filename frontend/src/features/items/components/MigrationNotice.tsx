import { RefreshCw } from "lucide-react"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"

type MigrationNoticeProps = {
    /** Items on an earlier module version that the module has a migration for. */
    pending: number
    onReview: () => void
}

export function MigrationNotice({ pending, onReview }: MigrationNoticeProps) {
    if (pending <= 0) return null

    return (
        <Alert>
            <RefreshCw className="h-4 w-4" aria-hidden="true" />
            <AlertDescription className="flex flex-wrap items-center gap-3">
                <span>
                    This module was updated. {pending} {pending === 1 ? "item was" : "items were"} saved under an earlier version and can be brought
                    up to date automatically.
                </span>
                <Button size="sm" variant="outline" onClick={onReview}>
                    Review changes
                </Button>
            </AlertDescription>
        </Alert>
    )
}
