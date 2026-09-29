import { useEffect, useState } from "react"
import { getErrorMessage } from "@/api/errors"
import { getModuleDetails, type ModuleDetails } from "../api/modulesApi"

/** The compiled contract of a module. It is only ever the contract of the requested key, never the previous module's. */
export function useModuleDetails(moduleKey: string | null | undefined) {
    const [loaded, setLoaded] = useState<{ key: string; details: ModuleDetails } | null>(null)
    const [failed, setFailed] = useState<{ key: string; message: string } | null>(null)

    useEffect(() => {
        if (!moduleKey) return
        let cancelled = false
        getModuleDetails(moduleKey)
            .then((details) => {
                if (!cancelled) setLoaded({ key: moduleKey, details })
            })
            .catch((err: unknown) => {
                if (!cancelled) setFailed({ key: moduleKey, message: getErrorMessage(err, "Failed to load module") })
            })
        return () => {
            cancelled = true
        }
    }, [moduleKey])

    return {
        details: loaded && loaded.key === moduleKey ? loaded.details : null,
        error: failed && failed.key === moduleKey && !(loaded && loaded.key === moduleKey) ? failed.message : null,
    }
}
