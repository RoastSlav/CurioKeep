import { Chip, Stack, Typography } from "@mui/material";
import type { ModuleStateDef } from "../../../api/types";

export default function StateChips({
    states,
    selected,
    onChange,
}: {
    states: ModuleStateDef[];
    selected: string[];
    onChange: (next: string[]) => void;
}) {
    if (!states.length) return null;
    const selectedSet = new Set(selected.map((s) => s.toUpperCase()));

    const toggle = (key: string) => {
        const upper = key.toUpperCase();
        const next = new Set(selectedSet);
        if (next.has(upper)) {
            next.delete(upper);
        } else {
            next.add(upper);
        }
        onChange(Array.from(next));
    };

    return (
        <Stack direction="row" spacing={1} flexWrap="wrap" alignItems="center">
            <Typography variant="body2" color="text.secondary">
                States:
            </Typography>
            {states.map((state) => {
                const active = selectedSet.has(state.key.toUpperCase());
                return (
                    <Chip
                        key={state.key}
                        label={state.label || state.key}
                        color={active ? "primary" : "default"}
                        variant={active ? "filled" : "outlined"}
                        size="small"
                        onClick={() => toggle(state.key)}
                    />
                );
            })}
        </Stack>
    );
}
