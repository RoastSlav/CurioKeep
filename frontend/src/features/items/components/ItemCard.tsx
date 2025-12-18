import { Card, CardContent, Chip, Stack, Typography } from "@mui/material";
import type { Item } from "../../../api/types";

export default function ItemCard({ item }: { item: Item }) {
    const firstIdentifier = item.identifiers?.[0];

    return (
        <Card variant="outlined" sx={{ height: "100%" }}>
            <CardContent>
                <Stack spacing={1.25}>
                    <Stack direction="row" justifyContent="space-between" alignItems="center">
                        <Typography variant="subtitle1" fontWeight={700} noWrap>
                            {item.id}
                        </Typography>
                        <Chip size="small" label={item.stateKey} />
                    </Stack>
                    {firstIdentifier && (
                        <Typography variant="body2" color="text.secondary">
                            {firstIdentifier.type}: {firstIdentifier.value}
                        </Typography>
                    )}
                    <Stack direction="row" spacing={2}>
                        <Typography variant="caption" color="text.secondary">
                            Created: {item.createdAt || ""}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                            Updated: {item.updatedAt || ""}
                        </Typography>
                    </Stack>
                </Stack>
            </CardContent>
        </Card>
    );
}
