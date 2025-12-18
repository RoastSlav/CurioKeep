import { MenuItem, Select, type SelectChangeEvent } from "@mui/material";
import type { ModuleStateDef } from "../../../api/types";

export default function StateDropdown({
    states,
    value,
    disabled,
    onChange,
}: {
    states: ModuleStateDef[];
    value: string;
    disabled?: boolean;
    onChange: (stateKey: string) => void;
}) {
    const handleChange = (e: SelectChangeEvent) => {
        const next = e.target.value as string;
        onChange(next);
    };

    return (
        <Select size="small" value={value} onChange={handleChange} disabled={disabled}>
            {states.map((state) => (
                <MenuItem key={state.key} value={state.key} disabled={state.deprecated === true}>
                    {state.label || state.key}
                </MenuItem>
            ))}
        </Select>
    );
}
