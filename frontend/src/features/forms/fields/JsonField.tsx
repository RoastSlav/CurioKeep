"use client"

import {Textarea} from "../../../../components/ui/textarea"
import {Label} from "../../../../components/ui/label"
import type {FieldDef} from "../../../api/types"

export default function JsonFieldField({
                                           field,
                                           value,
                                           error,
                                           disabled,
                                           onChange,
                                           onBlur,
}: {
    field: FieldDef
    value: any
    error?: string
    disabled?: boolean
    onChange: (value: any) => void
    onBlur?: () => void
}) {
    const displayValue = typeof value === "string" ? value : value ? JSON.stringify(value, null, 2) : ""
    const id = `field-${field.key}`

    return (
        <div className="flex flex-col gap-2">
            <Label htmlFor={id} className="text-sm font-semibold text-foreground">
                {field.label || field.key}
                {field.flags?.required && <span className="text-destructive ml-1">*</span>}
            </Label>
            <Textarea
                id={id}
                value={displayValue}
                placeholder={field.ui?.placeholder || '{\n  "key": "value"\n}'}
                onChange={(e) => onChange(e.target.value)}
                onBlur={onBlur}
                disabled={disabled}
                rows={5}
                className={`font-mono text-sm brutal-border ${error ? "border-destructive ring-destructive" : ""} bg-card text-card-foreground`}
            />
            {(error || field.ui?.helpText) && (
                <p className={`text-xs ${error ? "text-destructive" : "text-muted-foreground"}`}>
                    {error || field.ui?.helpText}
                </p>
            )}
        </div>
    )
}
