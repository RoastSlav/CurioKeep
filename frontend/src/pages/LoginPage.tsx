import { useState } from "react";
import { Alert, Box, Button, Card, CardContent, Stack, TextField, Typography } from "@mui/material";
import { login } from "../api";

type Props = { onLoginSuccess: () => void };

export default function LoginPage({ onLoginSuccess }: Props) {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            setLoading(true);
            setError(null);
            await login(email, password);
            onLoginSuccess();
        } catch (err) {
            setError((err as Error).message);
        } finally {
            setLoading(false);
        }
    };

    return (
        <Box sx={{ display: "grid", placeItems: "center", minHeight: "100vh", p: 2 }}>
            <Card sx={{ maxWidth: 420, width: "100%" }}>
                <CardContent>
                    <Stack spacing={3} component="form" onSubmit={handleSubmit}>
                        <Typography variant="h5">Sign in</Typography>
                        <Typography variant="body2" color="text.secondary">
                            Use the admin credentials created during setup.
                        </Typography>
                        {error && <Alert severity="error">{error}</Alert>}
                        <TextField label="Email" type="email" required value={email} onChange={(e) => setEmail(e.target.value)} fullWidth />
                        <TextField label="Password" type="password" required value={password} onChange={(e) => setPassword(e.target.value)} fullWidth />
                        <Button type="submit" variant="contained" disabled={loading} size="large">
                            {loading ? "Signing in…" : "Sign in"}
                        </Button>
                    </Stack>
                </CardContent>
            </Card>
        </Box>
    );
}
