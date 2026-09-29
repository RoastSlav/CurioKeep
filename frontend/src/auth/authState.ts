import { createContext } from "react";

export type AuthUser = {
  id: string;
  email: string;
  displayName?: string | null;
  isAdmin?: boolean;
};

export type AuthContextValue = {
  user: AuthUser | null;
  loading: boolean;
  error: string | null;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  refreshMe: (forceRefresh?: boolean) => Promise<void>;
};

export const AuthContext = createContext<AuthContextValue | undefined>(
  undefined
);
