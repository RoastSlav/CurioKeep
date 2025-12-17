import { useEffect, useMemo, useState } from "react";
import { CircularProgress, Container, Typography } from "@mui/material";
import { Navigate, Route, Routes, useLocation, useNavigate } from "react-router-dom";
import { getMe, getSetupStatus } from "./api";
import Layout from "./components/Layout";
import CollectionsPage from "./pages/CollectionsPage";
import DashboardPage from "./pages/DashboardPage";
import AddItemPage from "./pages/AddItemPage";
import LoginPage from "./pages/LoginPage";
import ModulesPage from "./pages/ModulesPage";
import ProvidersPage from "./pages/ProvidersPage";
import SetupPage from "./pages/SetupPage";
import ProfilePage from "./pages/ProfilePage";
import AdminInvitesPage from "./pages/AdminInvitesPage";
import AcceptInvitePage from "./pages/AcceptInvitePage";
import UsersPage from "./pages/UsersPage";
import type { User } from "./types";

type BootstrapState = {
    setupRequired: boolean | null;
    user: User | null;
    loading: boolean;
    error?: string;
};

function App() {
    const [state, setState] = useState<BootstrapState>({ setupRequired: null, user: null, loading: true });
    const location = useLocation();
    const navigate = useNavigate();

    const reload = async () => {
        try {
            setState((s) => ({ ...s, loading: true, error: undefined }));
            const setup = await getSetupStatus();
            let user: User | null = null;
            if (!setup.setupRequired) {
                try {
                    user = await getMe();
                } catch {
                    user = null;
                }
            }
            setState({ setupRequired: setup.setupRequired, user, loading: false });
        } catch (e) {
            setState({ setupRequired: null, user: null, loading: false, error: (e as Error).message });
        }
    };

    useEffect(() => {
        void reload();
    }, []);

    useEffect(() => {
        if (state.loading) return;
        const isInviteAcceptance = location.pathname.startsWith("/accept-invite");

        if (state.setupRequired) {
            if (location.pathname !== "/setup") navigate("/setup", { replace: true });
            return;
        }
        if (!state.user) {
            if (!isInviteAcceptance && location.pathname !== "/login") navigate("/login", { replace: true });
            return;
        }
        // If already authenticated but on the login screen, send to home.
        if (location.pathname === "/login" || isInviteAcceptance) navigate("/", { replace: true });
    }, [state.loading, state.setupRequired, state.user, location.pathname, navigate]);

    const shell = useMemo(() => {
        if (state.loading || state.setupRequired || !state.user) return null;
        return <Layout user={state.user} onLogout={reload} />;
    }, [state.loading, state.setupRequired, state.user]);

    if (state.loading) {
        return (
            <Container maxWidth="sm" sx={{ pt: 10, textAlign: "center" }}>
                <CircularProgress />
                <Typography variant="body2" sx={{ mt: 2 }}>
                    Checking setup and session…
                </Typography>
            </Container>
        );
    }

    if (state.error) {
        return (
            <Container maxWidth="sm" sx={{ pt: 10 }}>
                <Typography variant="h5" color="error" gutterBottom>
                    Startup error
                </Typography>
                <Typography variant="body2">{state.error}</Typography>
            </Container>
        );
    }

    return (
        <Routes>
            <Route
                path="/setup"
                element={<SetupPage onCompleted={reload} />}
            />
            <Route
                path="/login"
                element={<LoginPage onLoginSuccess={reload} />}
            />
            <Route
                path="/accept-invite"
                element={<AcceptInvitePage />}
            />
            <Route element={shell}>
                <Route index element={<DashboardPage />} />
                <Route path="/modules" element={<ModulesPage />} />
                <Route path="/collections" element={<CollectionsPage />} />
                <Route path="/collections/:collectionId/modules/:moduleKey/add" element={<AddItemPage />} />
                <Route path="/providers" element={<ProvidersPage />} />
                <Route path="/profile" element={<ProfilePage />} />
                <Route path="/admin/invites" element={<AdminInvitesPage />} />
                <Route path="/admin/users" element={<UsersPage />} />
            </Route>
            <Route path="*" element={<Navigate to={state.setupRequired ? "/setup" : state.user ? "/" : "/login"} replace />} />
        </Routes>
    );
}

export default App;
