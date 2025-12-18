import { Alert, Box, Button, Card, CardContent, Stack, TextField, Typography } from "@mui/material";
import { useMemo, useState } from "react";
import type { FormEvent } from "react";
import { Navigate, useNavigate } from "react-router-dom";
import { apiFetch } from "../api/client";
import { ApiError, isApiError } from "../api/errors";
import { useSetupStatus } from "../components/AppGate";
import { useToast } from "../components/Toasts";
import LoadingState from "../components/LoadingState";
import ErrorState from "../components/ErrorState";

export default function SetupPage() {
    const navigate = useNavigate();
    const { setupRequired, loading, error, reload, setSetupRequired } = useSetupStatus();
    const { showToast } = useToast();

    const [email, setEmail] = useState("");
    const [displayName, setDisplayName] = useState("");
    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [submitting, setSubmitting] = useState(false);
    const [submitError, setSubmitError] = useState<string | null>(null);

    const emailValid = useMemo(() => /.+@.+/.test(email), [email]);

    if (loading) {
        return <LoadingState message="Checking setup..." />;
    }

    if (error) {
        return <ErrorState title="Setup check failed" message={error} onRetry={reload} />;
    }

    if (!setupRequired) {
        return <Navigate to="/login" replace />;
    }

    const onSubmit = async (evt: FormEvent) => {
        evt.preventDefault();
        setSubmitError(null);

        if (!emailValid) {
            setSubmitError("Please enter a valid email");
            return;
        }
        if (!password || !confirmPassword) {
            setSubmitError("Password is required");
            return;
        }
        if (password !== confirmPassword) {
            setSubmitError("Passwords do not match");
            return;
        }

        setSubmitting(true);
        try {
            await apiFetch("/setup/admin", { method: "POST", body: { email, password, displayName } });
            setSetupRequired(false);
            showToast("Admin account created", "success");
            navigate("/login", { replace: true });
        } catch (err) {
            const apiErr = err as ApiError;
            setSubmitError(isApiError(apiErr) ? apiErr.message : "Setup failed");
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <Box maxWidth={520} mx="auto" mt={6}>
            <Card>
                <CardContent>
                    <Stack component="form" spacing={2} onSubmit={onSubmit}>
                        <Typography variant="h5" fontWeight={700}>First-time setup</Typography>
                        <Typography variant="body2" color="text.secondary">
                            Create the initial administrator account to finish setup.
                        </Typography>
                        {submitError && <Alert severity="error">{submitError}</Alert>}
                        <TextField
                            label="Email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required
                            error={!!email && !emailValid}
                            helperText={email && !emailValid ? "Enter a valid email" : ""}
                            autoFocus
                        />
                        <TextField
                            label="Display name"
                            value={displayName}
                            onChange={(e) => setDisplayName(e.target.value)}
                            required
                        />
                        <TextField
                            label="Password"
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                        />
                        <TextField
                            label="Confirm password"
                            type="password"
                            value={confirmPassword}
                            onChange={(e) => setConfirmPassword(e.target.value)}
                            required
                            error={!!confirmPassword && password !== confirmPassword}
                            helperText={confirmPassword && password !== confirmPassword ? "Passwords must match" : ""}
                        />
                        <Button type="submit" variant="contained" disabled={submitting}>
                            {submitting ? "Creating..." : "Create admin"}
                        </Button>
                    </Stack>
                </CardContent>
            </Card>
        </Box>
    );
}
