import { useState } from "react";
import { Alert, Box, Button, Card, CardContent, Stack, TextField, Typography } from "@mui/material";
import ContentCopyIcon from "@mui/icons-material/ContentCopy";
import { createInvite } from "../api";
import { useToasts } from "../components/Toasts";
import { useOutletContext } from "react-router-dom";
import type { User } from "../types";

export default function AdminInvitesPage() {
    const { user } = useOutletContext<{ user: User }>();
    const toasts = useToasts();
    const [email, setEmail] = useState("");
    const [token, setToken] = useState<string | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const inviteLink = token ? `${window.location.origin}/accept-invite?token=${encodeURIComponent(token)}` : "";

    const handleCopy = async (text: string) => {
        try {
            await navigator.clipboard.writeText(text);
            toasts.show("Copied to clipboard", "success");
        } catch (err) {
            toasts.show((err as Error).message || "Copy failed", "error");
        }
    };

    const handleCreate = async () => {
        setLoading(true);
        setError(null);
        try {
            const newToken = await createInvite(email);
            setToken(newToken);
            toasts.show("Invite created", "success");
        } catch (err) {
            setError((err as Error).message);
        } finally {
            setLoading(false);
        }
    };

    if (!user?.admin) {
        return (
            <Stack spacing={2}>
                <Typography variant="h4">Invites</Typography>
                <Alert severity="warning">Admin access required to create invites.</Alert>
            </Stack>
        );
    }

    return (
        <Stack spacing={2}>
            <Typography variant="h4">Invites</Typography>
            <Card>
                <CardContent>
                    <Stack spacing={2}>
                        <Typography variant="h6">Create invite</Typography>
                        <Typography color="text.secondary">
                            Generates a one-time token valid for 48 hours. Share the link with the user to let them finish sign-up.
                        </Typography>
                        {error && <Alert severity="error">{error}</Alert>}
                        <Stack direction={{ xs: "column", sm: "row" }} spacing={1} alignItems={{ sm: "center" }}>
                            <TextField
                                label="User email"
                                type="email"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                required
                                sx={{ flex: 1, minWidth: 240 }}
                            />
                            <Button variant="contained" onClick={handleCreate} disabled={!email.trim() || loading}>
                                {loading ? "Creating…" : "Create invite"}
                            </Button>
                        </Stack>
                        {token && (
                            <Box sx={{ border: (t) => `1px solid ${t.palette.divider}`, borderRadius: 1, p: 2 }}>
                                <Stack spacing={1}>
                                    <Typography variant="subtitle2">Invite token</Typography>
                                    <Box sx={{ display: "flex", alignItems: "center", gap: 1, flexWrap: "wrap" }}>
                                        <Typography sx={{ wordBreak: "break-all", flex: 1 }}>{token}</Typography>
                                        <Button size="small" startIcon={<ContentCopyIcon fontSize="small" />} onClick={() => handleCopy(token)}>
                                            Copy token
                                        </Button>
                                    </Box>
                                    <Typography variant="subtitle2">Shareable link</Typography>
                                    <Box sx={{ display: "flex", alignItems: "center", gap: 1, flexWrap: "wrap" }}>
                                        <Typography sx={{ wordBreak: "break-all", flex: 1 }}>{inviteLink}</Typography>
                                        <Button size="small" startIcon={<ContentCopyIcon fontSize="small" />} onClick={() => handleCopy(inviteLink)}>
                                            Copy link
                                        </Button>
                                    </Box>
                                </Stack>
                            </Box>
                        )}
                    </Stack>
                </CardContent>
            </Card>
        </Stack>
    );
}
