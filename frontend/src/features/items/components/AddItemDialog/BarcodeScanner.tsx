import { useEffect, useRef, useState } from "react";
import { Alert, Box, Button, Stack, Typography } from "@mui/material";
import { BrowserMultiFormatReader, type IScannerControls } from "@zxing/browser";

export default function BarcodeScanner({
    onDetected,
    onClose,
}: {
    onDetected: (code: string) => void;
    onClose?: () => void;
}) {
    const videoRef = useRef<HTMLVideoElement | null>(null);
    const readerRef = useRef<BrowserMultiFormatReader | null>(null);
    const controlsRef = useRef<IScannerControls | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [active, setActive] = useState(false);

    useEffect(() => {
        if (!active) return;
        const reader = new BrowserMultiFormatReader();
        readerRef.current = reader;
        setError(null);

        const start = async () => {
            try {
                const controls = await reader.decodeFromVideoDevice(undefined, videoRef.current ?? undefined, (result, err, controls) => {
                    if (result) {
                        const text = result.getText();
                        if (text) {
                            controlsRef.current = controls;
                            onDetected(text);
                            setActive(false);
                            controls.stop();
                            onClose?.();
                        }
                        return;
                    }

                    if (err && err.name !== "NotFoundException") {
                        setError(err.message || "Failed to scan");
                    }
                });
                controlsRef.current = controls;
            } catch (err: any) {
                setError(err?.message || "Failed to scan");
            }
        };

        void start();

        return () => {
            controlsRef.current?.stop();
            readerRef.current = null;
        };
    }, [active, onDetected, onClose]);

    const startScanning = () => {
        if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
            setError("Camera not supported on this device.");
            return;
        }
        setActive(true);
    };

    return (
        <Stack spacing={1.5}>
            {!active && (
                <Button variant="outlined" onClick={startScanning}>
                    Scan barcode
                </Button>
            )}
            {active && (
                <Box>
                    <video ref={videoRef} style={{ width: "100%" }} />
                </Box>
            )}
            {error && <Alert severity="error">{error}</Alert>}
            {!active && onClose && (
                <Typography variant="body2" color="text.secondary">
                    Or type manually below.
                </Typography>
            )}
        </Stack>
    );
}
