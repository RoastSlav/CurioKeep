import { Box, Button, Stack, Typography } from "@mui/material";
import type { ReactNode } from "react";

export default function EmptyState({
    title,
    description,
    actionLabel,
    onAction,
    secondary,
}: {
    title: string;
    description?: ReactNode;
    actionLabel?: string;
    onAction?: () => void;
    secondary?: ReactNode;
}) {
    return (
        <Box sx={{ border: (t) => `1px dashed ${t.palette.divider}`, borderRadius: 2, p: 4, textAlign: "center" }}>
            <Stack spacing={2} alignItems="center" justifyContent="center">
                <Typography variant="h6" fontWeight={700}>
                    {title}
                </Typography>
                {description && (
                    <Typography variant="body2" color="text.secondary" maxWidth={420}>
                        {description}
                    </Typography>
                )}
                {actionLabel && onAction && (
                    <Button variant="contained" onClick={onAction} size="medium">
                        {actionLabel}
                    </Button>
                )}
                {secondary}
            </Stack>
        </Box>
    );
}
