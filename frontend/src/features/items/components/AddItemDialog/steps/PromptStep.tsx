import { Typography } from "@mui/material";
import type { FieldDef } from "../../../../../api/types";
import DynamicForm from "../../../../forms/DynamicForm";

export default function PromptStep({
    field,
    values,
    onSubmit,
    onCancel,
}: {
    field: FieldDef;
    values: Record<string, any>;
    onSubmit: (values: Record<string, any>) => void | Promise<void>;
    onCancel?: () => void;
}) {
    return (
        <>
            <Typography variant="subtitle1" fontWeight={700} gutterBottom>
                {field.label || field.key}
            </Typography>
            <DynamicForm
                fields={[field]}
                initialValues={values}
                onSubmit={(attrs: Record<string, any>) => onSubmit(attrs)}
                onCancel={onCancel}
                submitLabel="Next"
            />
        </>
    );
}
