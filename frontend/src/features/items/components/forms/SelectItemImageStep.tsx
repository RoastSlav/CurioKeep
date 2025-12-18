import { useMemo, useState } from "react";
import {
    Alert,
    Box,
    Button,
    Dialog,
    DialogContent,
    DialogTitle,
    ImageList,
    ImageListItem,
    ImageListItemBar,
    Stack,
    Typography,
} from "@mui/material";
import type { ProviderAsset } from "../../../providers/providerTypes";

export type SelectedImage = { kind: "provider-url"; url: string } | { kind: "upload"; file: File } | null;

export default function SelectItemImageStep({
    assets,
    selected,
    onSelect,
    onNext,
    onSkip,
    onBack,
}: {
    assets?: ProviderAsset[];
    selected: SelectedImage;
    onSelect: (value: SelectedImage) => void;
    onNext: () => void;
    onSkip: () => void;
    onBack?: () => void;
}) {
    const [previewUrl, setPreviewUrl] = useState<string | null>(null);
    const [previewTitle, setPreviewTitle] = useState<string | undefined>(undefined);
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
        <Stack spacing={2}>
            <Typography variant="subtitle1" fontWeight={700}>
                Select picture
            </Typography>

            <Stack spacing={1}>
                <Typography variant="subtitle2" fontWeight={700}>
                    Provider images
                </Typography>
                {!imageAssets.length ? (
                    <Alert severity="info">No images returned from providers.</Alert>
                ) : (
                    <ImageList cols={3} gap={12} sx={{ width: "100%" }}>
                        {imageAssets.map((asset) => (
                            <ImageListItem key={asset.url} sx={{ cursor: "pointer" }}>
                                <img
                                    src={asset.url}
                                    alt={asset.label || "Provider image"}
                                    loading="lazy"
                                    onClick={() => {
                                        setPreviewUrl(asset.url);
                                        setPreviewTitle(asset.label || asset.providerKey || "Provider image");
                                    }}
                                />
                                <ImageListItemBar
                                    title={asset.label || asset.providerKey || ""}
                                    position="below"
                                    actionIcon={
                                        <Button
                                            size="small"
                                            variant={selectedUrl === asset.url ? "contained" : "outlined"}
                                            onClick={() => handleSelectProvider(asset)}
                                        >
                                            {selectedUrl === asset.url ? "Selected" : "Select"}
                                        </Button>
                                    }
                                    sx={{
                                        ".MuiImageListItemBar-actionIcon": { alignSelf: "center", pr: 0 },
                                    }}
                                />
                            </ImageListItem>
                        ))}
                    </ImageList>
                )}
            </Stack>

            <Stack spacing={1}>
                <Typography variant="subtitle2" fontWeight={700}>
                    Upload your own
                </Typography>
                <Stack direction="row" spacing={1} alignItems="center">
                    <Button variant="outlined" component="label">
                        Choose file
                        <input
                            hidden
                            type="file"
                            accept="image/*"
                            onChange={(e) => handleUploadChange(e.target.files?.[0])}
                        />
                    </Button>
                    {localPreview && (
                        <Stack spacing={0.5}>
                            <Box
                                component="img"
                                src={localPreview}
                                alt="Upload preview"
                                sx={{ width: 120, height: 120, objectFit: "cover", borderRadius: 1, border: 1, borderColor: "divider" }}
                                onClick={() => setPreviewUrl(localPreview)}
                            />
                            <Button size="small" variant="contained" onClick={() => onNext()}>
                                Use this
                            </Button>
                        </Stack>
                    )}
                </Stack>
            </Stack>

            <Stack direction="row" spacing={1} justifyContent="flex-end">
                {onBack && (
                    <Button variant="outlined" onClick={onBack}>
                        Back
                    </Button>
                )}
                <Button variant="outlined" onClick={onSkip}>
                    Skip
                </Button>
                <Button variant="contained" onClick={onNext}>
                    Continue
                </Button>
            </Stack>

            <Dialog open={Boolean(previewUrl)} onClose={() => setPreviewUrl(null)} maxWidth="md" fullWidth>
                <DialogTitle>{previewTitle || "Preview"}</DialogTitle>
                <DialogContent>
                    {previewUrl && (
                        <Box
                            component="img"
                            src={previewUrl}
                            alt={previewTitle || "Preview"}
                            sx={{ width: "100%", maxHeight: "70vh", objectFit: "contain" }}
                        />
                    )}
                </DialogContent>
            </Dialog>
        </Stack>
    );
}
