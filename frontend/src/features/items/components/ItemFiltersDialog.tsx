import { useState } from "react"
import { Button } from "@/components/ui/button"
import { Checkbox } from "@/components/ui/checkbox"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { omitKey } from "@/lib/utils"
import type { EnumValue, FieldContract } from "@/features/modules/moduleTypes"
import { cleanFilters, filterKindFor, type FieldFilter, type FieldFilters } from "../itemFilters"

const BOOLEAN_CHOICES: EnumValue[] = [
    { key: "true", label: "Yes" },
    { key: "false", label: "No" },
]

type ItemFiltersDialogProps = {
    open: boolean
    onOpenChange: (open: boolean) => void
    fields: FieldContract[]
    filters: FieldFilters
    onApply: (filters: FieldFilters) => void
}

export function ItemFiltersDialog({ open, onOpenChange, fields, filters, onApply }: ItemFiltersDialogProps) {
    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="max-h-[85vh] overflow-y-auto sm:max-w-lg">
                <DialogHeader>
                    <DialogTitle>Filter items</DialogTitle>
                    <DialogDescription>Only items that match all of these are listed.</DialogDescription>
                </DialogHeader>
                <FiltersForm
                    fields={fields}
                    initial={filters}
                    onCancel={() => onOpenChange(false)}
                    onApply={(next) => {
                        onApply(next)
                        onOpenChange(false)
                    }}
                />
            </DialogContent>
        </Dialog>
    )
}

// Mounted only while the dialog is open, so its draft always starts from the filters currently in effect.
function FiltersForm({ fields, initial, onApply, onCancel }: { fields: FieldContract[]; initial: FieldFilters; onApply: (filters: FieldFilters) => void; onCancel: () => void }) {
    const [draft, setDraft] = useState<FieldFilters>(initial)

    const update = (key: string, filter: FieldFilter) => setDraft((current) => ({ ...current, [key]: filter }))

    return (
        <form
            className="space-y-5"
            onSubmit={(event) => {
                event.preventDefault()
                onApply(cleanFilters(draft))
            }}
        >
            {fields.map((field) => (
                <fieldset key={field.key} className="space-y-2">
                    <legend className="text-sm font-bold">{field.label || field.key}</legend>
                    <FilterControl field={field} value={draft[field.key]} onChange={(filter) => update(field.key, filter)} />
                </fieldset>
            ))}
            <DialogFooter className="gap-2 sm:justify-between">
                <Button type="button" variant="ghost" onClick={() => setDraft((current) => omitAll(current))}>
                    Clear all
                </Button>
                <div className="flex gap-2">
                    <Button type="button" variant="outline" onClick={onCancel}>
                        Cancel
                    </Button>
                    <Button type="submit">Apply</Button>
                </div>
            </DialogFooter>
        </form>
    )
}

function omitAll(filters: FieldFilters): FieldFilters {
    return Object.keys(filters).reduce<FieldFilters>((rest, key) => omitKey(rest, key), filters)
}

function FilterControl({ field, value, onChange }: { field: FieldContract; value: FieldFilter | undefined; onChange: (filter: FieldFilter) => void }) {
    const kind = filterKindFor(field)
    const id = `filter-${field.key}`

    if (kind === "in" && (field.type === "ENUM" || field.type === "BOOLEAN")) {
        const choices = field.type === "BOOLEAN" ? BOOLEAN_CHOICES : field.enumValues
        const selected = value?.kind === "in" ? value.values : []
        return (
            <div className="grid gap-2 sm:grid-cols-2">
                {choices.map((choice) => (
                    <div key={choice.key} className="flex items-center gap-2">
                        <Checkbox
                            id={`${id}-${choice.key}`}
                            checked={selected.includes(choice.key)}
                            onCheckedChange={(checked) =>
                                onChange({ kind: "in", values: checked === true ? [...selected, choice.key] : selected.filter((v) => v !== choice.key) })
                            }
                        />
                        <Label htmlFor={`${id}-${choice.key}`}>{choice.label || choice.key}</Label>
                    </div>
                ))}
            </div>
        )
    }

    if (kind === "in") {
        return <TagsInput id={id} label={field.label || field.key} values={value?.kind === "in" ? value.values : []} onChange={(values) => onChange({ kind: "in", values })} />
    }

    if (kind === "contains") {
        return (
            <div>
                <Label htmlFor={id} className="sr-only">
                    {field.label || field.key} contains
                </Label>
                <Input id={id} placeholder="Contains…" value={value?.kind === "contains" ? value.text : ""} onChange={(event) => onChange({ kind: "contains", text: event.target.value })} />
            </div>
        )
    }

    if (kind === "range") {
        const range = value?.kind === "range" ? value : { kind: "range" as const }
        const parse = (text: string) => (text === "" ? undefined : Number(text))
        return (
            <div className="grid grid-cols-2 gap-3">
                <div>
                    <Label htmlFor={`${id}-min`}>{field.label || field.key} from</Label>
                    <Input id={`${id}-min`} type="number" value={range.min ?? ""} onChange={(event) => onChange({ ...range, min: parse(event.target.value) })} />
                </div>
                <div>
                    <Label htmlFor={`${id}-max`}>{field.label || field.key} to</Label>
                    <Input id={`${id}-max`} type="number" value={range.max ?? ""} onChange={(event) => onChange({ ...range, max: parse(event.target.value) })} />
                </div>
            </div>
        )
    }

    if (kind === "dates") {
        const dates = value?.kind === "dates" ? value : { kind: "dates" as const }
        return (
            <div className="grid grid-cols-2 gap-3">
                <div>
                    <Label htmlFor={`${id}-from`}>{field.label || field.key} from</Label>
                    <Input id={`${id}-from`} type="date" value={dates.from ?? ""} onChange={(event) => onChange({ ...dates, from: event.target.value || undefined })} />
                </div>
                <div>
                    <Label htmlFor={`${id}-to`}>{field.label || field.key} to</Label>
                    <Input id={`${id}-to`} type="date" value={dates.to ?? ""} onChange={(event) => onChange({ ...dates, to: event.target.value || undefined })} />
                </div>
            </div>
        )
    }

    return null
}

// Keeps what was typed (including a trailing comma) in local state and reports the parsed list.
function TagsInput({ id, label, values, onChange }: { id: string; label: string; values: string[]; onChange: (values: string[]) => void }) {
    const [text, setText] = useState(values.join(", "))
    return (
        <div>
            <Label htmlFor={id} className="sr-only">
                {label}, comma separated
            </Label>
            <Input
                id={id}
                placeholder="One or more, separated by commas"
                value={text}
                onChange={(event) => {
                    setText(event.target.value)
                    onChange(event.target.value.split(",").map((part) => part.trim()).filter(Boolean))
                }}
            />
        </div>
    )
}
