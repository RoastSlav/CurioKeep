import {useEffect, useState} from "react";
import {Alert, Box, Card, CardContent, Stack, Typography} from "@mui/material";
import {getHealth, listCollections, listModules} from "../api";
import type {Collection, ModuleSummary} from "../types";

export default function DashboardPage() {
    const [health, setHealth] = useState<string>("loading");
    const [collections, setCollections] = useState<Collection[]>([]);
    const [modules, setModules] = useState<ModuleSummary[]>([]);

    useEffect(() => {
        void getHealth()
            .then((h) => setHealth((h.status || "").toUpperCase()))
            .catch(() => setHealth("ERROR"));
        void listCollections().then(setCollections).catch(() => setCollections([]));
        void listModules().then(setModules).catch(() => setModules([]));
    }, []);

    return (
        <Stack spacing={3}>
            <Typography variant="h4">Dashboard</Typography>
            <Box sx={{ display: "grid", gap: 2, gridTemplateColumns: { xs: "1fr", md: "repeat(3, 1fr)" } }}>
                <Card>
                    <CardContent>
                        <Typography variant="subtitle2" color="text.secondary">
                            Backend health
                        </Typography>
                        <Typography variant="h6" sx={{ mt: 1 }} color={health === "UP" || health === "OK" ? "success.main" : "error.main"}>
                            {health === "loading" ? "Checking…" : health}
                        </Typography>
                    </CardContent>
                </Card>
                <Card>
                    <CardContent>
                        <Typography variant="subtitle2" color="text.secondary">
                            Collections
                        </Typography>
                        <Typography variant="h6" sx={{ mt: 1 }}>{collections.length}</Typography>
                    </CardContent>
                </Card>
                <Card>
                    <CardContent>
                        <Typography variant="subtitle2" color="text.secondary">
                            Modules available
                        </Typography>
                        <Typography variant="h6" sx={{ mt: 1 }}>{modules.length}</Typography>
                    </CardContent>
                </Card>
            </Box>
            <Card>
                <CardContent>
                    <Typography variant="h6" gutterBottom>
                        Recently loaded modules
                    </Typography>
                    {modules.length === 0 ? (
                        <Typography color="text.secondary">No modules available yet.</Typography>
                    ) : (
                        <Stack spacing={1}>
                            {modules.slice(0, 5).map((m) => (
                                <Stack key={m.key} direction="row" justifyContent="space-between">
                                    <Typography>{m.name || m.key}</Typography>
                                    <Typography color="text.secondary" variant="body2">
                                        {m.version}
                                    </Typography>
                                </Stack>
                            ))}
                        </Stack>
                    )}
                </CardContent>
            </Card>
            {health !== "UP" && health !== "OK" && health !== "loading" &&
                <Alert severity="warning">Backend not reporting UP.</Alert>}
        </Stack>
    );
}
