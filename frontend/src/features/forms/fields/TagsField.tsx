"use client"

import {useState, type KeyboardEvent} from "react"
import {Input} from "../../../../components/ui/input"
import {Label} from "../../../../components/ui/label"
import {Badge} from "../../../../components/ui/badge"
import {X} from "lucide-react"
import type {FieldDef} from "../../../api/types"

export default function TagsFieldField({
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
    const [inputValue, setInputValue] = useState("")
    const tags = Array.isArray(value) ? value : value ? [String(value)] : []
    const id = `field-${field.key}`

    const addTag = (tag: string) => {
        const trimmed = tag.trim()
        if (trimmed && !tags.includes(trimmed)) {
            onChange([...tags, trimmed])
        }
        setInputValue("")
    }

    const removeTag = (tagToRemove: string) => {
        onChange(tags.filter((t: string) => t !== tagToRemove))
    }

    const handleKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
        if (e.key === "Enter" || e.key === ",") {
            e.preventDefault()
            addTag(inputValue)
        } else if (e.key === "Backspace" && !inputValue && tags.length > 0) {
            removeTag(tags[tags.length - 1])
        }
    }

    return (
        <div className="flex flex-col gap-2">
            <Label htmlFor={id} className="text-sm font-semibold text-foreground">
                {field.label || field.key}
                {field.flags?.required && <span className="text-destructive ml-1">*</span>}
            </Label>
            <div
                className={`flex flex-wrap gap-2 p-2 brutal-border rounded-md bg-card min-h-[44px] ${
                    error ? "border-destructive" : ""
                } ${disabled ? "opacity-50" : ""}`}
            >
                {tags.map((tag: string) => (
                    <Badge
                        key={tag}
                        variant="secondary"
                        className="brutal-border bg-secondary text-secondary-foreground flex items-center gap-1"
                    >
                        {tag}
                        {!disabled && (
                            <X className="h-3 w-3 cursor-pointer hover:text-destructive"
                               onClick={() => removeTag(tag)}/>
                        )}
                    </Badge>
                ))}
                <Input
                    id={id}
                    type="text"
                    value={inputValue}
                    placeholder={tags.length === 0 ? field.ui?.placeholder || "Type and press Enter..." : ""}
                    onChange={(e) => setInputValue(e.target.value)}
                    onKeyDown={handleKeyDown}
                    onBlur={() => {
                        if (inputValue) addTag(inputValue)
                        onBlur?.()
                    }}
                    disabled={disabled}
                    className="flex-1 min-w-[120px] border-0 shadow-none focus-visible:ring-0 p-0 h-auto bg-transparent"
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
