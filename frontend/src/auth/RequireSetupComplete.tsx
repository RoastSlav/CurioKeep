import { Navigate } from "react-router-dom";
import LoadingState from "../components/LoadingState";
import ErrorState from "../components/ErrorState";
import { useSetupStatus } from "../components/AppGate";
import type { ReactElement } from "react";

export default function RequireSetupComplete({ children }: { children: ReactElement }) {
    const { setupRequired, loading, error, reload } = useSetupStatus();

    if (loading) {
        return <LoadingState message="Checking setup..." />;
    }

    if (error) {
        return <ErrorState title="Setup check failed" message={error} onRetry={reload} />;
    }

    if (setupRequired) {
        return <Navigate to="/setup" replace />;
    }

    return children;
}
