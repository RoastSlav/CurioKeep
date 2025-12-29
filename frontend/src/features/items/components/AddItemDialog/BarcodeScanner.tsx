import { useEffect, useRef, useState } from "react";
import {
  BrowserMultiFormatReader,
  type IScannerControls,
} from "@zxing/browser";
import { Button } from "../../../../../components/ui/button";
import { Alert, AlertDescription } from "../../../../../components/ui/alert";
import { cn } from "../../../../../lib/utils";
import type { ComponentProps } from "react";

export default function BarcodeScanner({
  onDetected,
  onClose,
  className,
  buttonProps,
}: {
  onDetected: (code: string) => void;
  onClose?: () => void;
  className?: string;
  buttonProps?: ComponentProps<typeof Button>;
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
        const controls = await reader.decodeFromVideoDevice(
          undefined,
          videoRef.current ?? undefined,
          (result, err, controls) => {
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
          }
        );
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
    <div className={cn("space-y-2", className)}>
      {!active && (
        <Button
          variant="outline"
          size="default"
          onClick={startScanning}
          {...buttonProps}
        >
          Scan barcode
        </Button>
      )}
      {active && (
        <div className="border-2 border-border rounded-md overflow-hidden brutal-shadow-sm">
          <video ref={videoRef} className="w-full aspect-video bg-black/40" />
        </div>
      )}
      {error && (
        <Alert variant="destructive">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}
      {!active && onClose && (
        <p className="text-xs text-muted-foreground">Or type manually below.</p>
      )}
    </div>
  );
}
