import { AlertTriangle } from "lucide-react"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import type { FieldContract } from "@/features/modules/moduleTypes"

export type DeprecatedFieldUse = {
    field: FieldContract
    /** How many items still hold a value in this field. */
    count: number
}

type DeprecatedFieldsNoticeProps = {
    usage: DeprecatedFieldUse[]
    /** The deprecated field whose items are being shown right now, if any. */
    showing?: string
    onShow: (fieldKey: string) => void
    onClear: () => void
}

export function DeprecatedFieldsNotice({ usage, showing, onShow, onClear }: DeprecatedFieldsNoticeProps) {
    if (usage.length === 0) return null

    return (
        <Alert>
            <AlertTriangle className="h-4 w-4" aria-hidden="true" />
            <AlertDescription className="space-y-3">
                <p>
                    Some items still have values in fields this module has deprecated. Open an item and move each value to the field that replaces it. The
                    values are kept until a newer version of the module removes the field.
                </p>
                <ul className="space-y-2">
                    {usage.map(({ field, count }) => (
                        <li key={field.key} className="flex flex-wrap items-center gap-3">
                            <span>
                                <strong>{field.label || field.key}</strong>: {count} {count === 1 ? "item" : "items"}
                                {field.replacedBy ? ` (replaced by ${field.replacedBy})` : ""}
                            </span>
                            {showing === field.key ? (
                                <Button size="sm" variant="outline" onClick={onClear}>
                                    Show all items
                                </Button>
                            ) : (
                                <Button size="sm" variant="outline" onClick={() => onShow(field.key)}>
                                    Show these items
                                </Button>
                            )}
                        </li>
                    ))}
                </ul>
            </AlertDescription>
        </Alert>
    )
}
