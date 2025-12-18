import { Chip, Paper, Stack, Typography } from "@mui/material";
import type { Item, ModuleStateDef } from "../../../api/types";

export default function StatsPanel({
    items,
    states,
    activeState,
    onFilterChange,
}: {
    items: Item[];
    states?: ModuleStateDef[];
    activeState?: string | null;
    onFilterChange?: (stateKey: string | null) => void;
}) {
    const counts = items.reduce<Record<string, number>>((acc, item) => {
        acc[item.stateKey] = (acc[item.stateKey] || 0) + 1;
        return acc;
    }, {});

    const total = items.length;
    const orderedStates = states && states.length ? states : Object.keys(counts).map((key) => ({ key } as ModuleStateDef));

    const handleSelect = (stateKey: string | null) => {
        if (!onFilterChange) return;
        onFilterChange(stateKey === activeState ? null : stateKey);
    };

    return (
        <Paper variant="outlined">
            <Stack direction="row" spacing={1.5} alignItems="center" p={2} flexWrap="wrap" useFlexGap>
                <Typography variant="subtitle1" fontWeight={700} sx={{ minWidth: 120 }}>
                    Stats
                </Typography>
                <Chip
                    label={`Total: ${total}`}
                    color={activeState ? undefined : "primary"}
                    variant={activeState ? "outlined" : "filled"}
                    onClick={() => handleSelect(null)}
                    sx={{ cursor: onFilterChange ? "pointer" : "default" }}
                />
                {orderedStates.map((state) => {
                    const selected = activeState === state.key;
                    return (
                        <Chip
                            key={state.key}
                            label={`${state.label || state.key}: ${counts[state.key] || 0}`}
                            color={selected ? "primary" : "default"}
                            variant={selected ? "filled" : "outlined"}
                            onClick={() => handleSelect(state.key)}
                            sx={{ cursor: onFilterChange ? "pointer" : "default" }}
                        />
                    );
                })}
            </Stack>
        </Paper>
    );
}
