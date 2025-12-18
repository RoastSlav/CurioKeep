import { TextField } from "@mui/material";
import type { FieldDef } from "../../../api/types";

export default function JsonFieldField({
    field,
    value,
    error,
    disabled,
    onChange,
    onBlur,
}: {
    field: FieldDef;
    value: any;
    error?: string;
    disabled?: boolean;
    onChange: (value: any) => void;
    onBlur?: () => void;
}) {
    const displayValue = typeof value === "string" ? value : value ? JSON.stringify(value, null, 2) : "";

    return (
        <TextField
            label={field.label || field.key}
            value={displayValue}
            placeholder={field.ui?.placeholder}
            helperText={error || field.ui?.helpText}
            error={Boolean(error)}
            required={Boolean(field.flags?.required)}
            onChange={(e) => onChange(e.target.value)}
            onBlur={onBlur}
            fullWidth
            multiline
            minRows={3}
            disabled={disabled}
        />
    );
}
