import { apiFetch } from "./client";
import { clearCached, getCached, setCached, DEFAULT_CACHE_TTL } from "./cache";
import type { SetupStatus } from "./types";

const SETUP_STATUS_CACHE_KEY = "setup:status";

export type GetSetupStatusOptions = {
  forceRefresh?: boolean;
  useCache?: boolean;
  ttlMs?: number;
};

export async function getSetupStatus({
  forceRefresh = false,
  useCache = true,
  ttlMs = DEFAULT_CACHE_TTL,
}: GetSetupStatusOptions = {}): Promise<SetupStatus> {
  if (useCache && !forceRefresh) {
    const cached = getCached<SetupStatus>(SETUP_STATUS_CACHE_KEY);
    if (cached) return cached;
  }

  const status = await apiFetch<SetupStatus>("/setup/status");
  setCached(SETUP_STATUS_CACHE_KEY, status, ttlMs, true);
  return status;
}

export function clearSetupStatusCache() {
  clearCached(SETUP_STATUS_CACHE_KEY);
}
