import {Button, Dialog, DialogActions, DialogContent, DialogTitle, Stack, Typography, Box} from "@mui/material";
import type {ProviderAsset} from "../types";

type AssetPreviewDialogProps = {
    open: boolean;
    asset: ProviderAsset | null;
    title?: string;
    useLabel?: string;
    onClose: () => void;
    onUse?: () => void;
};

export default function AssetPreviewDialog({open, asset, title, useLabel, onClose, onUse}: AssetPreviewDialogProps) {
    if (!asset) return null;

    return (
        <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
            <DialogTitle>{title || "Image preview"}</DialogTitle>
            <DialogContent>
                <Stack spacing={2} alignItems="center">
                    <Typography variant="subtitle2" color="text.secondary" sx={{wordBreak: "break-word"}}>
                        {asset.type ? `${asset.type} · ${asset.url}` : asset.url}
                    </Typography>
                    <Box
                        component="img"
                        src={asset.url}
                        alt={asset.type || "preview"}
                        loading="lazy"
                        style={{maxWidth: "100%", maxHeight: "70vh", borderRadius: 8, objectFit: "contain"}}
                    />
                </Stack>
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose}>Close</Button>
                {onUse && (
                    <Button onClick={() => {
                        onUse();
                        onClose();
                    }} variant="contained">
                        {useLabel || "Use this image"}
                    </Button>
                )}
            </DialogActions>
        </Dialog>
    );
}
