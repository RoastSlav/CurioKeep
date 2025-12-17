import { Card, CardContent, Chip, Stack, Typography } from "@mui/material";
import { useEffect, useState } from "react";
import { useOutletContext } from "react-router-dom";
import { getMe } from "../api";
import type { User } from "../types";

export default function ProfilePage() {
    const outlet = useOutletContext<{ user?: User }>();
    const [user, setUser] = useState<User | null>(outlet?.user ?? null);

    useEffect(() => {
        setUser(outlet?.user ?? null);
    }, [outlet?.user]);

    useEffect(() => {
        if (!user) {
            void getMe().then(setUser).catch(() => undefined);
        }
    }, [user]);

    if (!user) {
        return (
            <Stack spacing={2}>
                <Typography variant="h4">Profile</Typography>
                <Typography color="text.secondary">Loading your profile…</Typography>
            </Stack>
        );
    }

    return (
        <Stack spacing={2}>
            <Typography variant="h4">Profile</Typography>
            <Card>
                <CardContent>
                    <Stack spacing={1}>
                        <Typography variant="h6">{user.displayName || user.email}</Typography>
                        <Typography color="text.secondary">{user.email}</Typography>
                        <Stack direction="row" spacing={1} alignItems="center">
                            <Chip label={user.admin ? "Admin" : "User"} color={user.admin ? "primary" : "default"} />
                            <Chip label={`User ID: ${user.id}`} variant="outlined" />
                        </Stack>
                    </Stack>
                </CardContent>
            </Card>
        </Stack>
    );
}
