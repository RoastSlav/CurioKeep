import { Button, Card, CardActions, CardContent, Chip, Stack, Typography } from "@mui/material";
import { Link as RouterLink } from "react-router-dom";
import type { Collection } from "../api/types";

export default function CollectionCard({ collection }: { collection: Collection }) {
    const isOwner = collection.role?.toUpperCase() === "OWNER";

    return (
        <Card variant="outlined" sx={{ height: "100%" }}>
            <CardContent>
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
                    <Stack direction="row" spacing={2} alignItems="center">
                        {collection.modulesCount !== undefined && (
                            <Typography variant="caption" color="text.secondary">
                                Modules: {collection.modulesCount}
                            </Typography>
                        )}
                        {collection.itemsCount !== undefined && (
                            <Typography variant="caption" color="text.secondary">
                                Items: {collection.itemsCount}
                            </Typography>
                        )}
                    </Stack>
                </Stack>
            </CardContent>
            <CardActions sx={{ px: 2, pb: 2 }}>
                <Button component={RouterLink} to={`/collections/${collection.id}`} variant="contained" size="small">
                    Open
                </Button>
                <Button component={RouterLink} to={`/collections/${collection.id}`} variant="text" size="small">
                    Settings
                </Button>
            </CardActions>
        </Card>
    );
}
