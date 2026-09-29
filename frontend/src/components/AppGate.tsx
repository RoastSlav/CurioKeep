import {
  useCallback,
  useEffect,
  useMemo,
  useState,
} from "react";
import type { ReactNode } from "react";
import { useNavigate } from "react-router-dom";
import { ApiError, isApiError } from "../api/errors";
import { getSetupStatus } from "../api/setup";
import { useAuth } from "../auth/useAuth";
import { SetupStatusContext, type SetupStatusContextValue } from "./setupStatusContext";

export default function AppGate({ children }: { children: ReactNode }) {
  const navigate = useNavigate();
  const { refreshMe } = useAuth();

  const [setupRequired, setSetupRequired] = useState<boolean>(false);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const checkSetup = useCallback(
    async (forceRefresh = false) => {
      setLoading(true);
      setError(null);
      try {
        const status = await getSetupStatus({ forceRefresh });
        setSetupRequired(status.setupRequired);

        if (status.setupRequired) {
          navigate("/setup", { replace: true });
          return;
        }

        await refreshMe();
      } catch (err) {
        const apiErr = err as ApiError;
        setError(isApiError(apiErr) ? apiErr.message : "Failed to check setup");
      } finally {
        setLoading(false);
      }
    },
    [navigate, refreshMe]
  );

  useEffect(() => {
    void checkSetup();
  }, [checkSetup]);

  const value = useMemo<SetupStatusContextValue>(
    () => ({
      setupRequired,
      loading,
      error,
      reload: () => checkSetup(true),
      setSetupRequired,
    }),
    [setupRequired, loading, error, checkSetup, setSetupRequired]
  );

  return (
    <SetupStatusContext.Provider value={value}>
      {children}
    </SetupStatusContext.Provider>
  );
}
