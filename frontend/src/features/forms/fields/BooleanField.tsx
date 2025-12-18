import { Checkbox, FormControlLabel, FormHelperText, Stack, Typography } from "@mui/material";
import type { FieldDef } from "../../../api/types";

export default function BooleanFieldField({
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
        <Stack spacing={0.5}>
            <FormControlLabel
                control={
                    <Checkbox
                        checked={Boolean(value)}
                        onChange={(e) => onChange(e.target.checked)}
                        onBlur={onBlur}
                        disabled={disabled}
                    />
                }
                label={<Typography variant="body2">{field.label || field.key}</Typography>}
            />
            {(field.ui?.helpText || error) && (
                <FormHelperText error={Boolean(error)}>{error || field.ui?.helpText}</FormHelperText>
            )}
        </Stack>
    );
}
