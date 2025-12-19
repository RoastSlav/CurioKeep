"use client";

import { useEffect, useMemo, useState } from "react";
import { Edit, Trash2 } from "lucide-react";
import type { Item, ModuleDefinition } from "../../../api/types";
import { useToast } from "../../../components/Toasts";
import ItemForm from "./ItemForm";
import { changeItemState, deleteItem, getItem, updateItem } from "../api";
import LoadingState from "../../../components/LoadingState";
import ErrorState from "../../../components/ErrorState";
import ImageViewerDialog from "./ImageViewerDialog";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "../../../../components/ui/dialog";
import { Button } from "../../../../components/ui/button";
import StateDropdown from "./StateDropdown";

function formatValue(value: unknown): string {
  if (value === null || value === undefined) return "-";
  if (Array.isArray(value)) return value.join(", ");
  if (typeof value === "object") return JSON.stringify(value, null, 2);
  return String(value);
}

export default function ItemDetailModal({
  open,
  collectionId,
  itemId,
  moduleDefinition,
  role,
  onClose,
  onUpdated,
  onDeleted,
}: {
  open: boolean;
  collectionId: string;
  itemId: string;
  moduleDefinition: ModuleDefinition | null | undefined;
  role?: string;
  onClose: () => void;
  onUpdated?: (item: Item) => void;
  onDeleted?: () => void;
}) {
  const { showToast } = useToast();
  const [item, setItem] = useState<Item | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [editing, setEditing] = useState(false);
  const [changingState, setChangingState] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [reloadKey, setReloadKey] = useState(0);
  const [viewerOpen, setViewerOpen] = useState(false);

  useEffect(() => {
    if (!open) return;
    const fetchItem = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await getItem(collectionId, itemId);
        setItem(data);
      } catch (err: any) {
        setError(err?.message || "Failed to load item");
      } finally {
        setLoading(false);
      }
    };
    void fetchItem();
  }, [open, collectionId, itemId, reloadKey]);

  const fields = useMemo(
    () => moduleDefinition?.fields || [],
    [moduleDefinition]
  );
  const states = useMemo(
    () => moduleDefinition?.states || [],
    [moduleDefinition]
  );

  const handleStateChange = async (stateKey: string) => {
    if (!item) return;
    const prev = item;
    setItem({ ...item, stateKey });
    setChangingState(true);
    try {
      const updated = await changeItemState(collectionId, item.id, stateKey);
      setItem(updated);
      onUpdated?.(updated);
      showToast("State updated", "success");
    } catch (err: any) {
      setItem(prev);
      showToast(err?.message || "Failed to change state", "error");
    } finally {
      setChangingState(false);
    }
  };

  const handleSubmit = async (attributes: Record<string, any>) => {
    if (!item) return;
    const payload = { attributes } as Partial<Item>;
    const updated = await updateItem(collectionId, item.id, payload);
    setItem(updated);
    onUpdated?.(updated);
    setEditing(false);
    showToast("Item saved", "success");
  };

  const handleDelete = async () => {
    if (!item) return;
    const confirmed = window.confirm(
      "Delete this item? This cannot be undone."
    );
    if (!confirmed) return;
    setDeleting(true);
    try {
      await deleteItem(collectionId, item.id);
      showToast("Item deleted", "success");
      onDeleted?.();
      onClose();
    } catch (err: any) {
      showToast(err?.message || "Failed to delete item", "error");
    } finally {
      setDeleting(false);
    }
  };

  const stateLabel =
    states.find((s) => s.key === item?.stateKey)?.label || item?.stateKey;
  const imageUrl = (item?.attributes?.providerImageUrl as string) || null;
  const title = (item?.attributes?.title as string) || item?.id || "";

  const canEdit = role && ["OWNER", "ADMIN", "EDITOR"].includes(role);

  return (
    <Dialog open={open} onOpenChange={(isOpen) => !isOpen && onClose()}>
      <DialogContent
        className="max-w-4xl max-h-[90vh] overflow-y-auto"
        showCloseButton={true}
      >
        <DialogHeader>
          <DialogTitle className="text-xl font-bold uppercase">
            {(item?.attributes?.title as string) ||
              `Item ${item?.id?.slice(0, 8) || ""}`}
          </DialogTitle>
          <DialogDescription>View and edit item details</DialogDescription>
        </DialogHeader>

        <div className="py-4">
          {loading ? (
            <LoadingState message="Loading item..." />
          ) : error ? (
            <ErrorState
              title="Could not load item"
              message={error}
              onRetry={() => setReloadKey((k) => k + 1)}
            />
          ) : item && !editing ? (
            <div className="space-y-6">
              {canEdit && (
                <div className="flex items-center gap-3 flex-wrap">
                  <StateDropdown
                    value={item.stateKey}
                    states={states}
                    onChange={handleStateChange}
                    disabled={changingState || deleting}
                  />
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setEditing(true)}
                    disabled={changingState || deleting}
                    className="brutal-border"
                  >
                    <Edit className="w-4 h-4 mr-2" />
                    Edit
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={handleDelete}
                    disabled={changingState || deleting}
                    className="brutal-border text-destructive hover:bg-destructive hover:text-destructive-foreground bg-transparent"
                  >
                    <Trash2 className="w-4 h-4 mr-2" />
                    Delete
                  </Button>
                </div>
              )}

              {imageUrl && (
                <div
                  className="w-full border-2 border-border overflow-hidden cursor-pointer"
                  onClick={() => setViewerOpen(true)}
                >
                  <img
                    src={imageUrl || "/placeholder.svg"}
                    alt={title}
                    className="w-full max-h-[360px] object-contain bg-muted"
                  />
                </div>
              )}

              <div className="space-y-4">
                <h4 className="font-bold uppercase text-sm text-muted-foreground">
                  Details
                </h4>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {(fields.length
                    ? fields
                    : Object.keys(item.attributes || {}).map((key) => ({
                        key,
                        label: key,
                      }))
                  ).map((field) => (
                    <div
                      key={field.key}
                      className="space-y-1 p-3 bg-muted/50 border border-border"
                    >
                      <p className="text-xs font-bold uppercase text-muted-foreground">
                        {field.label || field.key}
                      </p>
                      <p className="whitespace-pre-wrap text-sm text-foreground">
                        {formatValue(item.attributes?.[field.key])}
                      </p>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          ) : item && editing ? (
            <ItemForm
              moduleDefinition={moduleDefinition}
              initialAttributes={item.attributes}
              onSubmit={handleSubmit}
              onCancel={() => setEditing(false)}
              disabled={changingState || deleting}
              submitLabel="Save"
            />
          ) : null}
        </div>
      </DialogContent>
      <ImageViewerDialog
        open={viewerOpen}
        src={imageUrl || "/placeholder.svg"}
        title={title || "Image"}
        onClose={() => setViewerOpen(false)}
      />
    </Dialog>
  );
}
