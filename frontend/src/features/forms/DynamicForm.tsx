import { useEffect, useMemo, useState } from "react";
import { Box, Button, Divider, Stack, Typography } from "@mui/material";
import type { FieldDef, ModuleDefinition } from "../../api/types";
import FieldRenderer from "./FieldRenderer";
import { validateAttributes, type ValidationErrors } from "./validation";

export type DynamicFormProps = {
    moduleDefinition?: ModuleDefinition | null;
    fields?: FieldDef[];
    initialValues?: Record<string, any>;
    disabled?: boolean;
    submitLabel?: string;
    cancelLabel?: string;
    onSubmit: (attributes: Record<string, any>) => void | Promise<void>;
    onCancel?: () => void;
};

function groupFields(visibleFields: FieldDef[]) {
    const groups: { name?: string; fields: FieldDef[] }[] = [];
    visibleFields.forEach((field) => {
        const groupName = field.ui?.group;
        const existing = groups.find((g) => g.name === groupName);
        if (existing) {
            existing.fields.push(field);
        } else {
            groups.push({ name: groupName, fields: [field] });
        }
    });
    return groups;
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
        const source = fields || moduleDefinition?.fields || [];
        return source.filter((f) => !f.ui?.hidden);
    }, [fields, moduleDefinition]);

    const [values, setValues] = useState<Record<string, any>>(initialValues || {});
    const [errors, setErrors] = useState<ValidationErrors>({});
    const [submitting, setSubmitting] = useState(false);

    useEffect(() => {
        setValues(initialValues || {});
    }, [initialValues]);

    const handleChange = (key: string, value: any) => {
        setValues((prev) => ({ ...prev, [key]: value }));
        setErrors((prev) => {
            if (!prev[key]) return prev;
            const { [key]: _, ...rest } = prev;
            return rest;
        });
    };

    const handleBlur = (field: FieldDef) => {
        const err = validateAttributes([field], { ...values, [field.key]: values[field.key] })[field.key];
        setErrors((prev) => {
            if (!err) {
                const { [field.key]: _, ...rest } = prev;
                return rest;
            }
            return { ...prev, [field.key]: err };
        });
    };

    const prepareAttributes = (): Record<string, any> => {
        const result: Record<string, any> = {};
        visibleFields.forEach((field) => {
            const raw = values[field.key];
            if (field.type === "JSON" && typeof raw === "string" && raw.trim()) {
                try {
                    result[field.key] = JSON.parse(raw);
                    return;
                } catch {
                    result[field.key] = raw; // keep string; validation will flag
                    return;
                }
            }
            result[field.key] = raw;
        });
        return result;
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        const prepared = prepareAttributes();
        const validation = validateAttributes(visibleFields, prepared);
        setErrors(validation);
        if (Object.keys(validation).length) return;

        setSubmitting(true);
        try {
            await onSubmit(prepared);
        } finally {
            setSubmitting(false);
        }
    };

    const groups = useMemo(() => groupFields(visibleFields), [visibleFields]);

    return (
        <form onSubmit={handleSubmit}>
            <Stack spacing={3}>
                {groups.map((group, idx) => (
                    <Stack key={group.name ?? `group-${idx}`} spacing={2}>
                        {group.name && (
                            <Typography variant="subtitle1" fontWeight={700}>
                                {group.name}
                            </Typography>
                        )}
                        <Stack spacing={2}>
                            {group.fields.map((field) => (
                                <Box key={field.key}>
                                    <FieldRenderer
                                        field={field}
                                        value={values[field.key]}
                                        error={errors[field.key] || undefined}
                                        disabled={disabled || submitting}
                                        onChange={(val) => handleChange(field.key, val)}
                                        onBlur={() => handleBlur(field)}
                                    />
                                </Box>
                            ))}
                        </Stack>
                        {idx < groups.length - 1 ? <Divider /> : null}
                    </Stack>
                ))}

                <Stack direction="row" spacing={2} justifyContent="flex-end">
                    {onCancel && (
                        <Button variant="outlined" onClick={onCancel} disabled={submitting}>
                            {cancelLabel}
                        </Button>
                    )}
                    <Button type="submit" variant="contained" disabled={submitting || disabled}>
                        {submitLabel}
                    </Button>
                </Stack>
            </Stack>
        </form>
    );
}
