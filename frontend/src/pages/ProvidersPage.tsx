import { useEffect, useMemo, useState } from "react";
import {
    Alert,
    Box,
    Button,
    Card,
    CardContent,
    CardHeader,
    Chip,
    CircularProgress,
    IconButton,
    InputAdornment,
    Link,
    Stack,
    TextField,
    Tooltip,
    Typography,
} from "@mui/material";
import ContentCopyIcon from "@mui/icons-material/ContentCopy";
import RefreshIcon from "@mui/icons-material/Refresh";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import ErrorOutlineIcon from "@mui/icons-material/ErrorOutline";
import { getProviderStatus, listProviders } from "../api";
import type { ProviderInfo, ProviderStatus } from "../types";

export default function ProvidersPage() {
    const [providers, setProviders] = useState<ProviderInfo[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string>();
    const [filter, setFilter] = useState("");
    const [statusByKey, setStatusByKey] = useState<Record<string, ProviderStatus>>({});
    const [checkingKey, setCheckingKey] = useState<string | null>(null);

    useEffect(() => {
        const load = async () => {
            try {
                setLoading(true);
                setError(undefined);
                const data = await listProviders();
                if (Array.isArray(data)) {
                    setProviders(data);
                } else {
                    setProviders([]);
                    setError("Unexpected response from provider service");
                }
            } catch (e) {
                setError((e as Error).message);
            } finally {
                setLoading(false);
            }
        };
        void load();
    }, []);

    const filtered = useMemo(() => {
        const q = filter.trim().toLowerCase();
        if (!q) return providers;
        return providers.filter((p) =>
            [p.displayName, p.key, p.description ?? ""].some((v) => v?.toLowerCase().includes(q))
        );
    }, [filter, providers]);

    const handleCopy = async (text: string) => {
        try {
            await navigator.clipboard.writeText(text);
        } catch {
            // noop
        }
    };

    const checkStatus = async (key: string) => {
        try {
            setCheckingKey(key);
            const status = await getProviderStatus(key);
            setStatusByKey((prev) => ({ ...prev, [key]: status }));
        } catch (e) {
            setStatusByKey((prev) => ({
                ...prev,
                [key]: {
                    key,
                    available: false,
                    supportedIdTypes: [],
                    message: (e as Error).message,
                },
            }));
        } finally {
            setCheckingKey(null);
        }
    };

    return (
        <Stack spacing={2}>
            <Typography variant="h4">Providers</Typography>

            <TextField
                value={filter}
                onChange={(e) => setFilter(e.target.value)}
                placeholder="Search providers by name, key, or description"
                size="small"
                InputProps={{
                    startAdornment: <InputAdornment position="start">🔍</InputAdornment>,
                }}
            />

            {loading && (
                <Box sx={{ display: "flex", justifyContent: "center", py: 4 }}>
                    <CircularProgress size={28} />
                </Box>
            )}

            {error && (
                <Alert severity="error" variant="outlined">{error}</Alert>
            )}

            {!loading && !error && filtered.length === 0 && (
                <Alert severity="info" variant="outlined">No providers match your search.</Alert>
            )}

            <Stack spacing={2}>
                {filtered.map((p) => {
                    const status = statusByKey[p.key];
                    const isChecking = checkingKey === p.key;
                    const statusIcon = status?.available ? <CheckCircleIcon color="success" fontSize="small" /> : <ErrorOutlineIcon color="warning" fontSize="small" />;
                    return (
                        <Card key={p.key} variant="outlined">
                            <CardHeader
                                title={p.displayName}
                                subheader={p.description || "No description"}
                                action={
                                    <Stack direction="row" spacing={1} alignItems="center">
                                        <Tooltip title="Copy provider key">
                                            <IconButton onClick={() => handleCopy(p.key)} size="small">
                                                <ContentCopyIcon fontSize="inherit" />
                                            </IconButton>
                                        </Tooltip>
                                        <Button
                                            onClick={() => checkStatus(p.key)}
                                            size="small"
                                            startIcon={<RefreshIcon fontSize="small" />}
                                            disabled={isChecking}
                                        >
                                            {isChecking ? "Checking…" : "Check status"}
                                        </Button>
                                    </Stack>
                                }
                            />
                            <CardContent>
                                <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                                    <Chip label={`Key: ${p.key}`} size="small" />
                                    {p.supportedIdTypes?.map((t) => (
                                        <Chip key={t} label={t} size="small" color="secondary" />
                                    ))}
                                    {typeof p.priority === "number" && <Chip label={`Priority: ${p.priority}`} size="small" />}
                                </Stack>

                                {p.dataReturned && (
                                    <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                                        {p.dataReturned}
                                    </Typography>
                                )}

                                {(p.websiteUrl || p.apiUrl) && (
                                    <Stack direction="row" spacing={2} flexWrap="wrap" sx={{ mt: 1 }}>
                                        {p.websiteUrl && (
                                            <Link href={p.websiteUrl} target="_blank" rel="noreferrer" underline="hover">
                                                Visit website
                                            </Link>
                                        )}
                                        {p.apiUrl && (
                                            <Link href={p.apiUrl} target="_blank" rel="noreferrer" underline="hover">
                                                API documentation
                                            </Link>
                                        )}
                                    </Stack>
                                )}

                                {p.highlights?.length ? (
                                    <Stack direction="row" spacing={1} flexWrap="wrap" sx={{ mt: 1 }}>
                                        {p.highlights.map((h) => (
                                            <Chip key={h} label={h} variant="outlined" size="small" />
                                        ))}
                                    </Stack>
                                ) : null}

                                {status && (
                                    <Box sx={{ mt: 2, display: "flex", alignItems: "center", gap: 1 }}>
                                        {statusIcon}
                                        <Typography variant="body2" color={status.available ? "success.main" : "warning.main"}>
                                            {status.message || (status.available ? "Available" : "Unavailable")}
                                        </Typography>
                                    </Box>
                                )}
                            </CardContent>
                        </Card>
                    );
                })}
            </Stack>
        </Stack>
    );
}
