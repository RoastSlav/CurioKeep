import { Avatar, Card, CardContent, CardMedia, Chip, Stack, Typography } from "@mui/material";
import type { Item } from "../../../api/types";

export default function ItemCard({ item }: { item: Item }) {
    const firstIdentifier = item.identifiers?.[0];
    const imageUrl = item.attributes?.providerImageUrl as string | undefined;
    const title = (item.attributes?.title as string | undefined) || item.id;

    return (
        <Card variant="outlined" sx={{ height: "100%" }}>
            {imageUrl ? (
                    <CardMedia
                        component="img"
                        image={imageUrl}
                        alt={title}
                        sx={{ height: 160, objectFit: "cover", borderBottom: 1, borderColor: "divider" }}
                    />
            ) : (
                    <Stack alignItems="center" justifyContent="center" sx={{ height: 160, borderBottom: 1, borderColor: "divider" }}>
                        <Avatar sx={{ width: 72, height: 72 }}>{(title || "?").substring(0, 1)}</Avatar>
                    </Stack>
            )}
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
