import { Alert, Box, Button, Dialog, DialogActions, DialogContent, DialogTitle, Stack, Typography } from "@mui/material";
import { useCallback, useMemo, useState } from "react";
import LoadingState from "../../../components/LoadingState";

type Props = {
    open: boolean;
    moduleKey?: string;
    xml?: string;
    error?: string;
    loading?: boolean;
    onClose: () => void;
};

export default function ModuleRawXmlDialog({ open, moduleKey, xml, error, loading = false, onClose }: Props) {
    const [copied, setCopied] = useState(false);
    const bodyText = useMemo(() => error ?? xml ?? "<no XML available>", [error, xml]);
    const canCopy = Boolean(xml && !loading && !error);

    const handleCopy = useCallback(async () => {
        if (!xml) return;
        try {
            await navigator.clipboard.writeText(xml);
            setCopied(true);
            setTimeout(() => setCopied(false), 1500);
        } catch (copyError) {
            console.error("Copy failed", copyError);
        }
    }, [xml]);

    return (
        <Dialog open={open} onClose={onClose} fullWidth maxWidth="md">
            <DialogTitle>{moduleKey ? `${moduleKey} XML` : "Module XML"}</DialogTitle>
            <DialogContent dividers>
                {loading ? (
                    <LoadingState message="Loading raw XML..." />
                ) : error ? (
                    <Alert severity="error">{error}</Alert>
                ) : (
                    <Box
                        component="pre"
                        sx={{
                            whiteSpace: "pre-wrap",
                            wordBreak: "break-word",
                            maxHeight: "60vh",
                            overflow: "auto",
                            fontSize: "0.75rem",
                        }}
                    >
                        {bodyText}
                    </Box>
                )}
            </DialogContent>
            <DialogActions>
                <Stack direction="row" spacing={1} alignItems="center">
                    <Button variant="outlined" onClick={handleCopy} disabled={!canCopy}>
                        {copied ? "Copied" : "Copy"}
                    </Button>
                    <Typography variant="caption" color="text.secondary">
                        {moduleKey}
                    </Typography>
                </Stack>
                <Button variant="contained" onClick={onClose}>
                    Close
                </Button>
            </DialogActions>
        </Dialog>
    );
}
