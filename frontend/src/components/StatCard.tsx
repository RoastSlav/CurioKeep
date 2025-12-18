import { Card, CardContent, Skeleton, Stack, Typography } from "@mui/material";
import type { ReactNode } from "react";

export default function StatCard({
    label,
    value,
    loading,
    hint,
}: {
    label: string;
    value: ReactNode;
    loading?: boolean;
    hint?: ReactNode;
}) {
    return (
        <Card variant="outlined" sx={{ height: "100%" }}>
            <CardContent>
                <Stack spacing={0.5}>
                    <Typography variant="body2" color="text.secondary">
                        {label}
                    </Typography>
                    {loading ? (
                        <Skeleton width={80} height={32} />
                    ) : (
                        <Typography variant="h4" fontWeight={700} lineHeight={1.1}>
                            {value}
                        </Typography>
                    )}
                    {hint && (
                        <Typography variant="caption" color="text.secondary">
                            {hint}
                        </Typography>
                    )}
                </Stack>
            </CardContent>
        </Card>
    );
}
