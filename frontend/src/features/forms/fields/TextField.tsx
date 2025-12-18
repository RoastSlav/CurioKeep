import { TextField } from "@mui/material";
import type { FieldDef } from "../../../api/types";

export default function TextFieldField({
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
    return (
        <TextField
            label={field.label || field.key}
            value={value ?? ""}
            placeholder={field.ui?.placeholder}
            helperText={error || field.ui?.helpText}
            error={Boolean(error)}
            required={Boolean(field.flags?.required)}
            onChange={(e) => onChange(e.target.value)}
            onBlur={onBlur}
            fullWidth
            disabled={disabled}
        />
    );
}
