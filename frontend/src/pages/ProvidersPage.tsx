import { useCallback, useEffect, useMemo, useState } from "react";
import {
    Alert,
    Box,
    Button,
    Card,
    CardContent,
    CardHeader,
    Chip,
    CircularProgress,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
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
import { checkProviderStatus, deleteProviderCredentials, listProviders, saveProviderCredentials } from "../api";
import type { ProviderInfo, ProviderStatus, User } from "../types";
import { useOutletContext } from "react-router-dom";

export default function ProvidersPage() {
    const { user } = useOutletContext<{ user: User }>();
    const [providers, setProviders] = useState<ProviderInfo[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string>();
    const [filter, setFilter] = useState("");
    const [statusByKey, setStatusByKey] = useState<Record<string, ProviderStatus>>({});
    const [checkingKey, setCheckingKey] = useState<string | null>(null);

    const [credentialDialogProvider, setCredentialDialogProvider] = useState<ProviderInfo | null>(null);
    const [credentialValues, setCredentialValues] = useState<Record<string, string>>({});
    const [credentialError, setCredentialError] = useState<string>();
    const [credentialMessage, setCredentialMessage] = useState<string>();
    const [credentialSaving, setCredentialSaving] = useState(false);
    const [credentialDeleting, setCredentialDeleting] = useState(false);

    const loadProviders = useCallback(async (showSpinner = true): Promise<ProviderInfo[] | undefined> => {
        if (showSpinner) setLoading(true);
        try {
            setError(undefined);
            const data = await listProviders();
            if (Array.isArray(data)) {
                setProviders(data);
                return data;
            }
            setProviders([]);
            setError("Unexpected response from provider service");
        } catch (e) {
            setProviders([]);
            setError((e as Error).message);
        } finally {
            if (showSpinner) setLoading(false);
        }
        return undefined;
    }, []);

    useEffect(() => {
        void loadProviders();
    }, [loadProviders]);

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

    const applyStatus = (key: string, status: ProviderStatus) => {
        setStatusByKey((prev) => ({ ...prev, [key]: status }));
        if (status.rateLimited && status.retryAfterSeconds) {
            window.setTimeout(() => {
                setStatusByKey((prev) => {
                    const existing = prev[key];
                    if (!existing || !existing.rateLimited) return prev;
                    return { ...prev, [key]: { ...existing, rateLimited: false } };
                });
            }, status.retryAfterSeconds * 1000);
        }
    };

    const checkStatus = async (key: string) => {
        try {
            setCheckingKey(key);
            const status = await checkProviderStatus(key);
            applyStatus(key, status);
        } catch (e) {
            applyStatus(key, {
                key,
                available: false,
                supportedIdTypes: [],
                message: (e as Error).message,
                credentialsRequired: false,
                credentialsConfigured: false,
            });
        } finally {
            setCheckingKey(null);
        }
    };

    const credentialFields = credentialDialogProvider?.credentialFields ?? [];
    const canSaveCredentials = credentialFields.length > 0 && credentialFields.every((field) => (credentialValues[field.name] ?? "").trim().length > 0);

    const openCredentialDialog = (provider: ProviderInfo) => {
        setCredentialDialogProvider(provider);
        setCredentialValues({});
        setCredentialError(undefined);
        setCredentialMessage(undefined);
        setCredentialSaving(false);
        setCredentialDeleting(false);
    };

    const closeCredentialDialog = () => {
        setCredentialDialogProvider(null);
        setCredentialValues({});
        setCredentialError(undefined);
        setCredentialMessage(undefined);
    };

    const handleSaveCredentials = async () => {
        if (!credentialDialogProvider) return;
        const key = credentialDialogProvider.key;
        const payload: Record<string, string> = {};
        credentialFields.forEach((field) => {
            payload[field.name] = (credentialValues[field.name] ?? "").trim();
        });
        try {
            setCredentialSaving(true);
            setCredentialError(undefined);
            await saveProviderCredentials(key, payload);
            setCredentialMessage("Credentials saved.");
            setCredentialValues({});
            const refreshed = await loadProviders(false);
            if (refreshed) {
                const updated = refreshed.find((p) => p.key === key);
                if (updated) setCredentialDialogProvider(updated);
            }
        } catch (e) {
            setCredentialError((e as Error).message);
        } finally {
            setCredentialSaving(false);
        }
    };

    const handleClearCredentials = async () => {
        if (!credentialDialogProvider) return;
        const key = credentialDialogProvider.key;
        try {
            setCredentialDeleting(true);
            setCredentialError(undefined);
            await deleteProviderCredentials(key);
            setCredentialMessage("Credentials cleared.");
            setCredentialValues({});
            const refreshed = await loadProviders(false);
            if (refreshed) {
                const updated = refreshed.find((p) => p.key === key);
                setCredentialDialogProvider(updated ?? null);
            }
        } catch (e) {
            setCredentialError((e as Error).message);
        } finally {
            setCredentialDeleting(false);
        }
    };

    const statusText = (status?: ProviderStatus) => {
        if (!status) return undefined;
        if (status.credentialsRequired && !status.credentialsConfigured) {
            return "Waiting for an administrator to store the required credentials.";
        }
        if (status.rateLimited) return "Status checks are cooling down. Try again soon.";
        if (status.available) return status.message || "Available";
        if (status.message?.toLowerCase().includes("internal")) return "Status check failed inside the app.";
        if (status.message?.toLowerCase().includes("unreachable")) return "Provider API is unreachable.";
        return "Provider API is unreachable.";
    };

    const credentialDialogTitle = credentialDialogProvider
        ? `${credentialDialogProvider.displayName} credentials`
        : "Provider credentials";

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
                    const statusMessage = statusText(status);
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
                                            disabled={isChecking || status?.rateLimited}
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
                                    {p.credentialFields?.length ? (
                                        <Chip
                                            label={p.credentialsConfigured ? "Credentials stored" : "Credentials required"}
                                            size="small"
                                            color={p.credentialsConfigured ? "success" : "warning"}
                                        />
                                    ) : null}
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

                                {p.credentialFields?.length ? (
                                    <Box sx={{ mt: 1 }}>
                                        {user.admin ? (
                                            <Button
                                                variant="outlined"
                                                size="small"
                                                onClick={() => openCredentialDialog(p)}
                                            >
                                                {p.credentialsConfigured ? "Update credentials" : "Configure credentials"}
                                            </Button>
                                        ) : !p.credentialsConfigured ? (
                                            <Typography variant="body2" color="text.secondary">
                                                Credentials are required for this provider and can only be configured by an administrator.
                                            </Typography>
                                        ) : null}
                                    </Box>
                                ) : null}

                                {status && statusMessage && (
                                    <Box sx={{ mt: 2, display: "flex", alignItems: "center", gap: 1 }}>
                                        {statusIcon}
                                        <Typography variant="body2" color={status.available ? "success.main" : "warning.main"}>
                                            {statusMessage}
                                        </Typography>
                                    </Box>
                                )}
                            </CardContent>
                        </Card>
                    );
                })}
            </Stack>

            <Dialog open={Boolean(credentialDialogProvider)} onClose={closeCredentialDialog} fullWidth maxWidth="sm">
                <DialogTitle>{credentialDialogTitle}</DialogTitle>
                <DialogContent dividers>
                    <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                        Credentials are stored encrypted and cannot be viewed later. Re-enter values to save or update them.
                    </Typography>
                    {credentialFields.map((field) => (
                        <TextField
                            key={field.name}
                            margin="dense"
                            label={field.label}
                            fullWidth
                            type={field.secret ? "password" : "text"}
                            value={credentialValues[field.name] ?? ""}
                            onChange={(event) =>
                                setCredentialValues((prev) => ({ ...prev, [field.name]: event.target.value }))
                            }
                            helperText={field.description}
                            autoComplete="off"
                        />
                    ))}
                    {credentialError && (
                        <Alert severity="error" sx={{ mt: 2 }}>
                            {credentialError}
                        </Alert>
                    )}
                    {credentialMessage && (
                        <Alert severity="success" sx={{ mt: 2 }}>
                            {credentialMessage}
                        </Alert>
                    )}
                </DialogContent>
                <DialogActions>
                    <Button onClick={closeCredentialDialog}>Close</Button>
                    {credentialDialogProvider?.credentialsConfigured && (
                        <Button color="error" onClick={handleClearCredentials} disabled={credentialDeleting || credentialSaving}>
                            {credentialDeleting ? "Removing…" : "Clear credentials"}
                        </Button>
                    )}
                    <Button variant="contained" onClick={handleSaveCredentials} disabled={!canSaveCredentials || credentialSaving}>
                        {credentialSaving
                            ? "Saving…"
                            : credentialDialogProvider?.credentialsConfigured
                            ? "Update credentials"
                            : "Save credentials"}
                    </Button>
                </DialogActions>
            </Dialog>
        </Stack>
    );
}
