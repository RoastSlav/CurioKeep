"use client"

import {Checkbox} from "../../../../components/ui/checkbox"
import {Label} from "../../../../components/ui/label"
import type {FieldDef} from "../../../api/types"

export default function BooleanFieldField({
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
            <div className="flex items-center gap-3">
                <Checkbox
                    id={id}
                    checked={Boolean(value)}
                    onCheckedChange={(checked) => onChange(checked)}
                    onBlur={onBlur}
                    disabled={disabled}
                    className="brutal-border data-[state=checked]:bg-primary data-[state=checked]:text-primary-foreground"
                />
                <Label htmlFor={id} className="text-sm font-medium text-foreground cursor-pointer">
                    {field.label || field.key}
                </Label>
            </div>
            {(error || field.ui?.helpText) && (
                <p className={`text-xs ${error ? "text-destructive" : "text-muted-foreground"}`}>
                    {error || field.ui?.helpText}
                </p>
            )}
        </div>
    )
}
