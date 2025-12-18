import { ContentCopy } from "@mui/icons-material";
import { Alert, Box, Button, MenuItem, Stack, TextField, Typography } from "@mui/material";
import { useMemo, useState } from "react";
import type { CollectionInvite } from "../../../../api/types";
import type { CreateCollectionInviteRequest } from "../../../../api/types";

const ROLES: Array<CreateCollectionInviteRequest["role"]> = ["VIEWER", "EDITOR", "ADMIN"];

type Props = {
    onCreate: (payload: CreateCollectionInviteRequest) => Promise<CollectionInvite>;
};

export default function InviteMemberForm({ onCreate }: Props) {
    const [role, setRole] = useState<CreateCollectionInviteRequest["role"]>("VIEWER");
    const [days, setDays] = useState<number | "">(7);
    const [link, setLink] = useState<string | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState(false);

    const fullUrl = useMemo(() => {
        if (!link) return null;
        if (typeof window === "undefined") return `/invites/collection/${link}`;
        return `${window.location.origin}/invites/collection/${link}`;
    }, [link]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        setError(null);
        try {
            const payload: CreateCollectionInviteRequest = {
                role,
                expiresInDays: days === "" ? undefined : Number(days),
            };
            const resp = await onCreate(payload);
            setLink(resp.token);
        } catch (err: any) {
            setError(err?.message || "Failed to create invite");
        } finally {
            setLoading(false);
        }
    };

    const copyLink = () => {
        if (!fullUrl) return;
        void navigator.clipboard.writeText(fullUrl);
    };

    return (
        <Stack component="form" spacing={2} onSubmit={handleSubmit}>
            <Typography variant="h6" fontWeight={700}>
                Invite member
            </Typography>

            <Stack direction={{ xs: "column", sm: "row" }} spacing={2}>
                <TextField
                    select
                    label="Role"
                    value={role}
                    onChange={(e) => setRole(e.target.value as CreateCollectionInviteRequest["role"])}
                    fullWidth
                    size="small"
                >
                    {ROLES.map((r) => (
                        <MenuItem key={r} value={r}>
                            {r}
                        </MenuItem>
                    ))}
                </TextField>

                <TextField
                    label="Expires in days"
                    type="number"
                    value={days}
                    onChange={(e) => setDays(e.target.value === "" ? "" : Number(e.target.value))}
                    fullWidth
                    size="small"
                    inputProps={{ min: 1, max: 365 }}
                />

                <Button type="submit" variant="contained" disabled={loading} sx={{ whiteSpace: "nowrap" }}>
                    {loading ? "Creating…" : "Create invite"}
                </Button>
            </Stack>

            {error && <Alert severity="error">{error}</Alert>}

            {link && (
                <Box
                    sx={{
                        border: "1px solid",
                        borderColor: "divider",
                        borderRadius: 1,
                        p: 1,
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "space-between",
                        gap: 1,
                        flexWrap: "wrap",
                    }}
                >
                    <Stack spacing={0.5}>
                        <Typography variant="body2" color="text.secondary">
                            Share this link
                        </Typography>
                        <Typography variant="body2" sx={{ wordBreak: "break-all" }}>
                            {fullUrl}
                        </Typography>
                    </Stack>
                    <Button variant="outlined" startIcon={<ContentCopy />} onClick={copyLink}>
                        Copy
                    </Button>
                </Box>
            )}
        </Stack>
    );
}
