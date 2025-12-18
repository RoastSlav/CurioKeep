import { Cancel, ContentCopy } from "@mui/icons-material";
import { Alert, Box, Chip, IconButton, Stack, Table, TableBody, TableCell, TableHead, TableRow, Tooltip, Typography } from "@mui/material";
import type { CollectionInvite } from "../../../../api/types";

function buildLink(token: string) {
    if (typeof window === "undefined") return `/invites/collection/${token}`;
    return `${window.location.origin}/invites/collection/${token}`;
}

type Props = {
    invites: CollectionInvite[];
    onCopy?: (token: string) => void;
    onRevoke?: (token: string) => void | Promise<void>;
};

export default function PendingInvitesSection({ invites, onCopy, onRevoke }: Props) {
    if (!invites.length) return null;

    return (
        <Stack spacing={1.5}>
            <Typography variant="h6" fontWeight={700}>
                Pending invites
            </Typography>
            <Box sx={{ overflowX: "auto" }}>
                <Table size="small">
                    <TableHead>
                        <TableRow>
                            <TableCell>Token</TableCell>
                            <TableCell>Role</TableCell>
                            <TableCell>Expires</TableCell>
                            <TableCell align="right">Actions</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {invites.map((invite) => (
                            <TableRow key={invite.token} hover>
                                <TableCell sx={{ maxWidth: 260 }}>
                                    <Tooltip title={invite.token}>
                                        <Typography variant="body2" sx={{ wordBreak: "break-all" }}>
                                            {invite.token}
                                        </Typography>
                                    </Tooltip>
                                </TableCell>
                                <TableCell>
                                    <Chip label={invite.role} size="small" />
                                </TableCell>
                                <TableCell>
                                    {invite.expiresAt ? new Date(invite.expiresAt).toLocaleString() : "Never"}
                                </TableCell>
                                <TableCell align="right">
                                    <Stack direction="row" spacing={1} justifyContent="flex-end">
                                        <IconButton
                                            size="small"
                                            onClick={() => {
                                                void navigator.clipboard.writeText(buildLink(invite.token));
                                                onCopy?.(invite.token);
                                            }}
                                        >
                                            <ContentCopy fontSize="small" />
                                        </IconButton>
                                        {onRevoke && (
                                            <IconButton
                                                size="small"
                                                color="error"
                                                onClick={() => void onRevoke(invite.token)}
                                            >
                                                <Cancel fontSize="small" />
                                            </IconButton>
                                        )}
                                    </Stack>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </Box>
            {onRevoke ? null : <Alert severity="info">Revoking invites not yet supported in API.</Alert>}
        </Stack>
    );
}
