import { Box, Stack, Typography } from "@mui/material";
import type { ProviderAsset } from "../types";

type AssetGalleryProps = {
    assets: ProviderAsset[];
    label?: string;
    selectable?: boolean;
    selectedUrl?: string;
    onSelect?: (asset: ProviderAsset) => void;
    onPreview?: (asset: ProviderAsset) => void;
};

export default function AssetGallery({ assets, label, selectable, selectedUrl, onSelect, onPreview }: AssetGalleryProps) {
    if (assets.length === 0) return null;

    return (
        <Stack spacing={1}>
            {label && (
                <Typography variant="subtitle2" color="text.secondary">
                    {label}
                </Typography>
            )}
            <Box
                sx={{
                    display: "grid",
                    gridTemplateColumns: "repeat(auto-fit, minmax(120px, 1fr))",
                    gap: 1,
                }}
            >
                {assets.map((asset) => (
                    <Box
                        key={asset.url}
                        onClick={() => {
                            onPreview?.(asset);
                            if (selectable) onSelect?.(asset);
                        }}
                        onKeyDown={(event) => {
                            if (event.key === "Enter" || event.key === " ") {
                                event.preventDefault();
                                onPreview?.(asset);
                                if (selectable) onSelect?.(asset);
                            }
                        }}
                        role="button"
                        tabIndex={0}
                        sx={(theme) => {
                            const isSelected = selectable && Boolean(selectedUrl) && asset.url === selectedUrl;
                            return {
                                borderRadius: 1,
                                overflow: "hidden",
                                border: 1,
                                borderColor: isSelected ? theme.palette.primary.main : theme.palette.divider,
                                position: "relative",
                                bgcolor: theme.palette.background.default,
                                cursor: onPreview ? "pointer" : "default",
                                boxShadow: isSelected ? `0 0 0 2px ${theme.palette.primary.main}` : undefined,
                                outline: "none",
                                transition: "border-color 120ms linear",
                                "&:focus-visible": onPreview ? { outline: `2px solid ${theme.palette.primary.light}` } : undefined,
                            };
                        }}
                    >
                        <Box
                            component="img"
                            src={asset.url}
                            alt={asset.type || "asset"}
                            sx={{ width: "100%", height: 120, objectFit: "cover", display: "block" }}
                        />
                        {asset.type && (
                            <Box sx={{ p: 0.5, textAlign: "center", backgroundColor: "rgba(0,0,0,0.6)" }}>
                                <Typography variant="caption" color="common.white" sx={{ textTransform: "capitalize" }}>
                                    {asset.type}
                                </Typography>
                            </Box>
                        )}
                        {selectable && selectedUrl === asset.url && (
                            <Box
                                sx={{
                                    position: "absolute",
                                    inset: 0,
                                    display: "flex",
                                    alignItems: "flex-end",
                                    justifyContent: "flex-end",
                                    p: 0.5,
                                }}
                            >
                                <Typography
                                    variant="caption"
                                    color="common.white"
                                    sx={{ px: 0.5, py: 0.25, backgroundColor: "rgba(0,0,0,0.7)", borderRadius: 1 }}
                                >
                                    Selected
                                </Typography>
                            </Box>
                        )}
                    </Box>
                ))}
            </Box>
        </Stack>
    );
}
