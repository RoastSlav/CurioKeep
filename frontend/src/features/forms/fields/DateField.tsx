import { toInputValue } from "../formValue"
import {Input} from "@/components/ui/input"
import {Label} from "@/components/ui/label"
import type { FieldContract } from "@/features/modules/moduleTypes";

export default function DateFieldField({
                                           field,
                                           value,
                                           error,
                                           disabled,
                                           onChange,
                                           onBlur,
}: {
    field: FieldContract
    value: unknown
    error?: string
    disabled?: boolean
    onChange: (value: unknown) => void
    onBlur?: () => void
}) {
    const id = `field-${field.key}`

    return (
        <div className="flex flex-col gap-2">
            <Label htmlFor={id} className="text-sm font-semibold text-foreground">
                {field.label || field.key}
                {field.required && <span className="text-destructive ml-1">*</span>}
            </Label>
            <Input
                id={id}
                type="date"
                value={toInputValue(value)}
                placeholder={field.ui?.placeholder}
                onChange={(e) => onChange(e.target.value)}
                onBlur={onBlur}
                disabled={disabled}
                className={`brutal-border ${error ? "border-destructive ring-destructive" : ""} bg-card text-card-foreground`}
            />
            {(error || field.ui?.helpText) && (
                <p className={`text-xs ${error ? "text-destructive" : "text-muted-foreground"}`}>
                    {error || field.ui?.helpText}
                </p>
            )}
        </div>
    )
}
