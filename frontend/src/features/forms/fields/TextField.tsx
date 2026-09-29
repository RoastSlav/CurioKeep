import { toInputValue } from "../formValue"
import {Input} from "@/components/ui/input"
import {Label} from "@/components/ui/label"
import {Badge} from "@/components/ui/badge"
import type { FieldContract } from "@/features/modules/moduleTypes";

export default function TextFieldField({
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
    const hasCustomIdentifier = field.identifiers?.includes("CUSTOM")

    return (
        <div className="flex flex-col gap-2">
            <Label htmlFor={id} className="text-sm font-semibold text-foreground">
                {field.label || field.key}
                {field.required && <span className="text-destructive ml-1">*</span>}
                {field.identifiers && field.identifiers.length > 0 && (
                    <div className="inline-flex gap-1 ml-2">
                        {field.identifiers.map((idType) => (
                            <Badge key={idType} variant={idType === "CUSTOM" ? "default" : "outline"}
                                   className="text-xs">
                                {idType}
                            </Badge>
                        ))}
                    </div>
                )}
            </Label>
            <Input
                id={id}
                type="text"
                value={toInputValue(value)}
                placeholder={field.ui?.placeholder}
                onChange={(e) => onChange(e.target.value)}
                onBlur={onBlur}
                disabled={disabled}
                className={`brutal-border ${error ? "border-destructive ring-destructive" : ""} bg-card text-card-foreground`}
            />
            {(error || field.ui?.helpText) && (
                <p
                    className={`text-sm max-w-full break-words ${
                        error
                            ? "text-destructive"
                            : hasCustomIdentifier
                                ? "text-foreground font-medium bg-muted p-2 rounded border-l-4 border-primary"
                                : "text-muted-foreground"
                    }`}
                >
                    {error || field.ui?.helpText}
                </p>
            )}
        </div>
    )
}
