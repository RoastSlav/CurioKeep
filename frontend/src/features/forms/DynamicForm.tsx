"use client"

import type React from "react"

import {useEffect, useMemo, useState} from "react"
import type {FieldDef, ModuleDefinition} from "../../api/types"
import FieldRenderer from "./FieldRenderer"
import {validateAttributes, type ValidationErrors} from "./validation"
import {Button} from "../../../components/ui/button"
import {Separator} from "../../../components/ui/separator"

export type DynamicFormProps = {
    moduleDefinition?: ModuleDefinition | null
    fields?: FieldDef[]
    initialValues?: Record<string, any>
    disabled?: boolean
    submitLabel?: string
    cancelLabel?: string
    onSubmit: (attributes: Record<string, any>) => void | Promise<void>
    onCancel?: () => void
}

function groupFields(visibleFields: FieldDef[]) {
    const groups: { name?: string; fields: FieldDef[] }[] = []
    visibleFields.forEach((field) => {
        const groupName = field.ui?.group
        const existing = groups.find((g) => g.name === groupName)
        if (existing) {
            existing.fields.push(field)
        } else {
            groups.push({name: groupName, fields: [field]})
        }
    })
    return groups
}

export default function DynamicForm({
                                        moduleDefinition,
                                        fields,
                                        initialValues,
                                        disabled,
                                        submitLabel = "Save",
                                        cancelLabel = "Cancel",
                                        onSubmit,
                                        onCancel,
}: DynamicFormProps) {
    const visibleFields = useMemo(() => {
        const source = fields || moduleDefinition?.fields || []
        return source.filter((f) => !f.ui?.hidden)
    }, [fields, moduleDefinition])

    const [values, setValues] = useState<Record<string, any>>(initialValues || {})
    const [errors, setErrors] = useState<ValidationErrors>({})
    const [submitting, setSubmitting] = useState(false)

    useEffect(() => {
        setValues(initialValues || {})
    }, [initialValues])

    const handleChange = (key: string, value: any) => {
        setValues((prev) => ({...prev, [key]: value}))
        setErrors((prev) => {
            if (!prev[key]) return prev
            const {[key]: _, ...rest} = prev
            return rest
        })
    }

    const handleBlur = (field: FieldDef) => {
        const err = validateAttributes([field], {...values, [field.key]: values[field.key]})[field.key]
        setErrors((prev) => {
            if (!err) {
                const {[field.key]: _, ...rest} = prev
                return rest
            }
            return {...prev, [field.key]: err}
        })
    }

    const prepareAttributes = (): Record<string, any> => {
        const result: Record<string, any> = {}
        visibleFields.forEach((field) => {
            const raw = values[field.key]
            if (field.type === "JSON" && typeof raw === "string" && raw.trim()) {
                try {
                    result[field.key] = JSON.parse(raw)
                    return
                } catch {
                    result[field.key] = raw
                    return
                }
            }
            result[field.key] = raw
        })
        return result
    }

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault()
        const prepared = prepareAttributes()
        const validation = validateAttributes(visibleFields, prepared)
        setErrors(validation)
        if (Object.keys(validation).length) return

        setSubmitting(true)
        try {
            await onSubmit(prepared)
        } finally {
            setSubmitting(false)
        }
    }

    const groups = useMemo(() => groupFields(visibleFields), [visibleFields])

    return (
        <form onSubmit={handleSubmit}>
            <div className="flex flex-col gap-6">
                {groups.map((group, idx) => (
                    <div key={group.name ?? `group-${idx}`} className="flex flex-col gap-4">
                        {group.name && <h3 className="text-base font-bold text-foreground">{group.name}</h3>}
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            {group.fields.map((field) => (
                                <div key={field.key}>
                                    <FieldRenderer
                                        field={field}
                                        value={values[field.key]}
                                        error={errors[field.key] || undefined}
                                        disabled={disabled || submitting}
                                        onChange={(val) => handleChange(field.key, val)}
                                        onBlur={() => handleBlur(field)}
                                    />
                                </div>
                            ))}
                        </div>
                        {idx < groups.length - 1 ? <Separator className="bg-border"/> : null}
                    </div>
                ))}

                <div className="flex justify-end gap-3">
                    {onCancel && (
                        <Button
                            type="button"
                            variant="outline"
                            onClick={onCancel}
                            disabled={submitting}
                            className="brutal-border brutal-shadow-sm bg-transparent"
                        >
                            {cancelLabel}
                        </Button>
                    )}
                    <Button
                        type="submit"
                        disabled={submitting || disabled}
                        className="bg-primary text-primary-foreground brutal-border brutal-shadow-sm hover:translate-x-0.5 hover:translate-y-0.5 hover:shadow-none transition-all"
                    >
                        {submitLabel}
                    </Button>
                </div>
            </div>
        </form>
    )
}
