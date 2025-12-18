import { Autocomplete, Chip, TextField } from "@mui/material";
import type { FieldDef } from "../../../api/types";

export default function TagsFieldField({
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
    const tags = Array.isArray(value) ? value : value ? [String(value)] : [];

    return (
        <Autocomplete
            multiple
            freeSolo
            options={[]}
            value={tags}
            onChange={(_, next) => onChange(next)}
            onBlur={onBlur}
            disabled={disabled}
            renderTags={(tagValue, getTagProps) =>
                tagValue.map((option, index) => <Chip variant="outlined" label={option} {...getTagProps({ index })} />)
            }
            renderInput={(params) => (
                <TextField
                    {...params}
                    label={field.label || field.key}
                    placeholder={field.ui?.placeholder}
                    helperText={error || field.ui?.helpText}
                    error={Boolean(error)}
                    required={Boolean(field.flags?.required)}
                />
            )}
        />
    );
}
