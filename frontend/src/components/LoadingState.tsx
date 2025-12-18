import { CircularProgress, Stack, Typography } from "@mui/material";
import type { ReactNode } from "react";

export default function LoadingState({ message }: { message?: ReactNode }) {
    return (
        <Stack spacing={1} alignItems="center" justifyContent="center" sx={{ py: 6 }}>
            <CircularProgress />
            {message && (
                <Typography variant="body2" color="text.secondary">
                    {message}
                </Typography>
            )}
        </Stack>
    );
}
