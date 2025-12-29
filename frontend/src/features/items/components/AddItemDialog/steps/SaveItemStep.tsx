import { useState } from "react";
import type { Item, ModuleDefinition } from "../../../../../api/types";
import { createItem, setItemImageFromUrl, uploadItemImage } from "../../../api";
import ItemForm from "../../../components/ItemForm";
import type { SelectedImage } from "../../forms/SelectItemImageStep";
import { Alert, AlertDescription } from "../../../../../../components/ui/alert";
import { Button } from "../../../../../../components/ui/button";

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
    <div className="space-y-3">
      <div className="flex items-center justify-between gap-2">
        <h3 className="text-sm font-bold uppercase">Save item</h3>
      </div>

      {error && (
        <Alert variant="destructive">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      <ItemForm
        moduleDefinition={moduleDefinition}
        initialAttributes={attributes}
        submitLabel={loading ? "Saving..." : "Save item"}
        cancelLabel="Back"
        disabled={loading}
        onCancel={onBack}
        onSubmit={async (attrs: Record<string, any>) => {
          setLoading(true);
          setError(null);
          try {
            let item = await createItem(collectionId, {
              moduleId,
              attributes: attrs,
              stateKey: defaultState,
            });

            if (selectedImage?.kind === "provider-url") {
              item = await setItemImageFromUrl(
                collectionId,
                item.id,
                selectedImage.url
              );
            } else if (selectedImage?.kind === "upload") {
              item = await uploadItemImage(
                collectionId,
                item.id,
                selectedImage.file
              );
            }

            onSaved(item);
          } catch (err: any) {
            setError(err?.message || "Failed to save item");
          } finally {
            setLoading(false);
          }
        }}
      />
    </div>
  );
}
