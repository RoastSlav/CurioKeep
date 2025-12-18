import { Alert, Box, Button, CircularProgress, Paper, Stack, Typography } from "@mui/material";
import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useAuth } from "../../../auth/useAuth";
import LoadingState from "../../../components/LoadingState";
import ErrorState from "../../../components/ErrorState";
import { acceptCollectionInvite, validateCollectionInvite } from "../api/collectionInvitesApi";

export default function AcceptCollectionInvitePage() {
    const { token } = useParams<{ token: string }>();
    const navigate = useNavigate();
    const { user, loading: authLoading } = useAuth();

    const [status, setStatus] = useState<"idle" | "loading" | "ready" | "accepting" | "accepted">("idle");
    const [error, setError] = useState<string | null>(null);
    const [role, setRole] = useState<string | null>(null);
    const [collectionId, setCollectionId] = useState<string | null>(null);
    const [reason, setReason] = useState<string | null>(null);

    const inviteValid = useMemo(() => !error && !reason && role && collectionId, [error, reason, role, collectionId]);

    useEffect(() => {
        if (!token) return;
        const load = async () => {
            setStatus("loading");
            setError(null);
            try {
                const res = await validateCollectionInvite(token);
                if (!res.valid || !res.collectionId) {
                    setReason(res.reason || "Invite is not valid");
                    setStatus("ready");
                    return;
                }
                setRole(res.role || null);
                setCollectionId(res.collectionId);
                setStatus("ready");
            } catch (err: any) {
                setError(err?.message || "Failed to validate invite");
                setStatus("ready");
            }
        };
        void load();
    }, [token]);

    useEffect(() => {
        if (authLoading) return;
        if (!user) {
            const params = new URLSearchParams();
            params.set("returnTo", `/invites/collection/${token}`);
            navigate(`/login?${params.toString()}`, { replace: true });
        }
    }, [authLoading, navigate, token, user]);

    const handleAccept = async () => {
        if (!token || !collectionId) return;
        setStatus("accepting");
        setError(null);
        try {
            await acceptCollectionInvite(token);
            setStatus("accepted");
            navigate(`/collections/${collectionId}`, { replace: true });
        } catch (err: any) {
            setError(err?.message || "Failed to accept invite");
            setStatus("ready");
        }
    };

    if (!token) return <ErrorState title="Invalid invite" message="Missing invite token" />;
    if (authLoading) return <LoadingState message="Checking session..." />;

    return (
        <Stack alignItems="center" mt={6}>
            <Paper variant="outlined" sx={{ p: 3, maxWidth: 520, width: "100%" }}>
                <Stack spacing={2}>
                    <Typography variant="h5" fontWeight={700}>
                        Accept collection invite
                    </Typography>

                    {status === "loading" && (
                        <Stack direction="row" spacing={1} alignItems="center">
                            <CircularProgress size={20} thickness={5} />
                            <Typography color="text.secondary">Validating invite…</Typography>
                        </Stack>
                    )}

                    {error && <Alert severity="error">{error}</Alert>}
                    {reason && <Alert severity="warning">{reason}</Alert>}

                    {status === "ready" && inviteValid && (
                        <Box>
                            <Typography gutterBottom>You have been invited to a collection.</Typography>
                            <Typography>Role: <strong>{role}</strong></Typography>
                        </Box>
                    )}

                    {status === "accepted" && (
                        <Alert severity="success">Invite accepted! Redirecting…</Alert>
                    )}

                    <Stack direction="row" spacing={1} justifyContent="flex-end">
                        <Button variant="outlined" onClick={() => navigate("/")}>Cancel</Button>
                        <Button
                            variant="contained"
                            disabled={!inviteValid || status === "accepting"}
                            onClick={() => void handleAccept()}
                        >
                            {status === "accepting" ? "Accepting…" : "Accept invite"}
                        </Button>
                    </Stack>
                </Stack>
            </Paper>
        </Stack>
    );
}
