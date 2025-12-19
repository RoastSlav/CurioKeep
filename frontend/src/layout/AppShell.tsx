"use client"

import {Suspense, useEffect, useState, type ReactNode} from "react"
import {useLocation} from "react-router-dom"
import {useAuth} from "../auth/useAuth"
import SideNav from "./SideNav"
import TopBar from "./TopBar"
import {Loader2} from "lucide-react"

const DRAWER_WIDTH = 260

export default function AppShell({ children }: { children: ReactNode }) {
    const {user} = useAuth()
    const location = useLocation()
    const [mobileOpen, setMobileOpen] = useState(false)

    useEffect(() => {
        setMobileOpen(false)
    }, [location.pathname, location.search])

    useEffect(() => {
        if (!mobileOpen) return undefined
        const handleKey = (event: KeyboardEvent) => {
            if (event.key === "Escape") {
                setMobileOpen(false)
            }
        }
        window.addEventListener("keydown", handleKey)
        return () => window.removeEventListener("keydown", handleKey)
    }, [mobileOpen])

    return (
        <div className="flex min-h-screen bg-background text-foreground">
            <TopBar onMenuToggle={() => setMobileOpen(true)} drawerWidth={DRAWER_WIDTH}/>

            {/* Mobile sidebar overlay */}
            {mobileOpen &&
                <div className="fixed inset-0 bg-black/50 z-40 md:hidden" onClick={() => setMobileOpen(false)}/>}

            <aside
                className="fixed inset-y-0 left-0 z-50 w-[260px] transform transition-transform duration-300 md:hidden border-r-4 border-border bg-sidebar"
                style={{transform: mobileOpen ? "translateX(0)" : "translateX(-100%)"}}
            >
                <SideNav isAdmin={user?.isAdmin} onNavigate={() => setMobileOpen(false)}/>
            </aside>

            <aside
                className="hidden md:block w-[260px] fixed inset-y-0 left-0 z-30 border-r-4 border-border bg-sidebar">
                <SideNav isAdmin={user?.isAdmin}/>
            </aside>

            {/* Main content */}
            <main className="flex-1 md:ml-[260px] min-h-screen overflow-x-hidden bg-background">
                <div className="h-16 md:h-[72px]"/>
                {/* Spacer for fixed header */}
                <div className="container mx-auto py-6 md:py-8 px-4 md:px-8 max-w-[1536px]">
                    <Suspense
                        fallback={
                            <div className="flex items-center justify-center min-h-[240px]">
                                <Loader2 className="w-8 h-8 animate-spin text-secondary"/>
                            </div>
                        }
                    >
                        {children}
                    </Suspense>
                </div>
            </main>
        </div>
    )
}
