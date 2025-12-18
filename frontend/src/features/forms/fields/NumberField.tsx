import { TextField } from "@mui/material";
import type { FieldDef } from "../../../api/types";

export default function NumberFieldField({
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
    const constraints = field.constraints || {};
    const handleChange = (next: string) => {
        if (next === "") {
            onChange(undefined);
            return;
        }
        const parsed = Number(next);
        onChange(Number.isNaN(parsed) ? next : parsed);
    };

    return (
        <TextField
            type="number"
            label={field.label || field.key}
            value={value ?? ""}
            placeholder={field.ui?.placeholder}
            helperText={error || field.ui?.helpText}
            error={Boolean(error)}
            required={Boolean(field.flags?.required)}
            onChange={(e) => handleChange(e.target.value)}
            onBlur={onBlur}
            inputProps={{ min: constraints.min, max: constraints.max }}
            fullWidth
            disabled={disabled}
        />
    );
}
