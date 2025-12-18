import {
    Dialog,
    DialogContent,
    DialogTitle,
    IconButton,
    useMediaQuery,
    useTheme,
    Box,
} from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";

export default function ImageViewerDialog({
    open,
    src,
    title,
    onClose,
}: {
    open: boolean;
    src: string | null;
    title?: string;
    onClose: () => void;
}) {
    const theme = useTheme();
    const fullScreen = useMediaQuery(theme.breakpoints.down("sm"));

    return (
        <Dialog open={open} onClose={onClose} maxWidth="lg" fullWidth fullScreen={fullScreen}>
            <DialogTitle sx={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                {title || "Image"}
                <IconButton onClick={onClose} size="small">
                    <CloseIcon />
                </IconButton>
            </DialogTitle>
            <DialogContent sx={{ display: "flex", justifyContent: "center", alignItems: "center" }}>
                {src && (
                    <Box
                        component="img"
                        src={src}
                        alt={title || "Image"}
                        sx={{
                            maxWidth: "100%",
                            maxHeight: "80vh",
                            objectFit: "contain",
                        }}
                    />
                )}
            </DialogContent>
        </Dialog>
    );
}
