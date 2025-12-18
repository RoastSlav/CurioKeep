import { useState } from "react";
import { Alert, Stack, Typography } from "@mui/material";
import type { Item, ModuleDefinition } from "../../../../../api/types";
import { createItem } from "../../../api";
import ItemForm from "../../../components/ItemForm";

export default function SaveItemStep({
    collectionId,
    moduleId,
    attributes,
    defaultState,
    onSaved,
    onBack,
    moduleDefinition,
}: {
    collectionId: string;
    moduleId: string;
    attributes: Record<string, any>;
    defaultState?: string;
    onSaved: (item: Item) => void;
    onBack?: () => void;
    moduleDefinition: ModuleDefinition | null | undefined;
}) {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    return (
        <Stack spacing={2}>
            <Typography variant="subtitle1" fontWeight={700}>
                Save item
            </Typography>
            <Typography variant="body2" color="text.secondary">
                Review and edit fields before saving.
            </Typography>
            {error && <Alert severity="error">{error}</Alert>}
            <ItemForm
                moduleDefinition={moduleDefinition}
                initialAttributes={attributes}
                submitLabel={loading ? "Saving..." : "Save item"}
                disabled={loading}
                onCancel={onBack}
                onSubmit={async (attrs: Record<string, any>) => {
                    setLoading(true);
                    setError(null);
                    try {
                        const item = await createItem(collectionId, { moduleId, attributes: attrs, stateKey: defaultState });
                        onSaved(item);
                    } catch (err: any) {
                        setError(err?.message || "Failed to save item");
                    } finally {
                        setLoading(false);
                    }
                }}
            />
        </Stack>
    );
}
