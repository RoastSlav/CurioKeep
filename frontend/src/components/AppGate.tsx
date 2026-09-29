import type {ReactNode} from "react";
import {useCallback, useEffect, useMemo, useState,} from "react";
import {ApiError, isApiError} from "../api/errors";
import {getSetupStatus} from "../api/setup";
import {useAuth} from "../auth/useAuth";
import {SetupStatusContext, type SetupStatusContextValue} from "./setupStatusContext";

export default function AppGate({ children }: { children: ReactNode }) {
  const { refreshMe } = useAuth();

  const [setupRequired, setSetupRequired] = useState<boolean>(false);
  const [tokenRequired, setTokenRequired] = useState<boolean>(false);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const checkSetup = useCallback(
    async (forceRefresh = false) => {
      setLoading(true);
      setError(null);
      try {
        const status = await getSetupStatus({ forceRefresh });
        setSetupRequired(status.setupRequired);
        setTokenRequired(status.tokenRequired);

          if (!status.setupRequired) {
              await refreshMe();
          }
      } catch (err) {
        const apiErr = err as ApiError;
        setError(isApiError(apiErr) ? apiErr.message : "Failed to check setup");
      } finally {
        setLoading(false);
      }
    },
      [refreshMe]
  );

  useEffect(() => {
    void checkSetup();
  }, [checkSetup]);

  const value = useMemo<SetupStatusContextValue>(
    () => ({
      setupRequired,
      tokenRequired,
      loading,
      error,
      reload: () => checkSetup(true),
      setSetupRequired,
    }),
    [setupRequired, tokenRequired, loading, error, checkSetup, setSetupRequired]
  );

  return (
    <SetupStatusContext.Provider value={value}>
      {children}
    </SetupStatusContext.Provider>
  );
}
