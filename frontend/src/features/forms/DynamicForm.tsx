import type React from "react"

import {useEffect, useMemo, useState} from "react"
import type { FieldContract, ModuleContract } from "@/features/modules/moduleTypes";
import type { Attributes } from "@/features/items/itemTypes";
import FieldRenderer from "./FieldRenderer"
import {omitKey} from "@/lib/utils"
import {validateAttributes, type ValidationErrors} from "./validation"
import {Badge} from "@/components/ui/badge"
import {Button} from "@/components/ui/button"
import {Separator} from "@/components/ui/separator"

export type DynamicFormProps = {
    moduleDefinition?: ModuleContract | null
    fields?: FieldContract[]
    initialValues?: Attributes
    disabled?: boolean
    submitLabel?: string
    cancelLabel?: string
    onSubmit: (attributes: Attributes) => void | Promise<void>
    onCancel?: () => void
}

function hasValue(value: unknown): boolean {
    if (value === null || value === undefined) return false
    if (typeof value === "string") return value.trim() !== ""
    if (Array.isArray(value)) return value.length > 0
    return true
}

/** A number typed or suggested as text is saved as a number, which is what the server accepts for a NUMBER field. */
function coerceForSave(field: FieldContract, value: unknown): unknown {
    if (field.type === "NUMBER" && typeof value === "string" && value.trim() !== "" && !Number.isNaN(Number(value))) {
        return Number(value)
    }
    return value
}

function groupFields(visibleFields: FieldContract[]) {
    const groups: { name?: string; fields: FieldContract[] }[] = []
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
    const allFields = useMemo(() => fields || moduleDefinition?.fields || [], [fields, moduleDefinition])

    // Retired (inactive) fields are not offered. A deprecated field is offered only while this item still has a value in it,
    // so the user can move that value to the field that replaces it; new items never see it.
    const currentFields = useMemo(() => allFields.filter((f) => !f.ui?.hidden && f.active !== false && !f.deprecated), [allFields])
    const deprecatedFields = useMemo(
        () => allFields.filter((f) => !f.ui?.hidden && f.active !== false && f.deprecated && hasValue(initialValues?.[f.key])),
        [allFields, initialValues],
    )
    const visibleFields = useMemo(() => [...currentFields, ...deprecatedFields], [currentFields, deprecatedFields])

    const [values, setValues] = useState<Attributes>(initialValues || {})
    const [errors, setErrors] = useState<ValidationErrors>({})
    const [submitting, setSubmitting] = useState(false)

    useEffect(() => {
        setValues(initialValues || {})
    }, [initialValues])

    const handleChange = (key: string, value: unknown) => {
        setValues((prev) => ({...prev, [key]: value}))
        setErrors((prev) => {
            if (!prev[key]) return prev
            return omitKey(prev, key)
        })
    }

    const handleBlur = (field: FieldContract) => {
        const err = validateAttributes([field], {...values, [field.key]: values[field.key]})[field.key]
        setErrors((prev) => {
            if (!err) {
                return omitKey(prev, field.key)
            }
            return {...prev, [field.key]: err}
        })
    }

    const moveToReplacement = (from: string, to: string) => {
        setValues((prev) => ({...prev, [to]: prev[from], [from]: undefined}))
        setErrors((prev) => omitKey(omitKey(prev, from), to))
    }

    const prepareAttributes = (): Attributes => {
        // Values the form does not show (hidden fields, fields a newer module version removed) are kept as they are. The cover
        // is managed on its own, so it is not sent back from here.
        const result: Attributes = omitKey(initialValues || {}, "providerImageUrl")
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
            result[field.key] = coerceForSave(field, raw)
        })
        return result
    }

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault()
        const prepared = prepareAttributes()
        // A deprecated field cannot be required: the module no longer asks for it.
        const validation = validateAttributes(visibleFields.map((f) => (f.deprecated ? {...f, required: false} : f)), prepared)
        setErrors(validation)
        if (Object.keys(validation).length) return

        setSubmitting(true)
        try {
            await onSubmit(prepared)
        } finally {
            setSubmitting(false)
        }
    }

    const groups = useMemo(() => groupFields(currentFields), [currentFields])

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

                {deprecatedFields.length > 0 && (
                    <section className="flex flex-col gap-4 border-2 border-dashed border-border p-4" aria-labelledby="deprecated-fields-heading">
                        <div>
                            <h3 id="deprecated-fields-heading" className="text-base font-bold text-foreground">
                                Deprecated fields
                            </h3>
                            <p className="text-sm text-muted-foreground">
                                The module no longer uses these. Move each value to the field that replaces it; if you leave it, the value is kept
                                until a newer version of the module removes the field.
                            </p>
                        </div>
                        {deprecatedFields.map((field) => {
                            const target = field.replacedBy ? allFields.find((f) => f.key === field.replacedBy) : undefined
                            const targetFree = target !== undefined && !hasValue(values[target.key])
                            return (
                                <div key={field.key} className="flex flex-col gap-2">
                                    <div className="flex items-center gap-2">
                                        <Badge variant="outline">Deprecated</Badge>
                                        {target && (
                                            <span className="text-sm text-muted-foreground">Replaced by {target.label || target.key}</span>
                                        )}
                                    </div>
                                    <FieldRenderer
                                        field={field}
                                        value={values[field.key]}
                                        error={errors[field.key] || undefined}
                                        disabled={disabled || submitting}
                                        onChange={(val) => handleChange(field.key, val)}
                                        onBlur={() => handleBlur(field)}
                                    />
                                    {target && (
                                        <div className="flex items-center gap-3">
                                            <Button type="button" size="sm" variant="outline" disabled={!targetFree || disabled || submitting} onClick={() => moveToReplacement(field.key, target.key)}>
                                                Move to {target.label || target.key}
                                            </Button>
                                            {!targetFree && hasValue(values[target.key]) && (
                                                <span className="text-sm text-muted-foreground">{target.label || target.key} already has a value.</span>
                                            )}
                                        </div>
                                    )}
                                </div>
                            )
                        })}
                    </section>
                )}

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
