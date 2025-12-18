import { Alert, Snackbar } from "@mui/material";
import type { AlertColor } from "@mui/material";
import { createContext, useContext, useMemo, useState } from "react";
import type { ReactNode } from "react";

type Toast = {
    id: number;
    message: string;
    severity?: AlertColor;
    durationMs?: number;
};

type ToastContextValue = {
    showToast: (message: string, severity?: AlertColor, durationMs?: number) => void;
};

const ToastContext = createContext<ToastContextValue | null>(null);

export function ToastProvider({ children }: { children: ReactNode }) {
    const [toast, setToast] = useState<Toast | null>(null);

    const value = useMemo<ToastContextValue>(() => ({
        showToast: (message, severity = "info", durationMs = 4000) => {
            setToast({ id: Date.now(), message, severity, durationMs });
        },
    }), []);

    const handleClose = () => setToast(null);

    return (
        <ToastContext.Provider value={value}>
            {children}
            <Snackbar
                open={Boolean(toast)}
                autoHideDuration={toast?.durationMs ?? 4000}
                onClose={handleClose}
                anchorOrigin={{ vertical: "bottom", horizontal: "center" }}
                children={toast ? (
                    <Alert elevation={3} severity={toast.severity ?? "info"} onClose={handleClose} sx={{ width: "100%" }}>
                        {toast.message}
                    </Alert>
                ) : undefined}
            />
        </ToastContext.Provider>
    );
}

export function useToast(): ToastContextValue {
    const ctx = useContext(ToastContext);
    if (!ctx) throw new Error("useToast must be used within a ToastProvider");
    return ctx;
}
