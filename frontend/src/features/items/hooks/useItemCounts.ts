import { useCallback, useEffect, useState } from "react"
import { fetchItemCounts, type ModuleItemCounts } from "../api"

/** How many items a collection holds per module and state; call `reload` after adding, changing or deleting items. */
export function useItemCounts(collectionId: string | undefined) {
    const [counts, setCounts] = useState<Record<string, ModuleItemCounts>>({})
    const [reloadCount, setReloadCount] = useState(0)

    const [seenCollection, setSeenCollection] = useState(collectionId)
    if (seenCollection !== collectionId) {
        setSeenCollection(collectionId)
        setCounts({})
    }

    useEffect(() => {
        if (!collectionId) return
        const controller = new AbortController()
        fetchItemCounts(collectionId, { signal: controller.signal })
            .then((response) => setCounts(response.modules))
            .catch(() => {
                // The counts only decorate the page; a failure here leaves the old numbers and the list reports real errors.
            })
        return () => controller.abort()
    }, [collectionId, reloadCount])

    const reload = useCallback(() => setReloadCount((count) => count + 1), [])
    return { counts, reload }
}
