const memoryCache = new Map<string, { value: unknown; expiresAt: number }>();

const TEN_MINUTES_MS = 10 * 60 * 1000;

function now() {
  return Date.now();
}

function isExpired(entry: { expiresAt: number }) {
  return entry.expiresAt <= now();
}

function safeGetStorage(): Storage | null {
  if (typeof window === "undefined") return null;
  try {
    return window.sessionStorage;
  } catch {
    return null;
  }
}

function storageKey(key: string) {
  return `cache:${key}`;
}

function readFromStorage(key: string) {
  const storage = safeGetStorage();
  if (!storage) return null;
  const raw = storage.getItem(storageKey(key));
  if (!raw) return null;
  try {
    const entry = JSON.parse(raw) as { value: unknown; expiresAt: number };
    if (isExpired(entry)) {
      storage.removeItem(storageKey(key));
      return null;
    }
    return entry;
  } catch {
    storage.removeItem(storageKey(key));
    return null;
  }
}

function writeToStorage(
  key: string,
  entry: { value: unknown; expiresAt: number }
) {
  const storage = safeGetStorage();
  if (!storage) return;
  try {
    storage.setItem(storageKey(key), JSON.stringify(entry));
  } catch {
    // ignore quota or serialization errors
  }
}

export function getCached<T>(key: string): T | null {
  const inMemory = memoryCache.get(key);
  if (inMemory && !isExpired(inMemory)) {
    return inMemory.value as T;
  }
  if (inMemory && isExpired(inMemory)) {
    memoryCache.delete(key);
  }

  const stored = readFromStorage(key);
  if (stored) {
    memoryCache.set(key, stored);
    return stored.value as T;
  }

  return null;
}

export function setCached<T>(
  key: string,
  value: T,
  ttlMs: number = TEN_MINUTES_MS,
  persist = false
): T {
  const entry = { value, expiresAt: now() + ttlMs };
  memoryCache.set(key, entry);
  if (persist) {
    writeToStorage(key, entry);
  }
  return value;
}

export function clearCached(key: string) {
  memoryCache.delete(key);
  const storage = safeGetStorage();
  if (storage) {
    storage.removeItem(storageKey(key));
  }
}

export function clearByPrefix(prefix: string) {
  for (const key of Array.from(memoryCache.keys())) {
    if (key.startsWith(prefix)) {
      memoryCache.delete(key);
    }
  }

  const storage = safeGetStorage();
  if (!storage) return;
  for (let i = storage.length - 1; i >= 0; i -= 1) {
    const k = storage.key(i);
    if (k && k.startsWith(storageKey(prefix))) {
      storage.removeItem(k);
    }
  }
}

export function clearAllCached() {
  memoryCache.clear();
  const storage = safeGetStorage();
  if (!storage) return;
  for (let i = storage.length - 1; i >= 0; i -= 1) {
    const k = storage.key(i);
    if (k && k.startsWith("cache:")) {
      storage.removeItem(k);
    }
  }
}

export const DEFAULT_CACHE_TTL = TEN_MINUTES_MS;
