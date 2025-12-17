import {useEffect, useMemo, useState} from "react";
import {useNavigate, useParams} from "react-router-dom";
import {Alert, Box, Button, Card, CardContent, Chip, CircularProgress, Container, Divider, Stack, Typography} from "@mui/material";
import {getItem, getModule} from "../api";
import type {Item, ModuleDetails} from "../types";

export default function ItemDetailPage() {
    const { collectionId, moduleKey, itemId } = useParams();
    const navigate = useNavigate();

    const [item, setItem] = useState<Item | null>(null);
    const [module, setModule] = useState<ModuleDetails | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        let cancelled = false;
        async function load() {
            if (!collectionId || !itemId) {
                setError("Missing collection or item id.");
                setLoading(false);
                return;
            }
            setLoading(true);
            setError(null);
            try {
                const [fetchedItem, mod] = await Promise.all([
                    getItem(collectionId, itemId),
                    moduleKey ? getModule(moduleKey) : Promise.resolve(null),
                ]);
                if (cancelled) return;
                setItem(fetchedItem);
                if (mod) setModule(mod);
            } catch (err) {
                if (cancelled) return;
                setError((err as Error).message);
            } finally {
                if (!cancelled) setLoading(false);
            }
        }
        void load();
        return () => { cancelled = true; };
    }, [collectionId, itemId, moduleKey]);

    const attributes = useMemo(() => item?.attributes as Record<string, unknown> | undefined, [item]);

    if (loading) {
        return (
            <Container maxWidth="md" sx={{ py: 4, textAlign: "center" }}>
                <CircularProgress />
                <Typography variant="body2" sx={{ mt: 2 }}>
                    Loading item…
                </Typography>
            </Container>
        );
    }

    if (error) {
        return (
            <Container maxWidth="md" sx={{ py: 4 }}>
                <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>
                <Button variant="outlined" onClick={() => navigate(-1)}>Back</Button>
            </Container>
        );
    }

    if (!item) return null;

    const displayTitle = item.title || (typeof attributes?.title === "string" ? attributes.title : undefined) || (typeof attributes?.name === "string" ? attributes.name : undefined) || "Untitled item";

    return (
        <Container maxWidth="md" sx={{ py: 4 }}>
            <Stack spacing={2}>
                <Stack direction="row" alignItems="center" spacing={1}>
                    <Typography variant="h5">{displayTitle}</Typography>
                    <Chip size="small" label={item.stateKey} />
                </Stack>
                <Typography variant="body2" color="text.secondary">
                    {module?.name || moduleKey || ""}
                </Typography>
                <Stack direction="row" spacing={1}>
                    <Button variant="outlined" onClick={() => navigate(collectionId ? `/collections/${collectionId}` : "/collections")}>Back to collection</Button>
                    {collectionId && moduleKey && (
                        <Button
                            variant="contained"
                            onClick={() => navigate(`/collections/${collectionId}/modules/${moduleKey}/add`)}
                        >
                            Add another item
                        </Button>
                    )}
                </Stack>

                <Card>
                    <CardContent>
                        <Stack spacing={2}>
                            <Typography variant="h6">Details</Typography>
                            <Stack direction="row" spacing={3} flexWrap="wrap">
                                <Detail label="State" value={item.stateKey} />
                                {item.createdAt && <Detail label="Created" value={new Date(item.createdAt).toLocaleString()} />}
                                {item.updatedAt && <Detail label="Updated" value={new Date(item.updatedAt).toLocaleString()} />}
                            </Stack>
                            <Divider />
                            <Typography variant="subtitle2" color="text.secondary">Attributes</Typography>
                            {attributes && Object.keys(attributes).length > 0 ? (
                                <Stack spacing={1}>
                                    {Object.entries(attributes).map(([key, value]) => (
                                        <Box key={key} sx={{ display: "flex", justifyContent: "space-between", gap: 2 }}>
                                            <Typography fontWeight={600}>{key}</Typography>
                                            <Typography sx={{ wordBreak: "break-word", textAlign: "right" }} color="text.secondary">
                                                {renderValue(value)}
                                            </Typography>
                                        </Box>
                                    ))}
                                </Stack>
                            ) : (
                                <Typography color="text.secondary">No attributes set.</Typography>
                            )}
                        </Stack>
                    </CardContent>
                </Card>
            </Stack>
        </Container>
    );
}

function Detail({ label, value }: { label: string; value?: string }) {
    if (!value) return null;
    return (
        <Stack spacing={0.5}>
            <Typography variant="caption" color="text.secondary">{label}</Typography>
            <Typography>{value}</Typography>
        </Stack>
    );
}

function renderValue(value: unknown): string {
    if (value === null || value === undefined) return "";
    if (typeof value === "string") return value;
    if (typeof value === "number" || typeof value === "boolean") return String(value);
    try {
        return JSON.stringify(value);
    } catch {
        return String(value);
    }
}
