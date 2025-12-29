"use client"

import type React from "react"

import {Navigate, Outlet, useLocation, useRoutes} from "react-router-dom"
import {Stack, Typography} from "@mui/material"
import {lazy, Suspense} from "react"
import AppShell from "../layout/AppShell"
import LoginPage from "../pages/LoginPage"
import SetupPage from "../pages/SetupPage"
import LoadingState from "../components/LoadingState"
import ErrorState from "../components/ErrorState"
import RequireAuth from "../auth/RequireAuth"
import RequireSetupComplete from "../auth/RequireSetupComplete"
import {useAuth} from "../auth/useAuth"
import {useSetupStatus} from "../components/AppGate"

const DashboardPage = lazy(() => import("../pages/DashboardPage"))
const CollectionsPage = lazy(() => import("../features/collections/pages/CollectionsPage"))
const CollectionDetailPage = lazy(() => import("../features/collections/pages/CollectionDetailPage"))
const AcceptCollectionInvitePage = lazy(() => import("../features/collections/pages/AcceptCollectionInvitePage"))
const ItemDetailPage = lazy(() => import("../features/items/pages/ItemDetailPage"))
const ModulesPage = lazy(() => import("../features/modules/pages/ModulesPage"))
const ProvidersPage = lazy(() => import("../features/providers/pages/ProvidersPage"))
const UsersPage = lazy(() => import("../features/admin/pages/UsersPage"))
const AcceptInvitePage = lazy(() => import("../features/admin/pages/AcceptInvitePage"))

function Suspended({ children }: { children: React.ReactNode }) {
    return <Suspense fallback={<LoadingState message="Loading..."/>}>{children}</Suspense>
}

function ProtectedLayout() {
    return (
        <AppShell>
            <RequireSetupComplete>
                <RequireAuth>
                    <Outlet/>
                </RequireAuth>
            </RequireSetupComplete>
        </AppShell>
    )
}

function LoginRoute() {
    const {setupRequired, loading: setupLoading, error: setupError, reload: reloadSetup} = useSetupStatus()
    const {user, loading: authLoading} = useAuth()

    if (setupLoading) return <LoadingState message="Checking setup..."/>
    if (setupError) return <ErrorState title="Setup check failed" message={setupError} onRetry={reloadSetup}/>
    if (setupRequired) return <Navigate to="/setup" replace/>

    if (authLoading) return <LoadingState message="Checking session..."/>
    if (user) return <Navigate to="/" replace/>

    return <LoginPage/>
}

function SetupRoute() {
    const {setupRequired, loading, error, reload} = useSetupStatus()
    const {user} = useAuth()

    if (loading) return <LoadingState message="Checking setup..."/>
    if (error) return <ErrorState title="Setup check failed" message={error} onRetry={reload}/>
    if (!setupRequired) return <Navigate to={user ? "/" : "/login"} replace/>

    return <SetupPage/>
}

function NotFoundPage() {
    return (
        <Stack spacing={1}>
            <Typography variant="h5" fontWeight={700}>
                Page not found
            </Typography>
            <Typography color="text.secondary">Check the URL or go back to the dashboard.</Typography>
        </Stack>
    )
}

function StubPage({ title, note }: { title: string; note?: string }) {
    return (
        <Stack spacing={1.5}>
            <Typography variant="h5" fontWeight={700}>
                {title}
            </Typography>
            {note && <Typography color="text.secondary">{note}</Typography>}
        </Stack>
    )
}

export default function AppRoutes() {
    const location = useLocation()

    const element = useRoutes([
        {path: "/login", element: <LoginRoute/>},
        {path: "/setup", element: <SetupRoute/>},
        {
            path: "/invites/accept/:token",
            element: (
                <Suspended>
                    <AcceptInvitePage/>
                </Suspended>
            ),
        },
        {
            path: "/",
            element: <ProtectedLayout/>,
            children: [
                {
                    index: true,
                    element: (
                        <Suspended>
                            <DashboardPage/>
                        </Suspended>
                    ),
                },
                {
                    path: "collections",
                    element: (
                        <Suspended>
                            <CollectionsPage/>
                        </Suspended>
                    ),
                },
                {
                    path: "collections/:id",
                    element: (
                        <Suspended>
                            <CollectionDetailPage/>
                        </Suspended>
                    ),
                },
                {
                    path: "collections/:id/items/:itemId",
                    element: (
                        <Suspended>
                            <ItemDetailPage/>
                        </Suspended>
                    ),
                },
                {
                    path: "invites/collection/:token",
                    element: (
                        <Suspended>
                            <AcceptCollectionInvitePage/>
                        </Suspended>
                    ),
                },
                {
                    path: "modules",
                    element: (
                        <Suspended>
                            <ModulesPage/>
                        </Suspended>
                    ),
                },
                {
                    path: "providers",
                    element: (
                        <Suspended>
                            <ProvidersPage/>
                        </Suspended>
                    ),
                },
                {path: "profile", element: <StubPage title="Profile" note="Profile editing coming soon"/>},
                {
                    path: "admin/invites",
                    element: (
                        <Suspended>
                            <UsersPage/>
                        </Suspended>
                    ),
                },
                {
                    path: "admin/users",
                    element: (
                        <Suspended>
                            <UsersPage/>
                        </Suspended>
                    ),
                },
                {path: "*", element: <NotFoundPage/>},
            ],
        },
        {path: "*", element: <Navigate to="/" state={{from: location}} replace/>},
    ])

    return element
}
