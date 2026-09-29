import { createContext, useContext } from "react";

export type SetupStatusContextValue = {
  setupRequired: boolean;
  tokenRequired: boolean;
  loading: boolean;
  error: string | null;
  reload: () => Promise<void>;
  setSetupRequired: (value: boolean) => void;
};

export const SetupStatusContext = createContext<SetupStatusContextValue | undefined>(
  undefined
);

export function useSetupStatus() {
  const ctx = useContext(SetupStatusContext);
  if (!ctx) throw new Error("useSetupStatus must be used within AppGate");
  return ctx;
}
