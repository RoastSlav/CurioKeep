import { Chip, Stack, Typography } from "@mui/material";
import type { ReactNode } from "react";
import type { Collection } from "../../../api/types";

export default function CollectionHeader({ collection, actions }: { collection: Collection; actions?: ReactNode }) {
    const role = collection.role?.toUpperCase();
    const isOwner = role === "OWNER";

    return (
        <Stack
            direction={{ xs: "column", md: "row" }}
            spacing={2}
            alignItems={{ xs: "flex-start", md: "center" }}
            justifyContent="space-between"
        >
            <Stack spacing={1.25} flex={1} minWidth={0}>
                <Stack direction="row" spacing={1} alignItems="center">
                    <Typography variant="h4" fontWeight={700} noWrap>
                        {collection.name}
                    </Typography>
                    {role && <Chip label={role} size="small" color={isOwner ? "primary" : "default"} variant={isOwner ? "filled" : "outlined"} />}
                </Stack>
                {collection.description && (
                    <Typography color="text.secondary" variant="body1">
                        {collection.description}
                    </Typography>
                )}
            </Stack>
            {actions}
        </Stack>
    );
}
