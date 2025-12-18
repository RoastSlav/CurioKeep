import { Alert, AlertTitle, Button, Stack } from "@mui/material";
import type { ReactNode } from "react";

type Props = {
    title?: string;
    message?: ReactNode;
    onRetry?: () => void;
};

export default function ErrorState({ title = "Something went wrong", message, onRetry }: Props) {
    return (
        <Alert severity="error" action={onRetry ? <Button color="inherit" size="small" onClick={onRetry}>Retry</Button> : undefined}>
            <Stack spacing={0.5}>
                <AlertTitle>{title}</AlertTitle>
                {message}
            </Stack>
        </Alert>
    );
}
