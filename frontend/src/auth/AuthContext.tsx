import {
  createContext,
  useCallback,
  useEffect,
  useMemo,
  useState,
} from "react";
import type { ReactNode } from "react";
import { useNavigate } from "react-router-dom";
import { apiFetch, subscribeAuthEvents } from "../api/client";
import { ApiError, isApiError } from "../api/errors";
import {
  clearCached,
  getCached,
  setCached,
  DEFAULT_CACHE_TTL,
} from "../api/cache";

export type AuthUser = {
  id: string;
  email: string;
  displayName?: string | null;
  isAdmin?: boolean;
};

type AuthContextValue = {
  user: AuthUser | null;
  loading: boolean;
  error: string | null;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  refreshMe: (forceRefresh?: boolean) => Promise<void>;
};

const ME_CACHE_KEY = "auth:me";

export const AuthContext = createContext<AuthContextValue | undefined>(
  undefined
);

export function AuthProvider({ children }: { children: ReactNode }) {
  const navigate = useNavigate();
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const refreshMe = useCallback(async (forceRefresh = false) => {
    setError(null);

    if (!forceRefresh) {
      const cached = getCached<AuthUser>(ME_CACHE_KEY);
      if (cached) {
        setUser(cached);
        return;
      }
    }

    setLoading(true);
    try {
      const me = await apiFetch<AuthUser>("/auth/me");
      setCached(ME_CACHE_KEY, me, DEFAULT_CACHE_TTL, true);
      setUser(me);
    } catch (err) {
      if (isApiError(err) && (err.status === 401 || err.status === 403)) {
        clearCached(ME_CACHE_KEY);
        setUser(null);
      } else {
        const apiErr = err as ApiError;
        setError(apiErr.message || "Failed to load session");
      }
    } finally {
      setLoading(false);
    }
  }, []);

  const login = useCallback(
    async (email: string, password: string) => {
      setLoading(true);
      setError(null);
      try {
        await apiFetch("/auth/login", {
          method: "POST",
          body: { email, password },
        });
        await refreshMe(true);
      } catch (err) {
        const apiErr = err as ApiError;
        setError(apiErr.message || "Login failed");
        throw err;
      } finally {
        setLoading(false);
      }
    },
    [refreshMe]
  );

  const logout = useCallback(async () => {
    try {
      await apiFetch("/auth/logout", { method: "POST" });
    } finally {
      clearCached(ME_CACHE_KEY);
      setUser(null);
    }
  }, []);

  useEffect(() => {
    const unsubscribe = subscribeAuthEvents(() => {
      clearCached(ME_CACHE_KEY);
      setUser(null);
      setError(null);
      setLoading(false);
      navigate("/login", { replace: true });
    });
    return unsubscribe;
  }, [navigate]);

  const value = useMemo<AuthContextValue>(
    () => ({ user, loading, error, login, logout, refreshMe }),
    [user, loading, error, login, logout, refreshMe]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
