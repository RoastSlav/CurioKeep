import { Alert, Box, Button, Card, CardContent, Stack, TextField, Typography } from "@mui/material";
import { useState } from "react";
import type { FormEvent } from "react";
import { Navigate, useLocation, useNavigate, useSearchParams } from "react-router-dom";
import { ApiError, isApiError } from "../api/errors";
import { useAuth } from "../auth/useAuth";
import { useSetupStatus } from "../components/AppGate";
import LoadingState from "../components/LoadingState";
import ErrorState from "../components/ErrorState";
import { useToast } from "../components/Toasts";

export default function LoginPage() {
    const navigate = useNavigate();
    const location = useLocation();
    const [params] = useSearchParams();
    const { user, loading: authLoading, error: authError, login } = useAuth();
    const { setupRequired, loading: setupLoading, error: setupError, reload: reloadSetup } = useSetupStatus();
    const { showToast } = useToast();

    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [submitting, setSubmitting] = useState(false);
    const [submitError, setSubmitError] = useState<string | null>(null);

    if (setupLoading) return <LoadingState message="Checking setup..." />;
    if (setupError) return <ErrorState title="Setup check failed" message={setupError} onRetry={reloadSetup} />;
    if (setupRequired) return <Navigate to="/setup" replace />;

    if (authLoading) return <LoadingState message="Checking session..." />;
    if (user) return <Navigate to="/" replace />;

    const onSubmit = async (evt: FormEvent) => {
        evt.preventDefault();
        setSubmitError(null);
        setSubmitting(true);
        try {
            await login(email, password);
            showToast("Signed in", "success");
            const returnTo = params.get("returnTo") || "/";
            navigate(returnTo, { replace: true, state: { from: location } });
        } catch (err) {
            const apiErr = err as ApiError;
            setSubmitError(isApiError(apiErr) ? apiErr.message : "Login failed");
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <Box maxWidth={420} mx="auto" mt={6}>
            <Card>
                <CardContent>
                    <Stack component="form" spacing={2} onSubmit={onSubmit}>
                        <Typography variant="h5" fontWeight={700}>Sign in</Typography>
                        {authError && <Alert severity="error">{authError}</Alert>}
                        {submitError && <Alert severity="error">{submitError}</Alert>}
                        <TextField
                            label="Email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required
                            autoFocus
                        />
                        <TextField
                            label="Password"
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                        />
                        <Button type="submit" variant="contained" disabled={submitting}>
                            {submitting ? "Signing in..." : "Sign in"}
                        </Button>
                    </Stack>
                </CardContent>
            </Card>
        </Box>
    );
}
