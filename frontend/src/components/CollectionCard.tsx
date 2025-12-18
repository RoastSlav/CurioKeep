import { Card, CardActionArea, CardContent, Chip, Stack, Typography } from "@mui/material";
import { Link as RouterLink } from "react-router-dom";
import type { Collection } from "../api/types";

export default function CollectionCard({ collection }: { collection: Collection }) {
    const isOwner = collection.role?.toUpperCase() === "OWNER";

    const stats = [] as { label: string; value: number }[];
    if (collection.itemsCount !== undefined) {
        stats.push({ label: "Items", value: collection.itemsCount });
    }
    if (collection.modulesCount !== undefined) {
        stats.push({ label: "Modules", value: collection.modulesCount });
    }

    return (
        <Card variant="outlined" sx={{ height: "100%" }}>
            <CardActionArea
                component={RouterLink}
                to={`/collections/${collection.id}`}
                sx={{ height: "100%", display: "flex", flexDirection: "column", alignItems: "stretch", textAlign: "left" }}
            >
                <CardContent sx={{ flexGrow: 1 }}>
                    <Stack spacing={1.5}>
                        <Stack direction="row" spacing={1} alignItems="center" justifyContent="space-between">
                            <Typography variant="h6" fontWeight={700} lineHeight={1.2}>
                                {collection.name}
                            </Typography>
                            <Chip
                                size="small"
                                label={isOwner ? "Owner" : "Shared"}
                                color={isOwner ? "primary" : "default"}
                                variant={isOwner ? "filled" : "outlined"}
                            />
                        </Stack>
                        {collection.description && (
                            <Typography variant="body2" color="text.secondary">
                                {collection.description}
                            </Typography>
                        )}
                        {stats.length ? (
                            <Stack direction={{ xs: "column", sm: "row" }} spacing={1.5} alignItems="flex-start">
                                {stats.map((stat) => (
                                    <Stack key={stat.label} spacing={0.25}>
                                        <Typography variant="caption" color="text.secondary">
                                            {stat.label}
                                        </Typography>
                                        <Typography variant="subtitle2" fontWeight={600}>
                                            {stat.value}
                                        </Typography>
                                    </Stack>
                                ))}
                            </Stack>
                        ) : (
                            <Typography variant="caption" color="text.secondary">
                                ID: {collection.id}
                            </Typography>
                        )}
                    </Stack>
                </CardContent>
            </CardActionArea>
        </Card>
    );
}
