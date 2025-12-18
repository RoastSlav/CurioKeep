import { Alert, Typography } from "@mui/material";
import type { FieldDef } from "../../../../../api/types";
import DynamicForm from "../../../../forms/DynamicForm";
import { useState } from "react";

export default function PromptAnyStep({
    fields,
    values,
    label,
    onSubmit,
    onCancel,
}: {
    fields: FieldDef[];
    values: Record<string, any>;
    label?: string;
    onSubmit: (values: Record<string, any>) => void | Promise<void>;
    onCancel?: () => void;
}) {
    const [error, setError] = useState<string | null>(null);

    const handleSubmit = async (attrs: Record<string, any>) => {
        const hasValue = fields.some((f) => {
            const val = attrs[f.key];
            if (val === null || val === undefined) return false;
            if (typeof val === "string") return val.trim().length > 0;
            if (Array.isArray(val)) return val.length > 0;
            return true;
        });
        if (!hasValue) {
            setError("Provide at least one value");
            return;
        }
        setError(null);
        await onSubmit(attrs);
    };

    return (
        <>
            <Typography variant="subtitle1" fontWeight={700} gutterBottom>
                {label || "Enter details"}
            </Typography>
            {error && <Alert severity="warning" sx={{ mb: 1 }}>{error}</Alert>}
            <DynamicForm
                fields={fields}
                initialValues={values}
                onSubmit={handleSubmit}
                onCancel={onCancel}
                submitLabel="Next"
            />
        </>
    );
}
