import { Chip, MenuItem, Stack, TextField, Typography } from "@mui/material";
import type { FieldDef } from "../../../api/types";

export default function EnumFieldField({
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
    const multi = Boolean(field.constraints?.multi);
    const current = multi ? (Array.isArray(value) ? value : []) : value ?? "";

    return (
        <TextField
            select
            label={field.label || field.key}
            value={current}
            placeholder={field.ui?.placeholder}
            helperText={error || field.ui?.helpText}
            error={Boolean(error)}
            required={Boolean(field.flags?.required)}
            onChange={(e) => onChange(e.target.value)}
            onBlur={onBlur}
            SelectProps={multi ? { multiple: true, renderValue: (selected) => (selected as string[]).join(", ") } : undefined}
            fullWidth
            disabled={disabled}
        >
            {(field.enumValues || []).map((opt) => (
                <MenuItem key={opt.key} value={opt.key}>
                    <Stack direction="row" spacing={1} alignItems="center">
                        {multi && <Chip size="small" label={opt.key} />}
                        <Typography variant="body2">{opt.label || opt.key}</Typography>
                    </Stack>
                </MenuItem>
            ))}
        </TextField>
    );
}
