"use client"

import {createContext, useContext, useMemo, type ReactNode} from "react"
import {toast as sonnerToast} from "sonner"
import {Toaster} from "../../components/ui/sonner"

type ToastContextValue = {
    showToast: (message: string, severity?: "success" | "error" | "warning" | "info", durationMs?: number) => void
}

const ToastContext = createContext<ToastContextValue | null>(null)

export function ToastProvider({ children }: { children: ReactNode }) {
    const value = useMemo<ToastContextValue>(
        () => ({
            showToast: (message, severity = "info", durationMs = 4000) => {
                const options = {duration: durationMs}

                switch (severity) {
                    case "success":
                        sonnerToast.success(message, options)
                        break
                    case "error":
                        sonnerToast.error(message, options)
                        break
                    case "warning":
                        sonnerToast.warning(message, options)
                        break
                    default:
                        sonnerToast.info(message, options)
                }
            },
        }),
        [],
    )

    return (
        <ToastContext.Provider value={value}>
            {children}
            <Toaster position="bottom-center"/>
        </ToastContext.Provider>
    )
}

export function useToast(): ToastContextValue {
    const ctx = useContext(ToastContext)
    if (!ctx) throw new Error("useToast must be used within a ToastProvider")
    return ctx
}
