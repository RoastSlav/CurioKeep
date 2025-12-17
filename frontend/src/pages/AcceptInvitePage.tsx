import { useEffect, useMemo, useState } from "react";
import { Alert, Box, Button, Card, CardContent, Stack, TextField, Typography } from "@mui/material";
import { useSearchParams, useNavigate } from "react-router-dom";
import { acceptInvite, validateInvite } from "../api";
import { useToasts } from "../components/Toasts";

export default function AcceptInvitePage() {
    const [params] = useSearchParams();
    const initialToken = params.get("token") || "";
    const [token, setToken] = useState(initialToken);
    const [displayName, setDisplayName] = useState("");
    const [password, setPassword] = useState("");
    const [confirm, setConfirm] = useState("");
    const [valid, setValid] = useState<boolean | null>(null);
    const [validating, setValidating] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [accepted, setAccepted] = useState(false);
    const navigate = useNavigate();
    const toasts = useToasts();

    const passwordsMatch = useMemo(() => password === confirm && password.length > 0, [password, confirm]);

    useEffect(() => {
        if (!token) {
            setValid(null);
            return;
        }
        setValidating(true);
        setError(null);
        void validateInvite(token)
            .then((r) => setValid(r.valid))
            .catch((err) => {
                setError((err as Error).message);
                setValid(false);
            })
            .finally(() => setValidating(false));
    }, [token]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!passwordsMatch || !token || !displayName) return;
        setSubmitting(true);
        setError(null);
        try {
            await acceptInvite(token, password, displayName);
            setAccepted(true);
            toasts.show("Invite accepted. You can now sign in.", "success");
        } catch (err) {
            setError((err as Error).message);
        } finally {
            setSubmitting(false);
        }
    };

    if (accepted) {
        return (
            <Stack spacing={2} sx={{ maxWidth: 480, mx: "auto", p: 2 }}>
                <Typography variant="h4">Invite accepted</Typography>
                <Alert severity="success">Your account is created. Continue to sign in.</Alert>
                <Button variant="contained" onClick={() => navigate("/login", { replace: true })}>
                    Go to login
                </Button>
            </Stack>
        );
    }

    return (
        <Box sx={{ display: "grid", placeItems: "center", minHeight: "100vh", p: 2 }}>
            <Card sx={{ maxWidth: 520, width: "100%" }}>
                <CardContent>
                    <Stack component="form" spacing={2} onSubmit={handleSubmit}>
                        <Typography variant="h5">Accept invite</Typography>
                        <Typography variant="body2" color="text.secondary">
                            Paste your invite link or token, set a display name, and choose a password to finish sign-up.
                        </Typography>
                        {error && <Alert severity="error">{error}</Alert>}
                        <TextField
                            label="Invite token"
                            value={token}
                            onChange={(e) => setToken(e.target.value.trim())}
                            required
                            helperText={valid === false ? "Invite looks invalid or expired" : ""}
                            error={valid === false}
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
                            value={confirm}
                            onChange={(e) => setConfirm(e.target.value)}
                            required
                            error={confirm.length > 0 && !passwordsMatch}
                            helperText={confirm.length > 0 && !passwordsMatch ? "Passwords do not match" : ""}
                        />
                        <Stack direction="row" spacing={1} alignItems="center" justifyContent="space-between">
                            <Typography color="text.secondary" variant="body2">
                                {validating ? "Validating…" : valid === false ? "Invite invalid/expired" : valid ? "Invite valid" : ""}
                            </Typography>
                            <Button
                                type="submit"
                                variant="contained"
                                disabled={submitting || validating || !token || !displayName || !passwordsMatch || valid === false}
                            >
                                {submitting ? "Submitting…" : "Accept invite"}
                            </Button>
                        </Stack>
                    </Stack>
                </CardContent>
            </Card>
        </Box>
    );
}
