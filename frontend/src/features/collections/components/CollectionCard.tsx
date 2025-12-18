import { Delete, Edit, OpenInNew } from "@mui/icons-material";
import { Box, Button, Card, CardActions, CardContent, Chip, Stack, Typography } from "@mui/material";
import { Link as RouterLink } from "react-router-dom";
import type { Collection } from "../../../api/types";

export default function CollectionCard({
    collection,
    onEdit,
    onDelete,
}: {
    collection: Collection;
    onEdit?: (collection: Collection) => void;
    onDelete?: (collection: Collection) => void;
}) {
    const role = collection.role?.toUpperCase();
    const isOwner = role === "OWNER";
    const isAdmin = role === "ADMIN" || isOwner;

    return (
        <Card variant="outlined" sx={{ height: "100%", display: "flex", flexDirection: "column" }}>
            <CardContent sx={{ flexGrow: 1 }}>
                <Stack spacing={1.25}>
                    <Stack direction="row" alignItems="center" justifyContent="space-between">
                        <Typography variant="h6" fontWeight={700} lineHeight={1.2}>
                            {collection.name}
                        </Typography>
                        <Chip label={role || ""} size="small" color={isOwner ? "primary" : "default"} variant={isOwner ? "filled" : "outlined"} />
                    </Stack>
                    {collection.description && (
                        <Typography variant="body2" color="text.secondary">
                            {collection.description}
                        </Typography>
                    )}
                    <Box>
                        <Typography variant="caption" color="text.secondary">
                            ID: {collection.id}
                        </Typography>
                    </Box>
                </Stack>
            </CardContent>
            <CardActions sx={{ px: 2, pb: 2, gap: 1, flexWrap: "wrap" }}>
                <Button
                    component={RouterLink}
                    to={`/collections/${collection.id}`}
                    size="small"
                    variant="contained"
                    endIcon={<OpenInNew fontSize="inherit" />}
                >
                    Open
                </Button>
                {isAdmin && onEdit && (
                    <Button size="small" variant="outlined" startIcon={<Edit fontSize="inherit" />} onClick={() => onEdit(collection)}>
                        Edit
                    </Button>
                )}
                {isOwner && onDelete && (
                    <Button size="small" color="error" startIcon={<Delete fontSize="inherit" />} onClick={() => onDelete(collection)}>
                        Delete
                    </Button>
                )}
            </CardActions>
        </Card>
    );
}
