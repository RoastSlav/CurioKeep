import { Navigate, useLocation } from "react-router-dom";
import LoadingState from "../components/LoadingState";
import ErrorState from "../components/ErrorState";
import { useAuth } from "./useAuth";
import type { ReactElement } from "react";

export default function RequireAuth({ children }: { children: ReactElement }) {
    const location = useLocation();
    const { user, loading, error, refreshMe } = useAuth();

    if (loading) {
        return <LoadingState message="Checking session..." />;
    }

    if (error) {
        return <ErrorState title="Session check failed" message={error} onRetry={refreshMe} />;
    }

    if (!user) {
        const params = new URLSearchParams();
        params.set("returnTo", location.pathname + location.search);
        return <Navigate to={`/login?${params.toString()}`} replace />;
    }

    return children;
}
