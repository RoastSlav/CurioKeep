import { Stack, Typography } from "@mui/material";
import { Navigate, Outlet, useRoutes } from "react-router-dom";
import AppShell from "../layout/AppShell";
import LoginPage from "../pages/LoginPage";
import SetupPage from "../pages/SetupPage";
import RequireAuth from "../auth/RequireAuth";
import RequireSetupComplete from "../auth/RequireSetupComplete";
import { useAuth } from "../auth/useAuth";
import { useSetupStatus } from "../components/AppGate";
import LoadingState from "../components/LoadingState";
import ErrorState from "../components/ErrorState";

function ProtectedLayout() {
    return (
        <RequireSetupComplete>
            <RequireAuth>
                <AppShell>
                    <Outlet />
                </AppShell>
            </RequireAuth>
        </RequireSetupComplete>
    );
}

function LoginRoute() {
    const { setupRequired, loading: setupLoading, error: setupError, reload: reloadSetup } = useSetupStatus();
    const { user, loading: authLoading } = useAuth();

    if (setupLoading) return <LoadingState message="Checking setup..." />;
    if (setupError) return <ErrorState title="Setup check failed" message={setupError} onRetry={reloadSetup} />;
    if (setupRequired) return <Navigate to="/setup" replace />;

    if (authLoading) return <LoadingState message="Checking session..." />;
    if (user) return <Navigate to="/" replace />;

    return <LoginPage />;
}

function SetupRoute() {
    const { setupRequired, loading, error, reload } = useSetupStatus();
    const { user } = useAuth();

    if (loading) return <LoadingState message="Checking setup..." />;
    if (error) return <ErrorState title="Setup check failed" message={error} onRetry={reload} />;
    if (!setupRequired) return <Navigate to={user ? "/" : "/login"} replace />;

    return <SetupPage />;
}

function HomePage() {
    return (
        <Stack spacing={2}>
            <Typography variant="h4" fontWeight={700}>Welcome to CurioKeep</Typography>
            <Typography variant="body1" color="text.secondary">
                Choose a collection to get started. Navigation links will appear here once features are added.
            </Typography>
        </Stack>
    );
}

function NotFoundPage() {
    return (
        <Stack spacing={1}>
            <Typography variant="h5" fontWeight={700}>Page not found</Typography>
            <Typography color="text.secondary">Check the URL or go back to the dashboard.</Typography>
        </Stack>
    );
}

export default function AppRouter() {
    const element = useRoutes([
        { path: "/login", element: <LoginRoute /> },
        { path: "/setup", element: <SetupRoute /> },
        {
            path: "/",
            element: <ProtectedLayout />,
            children: [
                { index: true, element: <HomePage /> },
                { path: "*", element: <NotFoundPage /> },
            ],
        },
        { path: "*", element: <Navigate to="/" replace /> },
    ]);

    return element;
}
