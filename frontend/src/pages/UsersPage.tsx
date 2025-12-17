import { useEffect, useState } from "react";
import {
    Alert,
    Button,
    Chip,
    Paper,
    Stack,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Typography,
} from "@mui/material";
import AdminPanelSettingsIcon from "@mui/icons-material/AdminPanelSettings";
import DoNotDisturbIcon from "@mui/icons-material/DoNotDisturb";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import KeyIcon from "@mui/icons-material/Key";
import { listUsers, resetUserPassword, setUserAdmin, setUserStatus } from "../api";
import { useToasts } from "../components/Toasts";
import { useOutletContext } from "react-router-dom";
import type { AdminUser, User } from "../types";

export default function UsersPage() {
    const outlet = useOutletContext<{ user?: User }>();
    const user = outlet?.user;
    const toasts = useToasts();
    const [users, setUsers] = useState<AdminUser[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [busyId, setBusyId] = useState<string | null>(null);

    const refresh = async () => {
        setLoading(true);
        setError(null);
        try {
            const list = await listUsers();
            setUsers(list);
        } catch (err) {
            setError((err as Error).message + " (API may not exist yet)");
            setUsers([]);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        if (user?.admin) void refresh();
    }, [user?.admin]);

    const handleStatus = async (u: AdminUser, status: "ACTIVE" | "DISABLED") => {
        setBusyId(u.id);
        try {
            await setUserStatus(u.id, status);
            toasts.show(`Status updated to ${status}`, "success");
            await refresh();
        } catch (err) {
            toasts.show((err as Error).message || "Failed to update status", "error");
        } finally {
            setBusyId(null);
        }
    };

    const handleAdmin = async (u: AdminUser, admin: boolean) => {
        setBusyId(u.id);
        try {
            await setUserAdmin(u.id, admin);
            toasts.show(admin ? "Granted admin" : "Revoked admin", "success");
            await refresh();
        } catch (err) {
            toasts.show((err as Error).message || "Failed to update admin", "error");
        } finally {
            setBusyId(null);
        }
    };

    const handleResetPassword = async (u: AdminUser) => {
        setBusyId(u.id);
        try {
            await resetUserPassword(u.id);
            toasts.show("Password reset request sent", "info");
        } catch (err) {
            toasts.show((err as Error).message || "Reset failed", "error");
        } finally {
            setBusyId(null);
        }
    };

    if (!user?.admin) {
        return (
            <Stack spacing={2}>
                <Typography variant="h4">Users</Typography>
                <Alert severity="warning">Admin access required to view users.</Alert>
            </Stack>
        );
    }

    return (
        <Stack spacing={2}>
            <Stack direction="row" alignItems="center" justifyContent="space-between">
                <Typography variant="h4">Users</Typography>
                <Button variant="outlined" onClick={refresh} disabled={loading}>
                    Refresh
                </Button>
            </Stack>
            {error && <Alert severity="warning">{error}</Alert>}
            <TableContainer component={Paper}>
                <Table size="small">
                    <TableHead>
                        <TableRow>
                            <TableCell>Name</TableCell>
                            <TableCell>Email</TableCell>
                            <TableCell>Status</TableCell>
                            <TableCell>Auth</TableCell>
                            <TableCell>Last login</TableCell>
                            <TableCell align="right">Actions</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {users.length === 0 && !loading && (
                            <TableRow>
                                <TableCell colSpan={6}>
                                    <Typography color="text.secondary">No users found.</Typography>
                                </TableCell>
                            </TableRow>
                        )}
                        {users.map((u) => (
                            <TableRow key={u.id} hover>
                                <TableCell>
                                    <Stack direction="row" spacing={1} alignItems="center">
                                        <Typography>{u.displayName || u.email}</Typography>
                                        {u.admin && <Chip size="small" label="Admin" color="primary" icon={<AdminPanelSettingsIcon fontSize="small" />} />}
                                    </Stack>
                                </TableCell>
                                <TableCell>{u.email}</TableCell>
                                <TableCell>
                                    <Chip
                                        size="small"
                                        label={u.status || "UNKNOWN"}
                                        color={u.status === "ACTIVE" ? "success" : u.status === "DISABLED" ? "default" : "warning"}
                                        icon={u.status === "ACTIVE" ? <CheckCircleIcon fontSize="small" /> : <DoNotDisturbIcon fontSize="small" />}
                                    />
                                </TableCell>
                                <TableCell>
                                    <Chip size="small" variant="outlined" label={u.authProvider || "LOCAL"} />
                                </TableCell>
                                <TableCell>
                                    <Typography variant="body2" color="text.secondary">
                                        {u.lastLoginAt ? new Date(u.lastLoginAt).toLocaleString() : "—"}
                                    </Typography>
                                </TableCell>
                                <TableCell align="right">
                                    <Stack direction="row" spacing={1} justifyContent="flex-end" flexWrap="wrap">
                                        <Button
                                            size="small"
                                            variant="outlined"
                                            onClick={() => handleStatus(u, u.status === "DISABLED" ? "ACTIVE" : "DISABLED")}
                                            disabled={busyId === u.id}
                                        >
                                            {u.status === "DISABLED" ? "Activate" : "Disable"}
                                        </Button>
                                        <Button
                                            size="small"
                                            variant="outlined"
                                            onClick={() => handleAdmin(u, !u.admin)}
                                            disabled={busyId === u.id}
                                        >
                                            {u.admin ? "Revoke admin" : "Make admin"}
                                        </Button>
                                        <Button
                                            size="small"
                                            variant="outlined"
                                            startIcon={<KeyIcon fontSize="small" />}
                                            onClick={() => handleResetPassword(u)}
                                            disabled={busyId === u.id}
                                        >
                                            Reset password
                                        </Button>
                                    </Stack>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>
        </Stack>
    );
}
