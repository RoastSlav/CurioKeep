import { Alert, Stack, Typography } from "@mui/material";

export default function ProvidersPage() {
    return (
        <Stack spacing={2}>
            <Typography variant="h4">Providers</Typography>
            <Alert
                severity="info"
                variant="outlined"
                sx={{
                    borderColor: (t) => t.palette.secondary.main,
                    color: (t) => t.palette.secondary.main,
                    backgroundColor: (t) => t.palette.background.default,
                }}
            >
                Provider lookup UI will live here (coming soon).
            </Alert>
        </Stack>
    );
}
