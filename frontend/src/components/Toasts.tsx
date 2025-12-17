import { Alert, Snackbar } from "@mui/material";
import { createContext, useCallback, useContext, useMemo, useState } from "react";

export type Toast = { id: number; message: string; severity?: "success" | "info" | "warning" | "error" };

type ToastContextValue = {
    show: (message: string, severity?: Toast["severity"]) => void;
};

const ToastContext = createContext<ToastContextValue | undefined>(undefined);

export function useToasts() {
    const ctx = useContext(ToastContext);
    if (!ctx) throw new Error("ToastContext missing");
    return ctx;
}

export function ToastProvider({ children }: { children: React.ReactNode }) {
    const [queue, setQueue] = useState<Toast[]>([]);

    const show = useCallback((message: string, severity: Toast["severity"] = "info") => {
        setQueue((prev) => [...prev, { id: Date.now(), message, severity }]);
    }, []);

    const handleClose = (id: number) => () => setQueue((prev) => prev.filter((t) => t.id !== id));

    const value = useMemo(() => ({ show }), [show]);

    return (
        <ToastContext.Provider value={value}>
            {children}
            {queue.map((toast) => (
                <Snackbar key={toast.id} open autoHideDuration={4000} onClose={handleClose(toast.id)} anchorOrigin={{ vertical: "bottom", horizontal: "right" }}>
                    <Alert onClose={handleClose(toast.id)} severity={toast.severity} variant="filled" sx={{ width: "100%" }}>
                        {toast.message}
                    </Alert>
                </Snackbar>
            ))}
        </ToastContext.Provider>
    );
}
