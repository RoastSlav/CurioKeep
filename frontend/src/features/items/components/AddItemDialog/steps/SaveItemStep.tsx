import { useState } from "react";
import { Alert, Stack, Typography } from "@mui/material";
import type { Item, ModuleDefinition } from "../../../../../api/types";
import { createItem, setItemImageFromUrl, uploadItemImage } from "../../../api";
import ItemForm from "../../../components/ItemForm";
import type { SelectedImage } from "../../forms/SelectItemImageStep";

export default function SaveItemStep({
    collectionId,
    moduleId,
    attributes,
    selectedImage,
    defaultState,
    onSaved,
    onBack,
    moduleDefinition,
}: {
    collectionId: string;
    moduleId: string;
    attributes: Record<string, any>;
    selectedImage: SelectedImage;
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
                        let item = await createItem(collectionId, { moduleId, attributes: attrs, stateKey: defaultState });

                        if (selectedImage?.kind === "provider-url") {
                            item = await setItemImageFromUrl(collectionId, item.id, selectedImage.url);
                        } else if (selectedImage?.kind === "upload") {
                            item = await uploadItemImage(collectionId, item.id, selectedImage.file);
                        }

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
