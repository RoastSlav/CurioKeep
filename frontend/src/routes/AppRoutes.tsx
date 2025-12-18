import { Navigate, Outlet, useLocation, useRoutes } from "react-router-dom";
import { Stack, Typography } from "@mui/material";
import AppShell from "../layout/AppShell";
import LoginPage from "../pages/LoginPage";
import SetupPage from "../pages/SetupPage";
import DashboardPage from "../pages/DashboardPage";
import CollectionsPage from "../features/collections/pages/CollectionsPage";
import CollectionDetailPage from "../features/collections/pages/CollectionDetailPage";
import AcceptCollectionInvitePage from "../features/collections/pages/AcceptCollectionInvitePage";
import ItemDetailPage from "../features/items/pages/ItemDetailPage";
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

function NotFoundPage() {
    return (
        <Stack spacing={1}>
            <Typography variant="h5" fontWeight={700}>Page not found</Typography>
            <Typography color="text.secondary">Check the URL or go back to the dashboard.</Typography>
        </Stack>
    );
}

function StubPage({ title, note }: { title: string; note?: string }) {
    return (
        <Stack spacing={1.5}>
            <Typography variant="h5" fontWeight={700}>{title}</Typography>
            {note && (
                <Typography color="text.secondary">{note}</Typography>
            )}
        </Stack>
    );
}

export default function AppRoutes() {
    const location = useLocation();

    const element = useRoutes([
        { path: "/login", element: <LoginRoute /> },
        { path: "/setup", element: <SetupRoute /> },
        {
            path: "/",
            element: <ProtectedLayout />,
            children: [
                { index: true, element: <DashboardPage /> },
                { path: "collections", element: <CollectionsPage /> },
                { path: "collections/:id", element: <CollectionDetailPage /> },
                { path: "collections/:id/items/:itemId", element: <ItemDetailPage /> },
                { path: "invites/collection/:token", element: <AcceptCollectionInvitePage /> },
                { path: "modules", element: <StubPage title="Modules" note="Modules page placeholder" /> },
                { path: "providers", element: <StubPage title="Providers" note="Providers page placeholder" /> },
                { path: "profile", element: <StubPage title="Profile" note="Profile editing coming soon" /> },
                { path: "admin/invites", element: <StubPage title="Admin Invites" note="Admin invites placeholder" /> },
                { path: "admin/users", element: <StubPage title="Admin Users" note="Admin users placeholder" /> },
                { path: "*", element: <NotFoundPage /> },
            ],
        },
        { path: "*", element: <Navigate to="/" state={{ from: location }} replace /> },
    ]);

    return element;
}
