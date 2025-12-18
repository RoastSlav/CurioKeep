import { Checkbox, Chip, Stack, TableCell, TableRow, Typography } from "@mui/material";
import type { Item, ModuleDefinition, ModuleStateDef } from "../../../api/types";
import StateDropdown from "./StateDropdown";

function formatValue(value: unknown): string {
    if (value === null || value === undefined) return "";
    if (Array.isArray(value)) return value.join(", ");
    if (typeof value === "object") return JSON.stringify(value);
    return String(value);
}

export default function ItemRow({
    item,
    moduleDefinition,
    onClick,
    states,
    onChangeState,
    canChangeState,
    showSelection,
    selected,
    onToggleSelect,
}: {
    item: Item;
    moduleDefinition?: ModuleDefinition | null;
    onClick?: (item: Item) => void;
    states?: ModuleStateDef[];
    onChangeState?: (item: Item, next: string) => void;
    canChangeState?: boolean;
    showSelection?: boolean;
    selected?: boolean;
    onToggleSelect?: (itemId: string, checked: boolean) => void;
}) {
    const firstIdentifier = item.identifiers?.[0];
    const fieldsToShow = (moduleDefinition?.fields || [])
        .slice()
        .sort((a, b) => (a.flags?.order ?? Number.MAX_SAFE_INTEGER) - (b.flags?.order ?? Number.MAX_SAFE_INTEGER))
        .slice(0, 3);
    const stateLabel = moduleDefinition?.states?.find((s) => s.key === item.stateKey)?.label;

    return (
        <TableRow hover sx={{ cursor: onClick ? "pointer" : "default" }} onClick={onClick ? () => onClick(item) : undefined}>
            {showSelection ? (
                <TableCell padding="checkbox">
                    <Checkbox
                        size="small"
                        checked={Boolean(selected)}
                        onChange={(e) => onToggleSelect?.(item.id, e.target.checked)}
                        onClick={(e) => e.stopPropagation()}
                        indeterminate={false}
                    />
                </TableCell>
            ) : null}
            <TableCell>
                <Stack spacing={0.25}>
                    <Typography fontWeight={600}>{item.id}</Typography>
                    {firstIdentifier && (
                        <Typography variant="caption" color="text.secondary">
                            {firstIdentifier.type}: {firstIdentifier.value}
                        </Typography>
                    )}
                </Stack>
            </TableCell>
            <TableCell>
                {canChangeState && states?.length ? (
                    <StateDropdown
                        states={states}
                        value={item.stateKey}
                        onChange={(next) => onChangeState?.(item, next)}
                    />
                ) : (
                    <Chip size="small" label={stateLabel || item.stateKey} />
                )}
            </TableCell>
            {fieldsToShow.length ? (
                <TableCell sx={{ maxWidth: 420 }}>
                    <Stack spacing={0.25}>
                        {fieldsToShow.map((field) => (
                            <Stack key={field.key} direction="row" spacing={0.75} alignItems="baseline">
                                <Typography variant="caption" color="text.secondary" noWrap>
                                    {field.label || field.key}:
                                </Typography>
                                <Typography variant="body2" noWrap>
                                    {formatValue((item.attributes || {})[field.key]) || "-"}
                                </Typography>
                            </Stack>
                        ))}
                    </Stack>
                </TableCell>
            ) : null}
            <TableCell sx={{ whiteSpace: "nowrap" }}>{item.createdAt || ""}</TableCell>
            <TableCell sx={{ whiteSpace: "nowrap" }}>{item.updatedAt || ""}</TableCell>
        </TableRow>
    );
}
