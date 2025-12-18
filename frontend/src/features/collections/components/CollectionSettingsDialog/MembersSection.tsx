import { Delete, Shield } from "@mui/icons-material";
import {
    Alert,
    Box,
    Chip,
    CircularProgress,
    IconButton,
    MenuItem,
    Select,
    Stack,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableRow,
    Typography,
} from "@mui/material";
import { useState } from "react";
import type { CollectionMember } from "../../../../api/types";
import ConfirmRemoveMemberDialog from "./ConfirmRemoveMemberDialog";

const ROLE_OPTIONS: Array<CollectionMember["role"]> = ["ADMIN", "EDITOR", "VIEWER"];

type Props = {
    currentUserId?: string;
    members: CollectionMember[];
    loading?: boolean;
    saving?: boolean;
    error?: string | null;
    onChangeRole: (userId: string, role: CollectionMember["role"]) => Promise<void>;
    onRemove: (userId: string) => Promise<void>;
    onRefresh?: () => void;
};

export default function MembersSection({
    currentUserId,
    members,
    loading,
    saving,
    error,
    onChangeRole,
    onRemove,
    onRefresh,
}: Props) {
    const [removeUserId, setRemoveUserId] = useState<string | null>(null);

    const handleRoleChange = async (userId: string, role: CollectionMember["role"]) => {
        await onChangeRole(userId, role);
    };

    const removeMember = async () => {
        if (!removeUserId) return;
        const id = removeUserId;
        setRemoveUserId(null);
        await onRemove(id);
    };

    return (
        <Stack spacing={2}>
            <Stack direction="row" alignItems="center" justifyContent="space-between">
                <Typography variant="h6" fontWeight={700}>
                    Members
                </Typography>
                <Stack direction="row" spacing={1} alignItems="center">
                    {saving && <CircularProgress size={18} thickness={5} />}
                    {onRefresh && (
                        <IconButton onClick={onRefresh} disabled={loading}>
                            <Shield fontSize="small" />
                        </IconButton>
                    )}
                </Stack>
            </Stack>

            {error && <Alert severity="error">{error}</Alert>}

            {loading ? (
                <Stack direction="row" spacing={1} alignItems="center">
                    <CircularProgress size={20} thickness={4} />
                    <Typography color="text.secondary">Loading members…</Typography>
                </Stack>
            ) : members.length ? (
                <Box sx={{ overflowX: "auto" }}>
                    <Table size="small">
                        <TableHead>
                            <TableRow>
                                <TableCell>User</TableCell>
                                <TableCell>Role</TableCell>
                                <TableCell align="right">Actions</TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {members.map((m) => {
                                const isSelf = m.userId === currentUserId;
                                const isOwner = m.role === "OWNER";
                                const disableActions = saving || isSelf;
                                const showActions = !isOwner;
                                return (
                                    <TableRow key={m.userId} hover>
                                        <TableCell>
                                            <Stack spacing={0.2}>
                                                <Typography fontWeight={700}>{m.displayName || m.email}</Typography>
                                                <Typography variant="body2" color="text.secondary">
                                                    {m.email}
                                                </Typography>
                                            </Stack>
                                        </TableCell>
                                        <TableCell>
                                            <Chip label={m.role} size="small" color={m.role === "OWNER" ? "primary" : "default"} />
                                        </TableCell>
                                        <TableCell align="right">
                                            <Stack direction="row" spacing={1} justifyContent="flex-end" alignItems="center">
                                                {showActions && (
                                                    <Select
                                                        size="small"
                                                        value={m.role}
                                                        disabled={disableActions}
                                                        onChange={(e) => void handleRoleChange(m.userId, e.target.value as CollectionMember["role"])}
                                                    >
                                                        {ROLE_OPTIONS.map((r) => (
                                                            <MenuItem key={r} value={r} disabled={r === "OWNER"}>
                                                                {r}
                                                            </MenuItem>
                                                        ))}
                                                    </Select>
                                                )}
                                                {showActions && (
                                                    <IconButton
                                                        color="error"
                                                        size="small"
                                                        disabled={disableActions}
                                                        onClick={() => setRemoveUserId(m.userId)}
                                                    >
                                                        <Delete fontSize="small" />
                                                    </IconButton>
                                                )}
                                            </Stack>
                                        </TableCell>
                                    </TableRow>
                                );
                            })}
                        </TableBody>
                    </Table>
                </Box>
            ) : (
                <Alert severity="info">No members found.</Alert>
            )}

            <ConfirmRemoveMemberDialog
                open={Boolean(removeUserId)}
                memberName={members.find((m) => m.userId === removeUserId)?.displayName || undefined}
                onCancel={() => setRemoveUserId(null)}
                onConfirm={() => void removeMember()}
            />
        </Stack>
    );
}
