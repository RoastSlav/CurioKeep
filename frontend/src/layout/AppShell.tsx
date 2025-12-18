import { Box, CircularProgress, Container, Drawer, Toolbar, useMediaQuery, useTheme } from "@mui/material";
import { Suspense, useEffect, useState, type ReactNode } from "react";
import { useLocation } from "react-router-dom";
import { useAuth } from "../auth/useAuth";
import SideNav from "./SideNav";
import TopBar from "./TopBar";

const DRAWER_WIDTH = 260;

export default function AppShell({ children }: { children: ReactNode }) {
    const theme = useTheme();
    const isDesktop = useMediaQuery(theme.breakpoints.up("md"));
    const { user } = useAuth();
    const location = useLocation();
    const [mobileOpen, setMobileOpen] = useState(false);

    useEffect(() => {
        setMobileOpen(false);
    }, [location.pathname, location.search]);

    useEffect(() => {
        if (!mobileOpen) return undefined;
        const handleKey = (event: KeyboardEvent) => {
            if (event.key === "Escape") {
                setMobileOpen(false);
            }
        };
        window.addEventListener("keydown", handleKey);
        return () => window.removeEventListener("keydown", handleKey);
    }, [mobileOpen]);

    return (
        <Box sx={{ display: "flex", minHeight: "100vh", bgcolor: (t) => t.palette.background.default }}>
            <TopBar onMenuToggle={() => setMobileOpen(true)} drawerWidth={DRAWER_WIDTH} isDesktop={isDesktop} />

            <Box component="nav" sx={{ width: { md: DRAWER_WIDTH }, flexShrink: { md: 0 } }} aria-label="navigation">
                <Drawer
                    variant="temporary"
                    open={mobileOpen}
                    onClose={() => setMobileOpen(false)}
                    ModalProps={{ keepMounted: true }}
                    sx={{
                        display: { xs: "block", md: "none" },
                        "& .MuiDrawer-paper": { boxSizing: "border-box", width: DRAWER_WIDTH },
                    }}
                >
                    <SideNav isAdmin={user?.isAdmin} onNavigate={() => setMobileOpen(false)} />
                </Drawer>
                <Drawer
                    variant="permanent"
                    open
                    sx={{
                        display: { xs: "none", md: "block" },
                        "& .MuiDrawer-paper": { boxSizing: "border-box", width: DRAWER_WIDTH },
                    }}
                >
                    <SideNav isAdmin={user?.isAdmin} />
                </Drawer>
            </Box>

            <Box
                component="main"
                sx={{
                    flexGrow: 1,
                    width: { md: `calc(100% - ${DRAWER_WIDTH}px)` },
                    minHeight: "100vh",
                    minWidth: 0,
                    overflowX: "hidden",
                }}
            >
                <Toolbar />
                <Container maxWidth="lg" sx={{ py: { xs: 2, md: 3 }, px: { xs: 2, md: 3 } }}>
                    <Suspense
                        fallback={
                            <Box sx={{ display: "grid", placeItems: "center", minHeight: 240 }}>
                                <CircularProgress />
                            </Box>
                        }
                    >
                        {children}
                    </Suspense>
                </Container>
            </Box>
        </Box>
    );
}
