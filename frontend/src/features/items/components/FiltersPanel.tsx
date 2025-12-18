import { Fragment } from "react";
import { Box, Button, Chip, MenuItem, Stack, TextField, Typography } from "@mui/material";
import type { ModuleDefinition } from "../../../api/types";
import type { FieldFilter } from "../hooks/useItemsQuery";

function renderEnumFilter(
    filter: FieldFilter | undefined,
    onChange: (next: FieldFilter | undefined) => void,
    enumValues: { key: string; label: string }[]
) {
    const values = filter?.type === "enum" ? filter.values : [];
    return (
        <TextField
            select
            fullWidth
            size="small"
            label="Select values"
            value={values}
            SelectProps={{ multiple: true, renderValue: (selected) => (selected as string[]).join(", ") }}
            onChange={(e) =>
                onChange({ type: "enum", values: Array.isArray(e.target.value) ? e.target.value.map(String) : [] })
            }
        >
            {enumValues.map((opt) => (
                <MenuItem key={opt.key} value={opt.key}>
                    <Stack direction="row" spacing={1} alignItems="center">
                        <Chip label={opt.key} size="small" />
                        <Typography variant="body2">{opt.label || opt.key}</Typography>
                    </Stack>
                </MenuItem>
            ))}
        </TextField>
    );
}

function renderTextFilter(_fieldKey: string, filter: FieldFilter | undefined, onChange: (next: FieldFilter | undefined) => void) {
    const value = filter?.type === "text" ? filter.value || "" : "";
    return (
        <TextField
            fullWidth
            size="small"
            label="Contains"
            value={value}
            onChange={(e) => onChange(e.target.value ? { type: "text", value: e.target.value } : undefined)}
        />
    );
}

function renderNumberFilter(
    _fieldKey: string,
    filter: FieldFilter | undefined,
    onChange: (next: FieldFilter | undefined) => void
) {
    const current = filter?.type === "number" ? filter : { type: "number" as const };
    return (
        <Stack direction="row" spacing={1}>
            <TextField
                label="Min"
                size="small"
                type="number"
                value={current.min ?? ""}
                onChange={(e) =>
                    onChange({
                        type: "number",
                        min: e.target.value === "" ? undefined : Number(e.target.value),
                        max: current.max,
                    })
                }
            />
            <TextField
                label="Max"
                size="small"
                type="number"
                value={current.max ?? ""}
                onChange={(e) =>
                    onChange({
                        type: "number",
                        min: current.min,
                        max: e.target.value === "" ? undefined : Number(e.target.value),
                    })
                }
            />
        </Stack>
    );
}

function renderDateFilter(
    _fieldKey: string,
    filter: FieldFilter | undefined,
    onChange: (next: FieldFilter | undefined) => void
) {
    const current = filter?.type === "date" ? filter : { type: "date" as const };
    return (
        <Stack direction="row" spacing={1}>
            <TextField
                label="From"
                size="small"
                type="date"
                InputLabelProps={{ shrink: true }}
                value={current.from ?? ""}
                onChange={(e) => onChange({ type: "date", from: e.target.value || undefined, to: current.to })}
            />
            <TextField
                label="To"
                size="small"
                type="date"
                InputLabelProps={{ shrink: true }}
                value={current.to ?? ""}
                onChange={(e) => onChange({ type: "date", from: current.from, to: e.target.value || undefined })}
            />
        </Stack>
    );
}

function renderFilterControl(
    field: ModuleDefinition["fields"][number],
    filter: FieldFilter | undefined,
    onChange: (next: FieldFilter | undefined) => void
) {
    switch (field.type) {
        case "ENUM":
            return renderEnumFilter(filter, onChange, field.enumValues || []);
        case "NUMBER":
            return renderNumberFilter(field.key, filter, onChange);
        case "DATE":
            return renderDateFilter(field.key, filter, onChange);
        default:
            return renderTextFilter(field.key, filter, onChange);
    }
}

export default function FiltersPanel({
    moduleDefinition,
    filters,
    onChange,
    onClear,
}: {
    moduleDefinition: ModuleDefinition | null | undefined;
    filters: Record<string, FieldFilter> | undefined;
    onChange: (next: Record<string, FieldFilter> | undefined) => void;
    onClear?: () => void;
}) {
    if (!moduleDefinition) return null;
    const filterableFields = (moduleDefinition.fields || []).filter((f) => f.flags?.filterable);
    if (!filterableFields.length) return null;

    const updateFilter = (key: string, next: FieldFilter | undefined) => {
        const current = filters || {};
        if (!next) {
            const { [key]: _, ...rest } = current;
            onChange(Object.keys(rest).length ? rest : undefined);
            return;
        }
        onChange({ ...current, [key]: next });
    };

    return (
        <Stack spacing={2} padding={2} border={1} borderColor="divider" borderRadius={1}>
            <Stack direction="row" alignItems="center" justifyContent="space-between">
                <Typography variant="subtitle2">Filters</Typography>
                {onClear && (
                    <Button size="small" onClick={onClear} disabled={!filters || !Object.keys(filters).length}>
                        Clear
                    </Button>
                )}
            </Stack>
            {filterableFields.map((field) => (
                <Fragment key={field.key}>
                    <Stack spacing={1}>
                        <Typography variant="body2" fontWeight={600}>
                            {field.label || field.key}
                        </Typography>
                        <Box>{renderFilterControl(field, filters?.[field.key], (next) => updateFilter(field.key, next))}</Box>
                    </Stack>
                </Fragment>
            ))}
        </Stack>
    );
}
