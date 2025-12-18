import { Chip, Paper, Stack, Typography } from "@mui/material";
import type { Item, ModuleStateDef } from "../../../api/types";

export default function StatsPanel({ items, states }: { items: Item[]; states?: ModuleStateDef[] }) {
    const counts = items.reduce<Record<string, number>>((acc, item) => {
        acc[item.stateKey] = (acc[item.stateKey] || 0) + 1;
        return acc;
    }, {});

    const total = items.length;
    const orderedStates = states && states.length ? states : Object.keys(counts).map((key) => ({ key } as ModuleStateDef));

    return (
        <Paper variant="outlined">
            <Stack direction="row" spacing={1.5} alignItems="center" p={2} flexWrap="wrap" useFlexGap>
                <Typography variant="subtitle1" fontWeight={700} sx={{ minWidth: 120 }}>
                    Stats
                </Typography>
                <Chip label={`Total: ${total}`} color="primary" />
                {orderedStates.map((state) => (
                    <Chip
                        key={state.key}
                        label={`${state.label || state.key}: ${counts[state.key] || 0}`}
                        variant="outlined"
                    />
                ))}
            </Stack>
        </Paper>
    );
}
