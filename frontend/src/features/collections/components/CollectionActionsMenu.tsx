import { Add, Settings } from "@mui/icons-material";
import { Button, Stack } from "@mui/material";

export default function CollectionActionsMenu({
    role,
    onAddItem,
    onOpenSettings,
}: {
    role?: string;
    onAddItem?: () => void;
    onOpenSettings?: () => void;
}) {
    const roleUpper = role?.toUpperCase();
    const canAdd = roleUpper === "OWNER" || roleUpper === "ADMIN" || roleUpper === "EDITOR";
    const canManage = roleUpper === "OWNER" || roleUpper === "ADMIN";

    if (!canAdd && !canManage) return null;

    return (
        <Stack direction="row" spacing={1} flexShrink={0}>
            {canAdd && (
                <Button variant="contained" startIcon={<Add />} onClick={onAddItem}>
                    Add Item
                </Button>
            )}
            {canManage && (
                <Button variant="outlined" startIcon={<Settings />} onClick={onOpenSettings}>
                    Collection Settings
                </Button>
            )}
        </Stack>
    );
}
