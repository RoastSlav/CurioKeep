"use client"

import {Input} from "../../../../components/ui/input"
import {Label} from "../../../../components/ui/label"
import {Link2} from "lucide-react"
import type {FieldDef} from "../../../api/types"

export default function LinkFieldField({
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
    const id = `field-${field.key}`

    return (
        <div className="flex flex-col gap-2">
            <Label htmlFor={id} className="text-sm font-semibold text-foreground">
                {field.label || field.key}
                {field.flags?.required && <span className="text-destructive ml-1">*</span>}
            </Label>
            <div className="relative">
                <Link2 className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground"/>
                <Input
                    id={id}
                    type="url"
                    value={value ?? ""}
                    placeholder={field.ui?.placeholder || "https://..."}
                    onChange={(e) => onChange(e.target.value)}
                    onBlur={onBlur}
                    disabled={disabled}
                    className={`pl-10 brutal-border ${error ? "border-destructive ring-destructive" : ""} bg-card text-card-foreground`}
        />
            </div>
            {(error || field.ui?.helpText) && (
                <p className={`text-xs ${error ? "text-destructive" : "text-muted-foreground"}`}>
                    {error || field.ui?.helpText}
                </p>
            )}
        </div>
    )
}
