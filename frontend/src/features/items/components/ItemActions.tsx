import DeleteIcon from "@mui/icons-material/Delete";
import EditIcon from "@mui/icons-material/Edit";
import { Button, IconButton, Stack, Tooltip } from "@mui/material";
import type { ModuleStateDef } from "../../../api/types";
import StateDropdown from "./StateDropdown";

export default function ItemActions({
    role,
    stateKey,
    states,
    onChangeState,
    onEdit,
    onDelete,
    compact,
    disabled,
}: {
    role?: string;
    stateKey: string;
    states: ModuleStateDef[];
    onChangeState?: (stateKey: string) => void;
    onEdit?: () => void;
    onDelete?: () => void;
    compact?: boolean;
    disabled?: boolean;
}) {
    const upperRole = role?.toUpperCase();
    const canEdit = upperRole === "OWNER" || upperRole === "ADMIN" || upperRole === "EDITOR";
    const canDelete = upperRole === "OWNER" || upperRole === "ADMIN";

    return (
        <Stack direction="row" spacing={1} alignItems="center">
            {onChangeState && states.length ? (
                <StateDropdown states={states} value={stateKey} onChange={onChangeState} disabled={!canEdit || disabled} />
            ) : null}
            {canEdit && onEdit && (
                compact ? (
                    <Tooltip title="Edit">
                        <IconButton size="small" onClick={onEdit} disabled={disabled}>
                            <EditIcon fontSize="small" />
                        </IconButton>
                    </Tooltip>
                ) : (
                    <Button variant="outlined" startIcon={<EditIcon />} onClick={onEdit} disabled={disabled}>
                        Edit
                    </Button>
                )
            )}
            {canDelete && onDelete && (
                compact ? (
                    <Tooltip title="Delete">
                        <IconButton size="small" color="error" onClick={onDelete} disabled={disabled}>
                            <DeleteIcon fontSize="small" />
                        </IconButton>
                    </Tooltip>
                ) : (
                    <Button variant="outlined" color="error" startIcon={<DeleteIcon />} onClick={onDelete} disabled={disabled}>
                        Delete
                    </Button>
                )
            )}
        </Stack>
    );
}
