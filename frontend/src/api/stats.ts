// Placeholder for future stats endpoints. Currently the backend does not expose global stats.
// Keep this file so callers have a consistent import path.

export type GlobalStats = {
    collectionsCount?: number;
    itemsCount?: number;
    ownedCount?: number;
    wishlistCount?: number;
};

export async function fetchGlobalStats(): Promise<GlobalStats | null> {
    // eslint-disable-next-line no-console
    console.info("Global stats endpoint is not available yet. Add backend support to enable this.");
    return null;
}
