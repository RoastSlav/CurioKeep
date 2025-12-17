import { useState } from "react";
import { Alert, Box, Button, Card, CardContent, Stack, TextField, Typography } from "@mui/material";
import { createAdmin, getSetupStatus } from "../api";

type Props = { onCompleted: () => void };

export default function SetupPage({ onCompleted }: Props) {
    const [email, setEmail] = useState("");
    const [displayName, setDisplayName] = useState("");
    const [password, setPassword] = useState("");
    const [confirm, setConfirm] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (password !== confirm) {
            setError("Passwords do not match");
            return;
        }
        try {
            setLoading(true);
            setError(null);
            await createAdmin(email, displayName, password);
            const status = await getSetupStatus();
            if (!status.setupRequired) {
                onCompleted();
            }
        } catch (err) {
            setError((err as Error).message);
        } finally {
            setLoading(false);
        }
    };

    return (
        <Box sx={{ display: "grid", placeItems: "center", minHeight: "100vh", p: 2 }}>
            <Card sx={{ maxWidth: 460, width: "100%" }}>
                <CardContent>
                    <Stack spacing={3} component="form" onSubmit={handleSubmit}>
                        <Typography variant="h5">Set up CurioKeep</Typography>
                        <Typography variant="body2" color="text.secondary">
                            Create the first admin account to finish setup. This form is only available until the first admin exists.
                        </Typography>
                        {error && <Alert severity="error">{error}</Alert>}
                        <TextField label="Email" type="email" required value={email} onChange={(e) => setEmail(e.target.value)} fullWidth />
                        <TextField label="Display name" required value={displayName} onChange={(e) => setDisplayName(e.target.value)} fullWidth />
                        <TextField label="Password" type="password" required value={password} onChange={(e) => setPassword(e.target.value)} fullWidth />
                        <TextField label="Confirm password" type="password" required value={confirm} onChange={(e) => setConfirm(e.target.value)} fullWidth />
                        <Button type="submit" variant="contained" disabled={loading} size="large">
                            {loading ? "Creating…" : "Create admin"}
                        </Button>
                    </Stack>
                </CardContent>
            </Card>
        </Box>
    );
}
