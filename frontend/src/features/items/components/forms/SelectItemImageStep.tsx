import { useMemo, useState } from "react";
import type { ProviderAsset } from "../../../providers/providerTypes";
import { Button } from "../../../../../components/ui/button";
import { Alert, AlertDescription } from "../../../../../components/ui/alert";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "../../../../../components/ui/dialog";
import { Separator } from "../../../../../components/ui/separator";

export type SelectedImage =
  | { kind: "provider-url"; url: string }
  | { kind: "upload"; file: File }
  | null;

export default function SelectItemImageStep({
  assets,
  selected,
  onSelect,
  onNext,
  onBack,
}: {
  assets?: ProviderAsset[];
  selected: SelectedImage;
  onSelect: (value: SelectedImage) => void;
  onNext: () => void;
  onBack?: () => void;
}) {
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [previewTitle, setPreviewTitle] = useState<string | undefined>(
    undefined
  );
  const [localPreview, setLocalPreview] = useState<string | null>(null);

  const imageAssets = useMemo(() => {
    const isImage = (a: ProviderAsset) => {
      if (a.type && a.type.toLowerCase().startsWith("image")) return true;
      return /\.(png|jpe?g|webp|gif|bmp)$/i.test(a.url);
    };
    return (assets || []).filter(isImage);
  }, [assets]);

  const handleSelectProvider = (asset: ProviderAsset) => {
    onSelect({ kind: "provider-url", url: asset.url });
    setLocalPreview(null);
  };

  const handleUploadChange = (file: File | undefined | null) => {
    if (!file) return;
    const preview = URL.createObjectURL(file);
    setLocalPreview(preview);
    onSelect({ kind: "upload", file });
  };

  const selectedUrl = selected?.kind === "provider-url" ? selected.url : null;

  return (
    <div className="space-y-4">
      <h3 className="text-sm font-bold uppercase">Select picture</h3>

      <div className="space-y-2">
        <div className="text-xs font-bold uppercase text-muted-foreground">
          Provider images
        </div>
        {!imageAssets.length ? (
          <Alert variant="default">
            <AlertDescription>
              No images returned from providers.
            </AlertDescription>
          </Alert>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {imageAssets.map((asset) => (
              <div
                key={asset.url}
                className="border-2 border-border rounded-md overflow-hidden brutal-shadow-sm bg-card flex flex-col"
              >
                <button
                  type="button"
                  className="block"
                  onClick={() => {
                    setPreviewUrl(asset.url);
                    setPreviewTitle(
                      asset.label || asset.providerKey || "Provider image"
                    );
                  }}
                >
                  <img
                    src={asset.url}
                    alt={asset.label || "Provider image"}
                    loading="lazy"
                    className="w-full h-48 object-cover"
                  />
                </button>
                <div className="flex items-center justify-between px-3 py-2 text-sm">
                  <div className="font-bold uppercase truncate pr-2">
                    {asset.label || asset.providerKey || ""}
                  </div>
                  <Button
                    variant={selectedUrl === asset.url ? "default" : "outline"}
                    size="sm"
                    onClick={() => handleSelectProvider(asset)}
                  >
                    {selectedUrl === asset.url ? "Selected" : "Select"}
                  </Button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      <Separator />

      <div className="space-y-2">
        <div className="text-xs font-bold uppercase text-muted-foreground">
          Upload your own
        </div>
        <div className="flex items-center gap-3 flex-wrap">
          <label className="inline-flex">
            <input
              type="file"
              accept="image/*"
              className="hidden"
              onChange={(e) => handleUploadChange(e.target.files?.[0])}
            />
            <Button asChild variant="outline">
              <span>Choose file</span>
            </Button>
          </label>
          {localPreview && (
            <div className="space-y-2">
              <img
                src={localPreview}
                alt="Upload preview"
                className="w-32 h-32 object-cover rounded-md border-2 border-border brutal-shadow-sm cursor-pointer"
                onClick={() => setPreviewUrl(localPreview)}
              />
              <Button size="sm" onClick={() => onNext()}>
                Use this
              </Button>
            </div>
          )}
        </div>
      </div>

      <div className="flex items-center justify-end gap-2 flex-wrap">
        {onBack && (
          <Button variant="outline" onClick={onBack}>
            Back
          </Button>
        )}
        <Button variant="default" onClick={onNext}>
          Next
        </Button>
      </div>

      <Dialog
        open={Boolean(previewUrl)}
        onOpenChange={(open) => !open && setPreviewUrl(null)}
      >
        <DialogContent className="sm:max-w-3xl">
          <DialogHeader>
            <DialogTitle>{previewTitle || "Preview"}</DialogTitle>
          </DialogHeader>
          {previewUrl && (
            <div className="w-full max-h-[70vh] flex items-center justify-center">
              <img
                src={previewUrl}
                alt={previewTitle || "Preview"}
                className="max-h-[70vh] object-contain"
              />
            </div>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}
