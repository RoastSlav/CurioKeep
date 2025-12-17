import { AppBar, Avatar, Box, Button, Dialog, DialogActions, DialogContent, DialogTitle, Divider, Drawer, IconButton, List, ListItemButton, ListItemText, ListItemIcon, Toolbar, Typography, alpha, useMediaQuery, useTheme } from "@mui/material";
import { useEffect, useMemo } from "react";
import MenuIcon from "@mui/icons-material/Menu";
import DashboardIcon from "@mui/icons-material/Dashboard";
import ViewListIcon from "@mui/icons-material/ViewList";
import ExtensionIcon from "@mui/icons-material/Extension";
import HubIcon from "@mui/icons-material/Hub";
import LogoutIcon from "@mui/icons-material/Logout";
import PersonIcon from "@mui/icons-material/Person";
import GroupIcon from "@mui/icons-material/Group";
import PersonAddAltIcon from "@mui/icons-material/PersonAddAlt";
import type { PropsWithChildren } from "react";
import { useState } from "react";
import { Link as RouterLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import { logout } from "../api";
import type { User } from "../types";

const drawerWidth = 240;

type LayoutProps = PropsWithChildren<{ user: User; onLogout?: () => void }>;

export default function Layout({ user, onLogout }: LayoutProps) {
    const [open, setOpen] = useState(true);
    const [logoutOpen, setLogoutOpen] = useState(false);
    const location = useLocation();
    const navigate = useNavigate();
    const theme = useTheme();
    const isMobile = useMediaQuery(theme.breakpoints.down("md"));

    // Keep drawer closed by default on mobile, open on desktop
    // Sync when viewport changes
    useEffect(() => setOpen(!isMobile), [isMobile]);

    const navItems = useMemo(() => {
        const items = [
            { label: "Collections", to: "/", icon: <ViewListIcon fontSize="small" /> },
            { label: "Dashboard", to: "/dashboard", icon: <DashboardIcon fontSize="small" /> },
            { label: "Modules", to: "/modules", icon: <ExtensionIcon fontSize="small" /> },
            { label: "Providers", to: "/providers", icon: <HubIcon fontSize="small" /> },
            { label: "Profile", to: "/profile", icon: <PersonIcon fontSize="small" /> },
        ];

        if (user.admin) {
            items.push({ label: "Users", to: "/admin/users", icon: <GroupIcon fontSize="small" /> });
            items.push({ label: "Invites", to: "/admin/invites", icon: <PersonAddAltIcon fontSize="small" /> });
        }

        return items;
    }, [user.admin]);

    const activeNav = useMemo(
        () => navItems.find((item) => location.pathname === item.to || (item.to !== "/" && location.pathname.startsWith(item.to))),
        [location.pathname, navItems]
    );

    return (
        <Box sx={{ display: "flex", minHeight: "100vh", backgroundColor: (t) => t.palette.background.default }}>
            <AppBar
                position="fixed"
                elevation={0}
                sx={{
                    ml: !isMobile && open ? `${drawerWidth}px` : 0,
                    width: !isMobile && open ? `calc(100% - ${drawerWidth}px)` : "100%",
                    backgroundColor: (t) => t.palette.common.black,
                    zIndex: (t) => t.zIndex.drawer + 1,
                    borderRadius: 0,
                    borderTopLeftRadius: 0,
                    borderTopRightRadius: 0,
                }}
            >
                <Toolbar>
                    <IconButton color="inherit" edge="start" onClick={() => setOpen((v) => !v)} sx={{ mr: 2 }}>
                        <MenuIcon />
                    </IconButton>
                    <Typography variant="h6" sx={{ flexGrow: 1 }}>
                        CurioKeep
                    </Typography>
                    <Avatar sx={{ width: 32, height: 32, bgcolor: (t) => t.palette.background.paper, color: (t) => t.palette.primary.main, mr: 1 }}>
                        {user.displayName?.[0]?.toUpperCase() || user.email[0].toUpperCase()}
                    </Avatar>
                    <Typography variant="body2">{user.displayName || user.email}</Typography>
                </Toolbar>
            </AppBar>

            <Drawer
                variant={isMobile ? "temporary" : "persistent"}
                open={open}
                onClose={() => setOpen(false)}
                ModalProps={{ keepMounted: true }}
                sx={{
                    width: drawerWidth,
                    flexShrink: 0,
                    [`& .MuiDrawer-paper`]: {
                        width: drawerWidth,
                        boxSizing: "border-box",
                        backgroundColor: (t) => t.palette.background.paper,
                        borderRight: (t) => `1px solid ${alpha(t.palette.secondary.main, 0.3)}`,
                    },
                }}
            >
                <Toolbar>
                    <Typography variant="h6" color="primary">
                        {activeNav?.label || "CurioKeep"}
                    </Typography>
                </Toolbar>
                <Divider />
                <List>
                    {navItems.map((item) => {
                        const active = location.pathname === item.to || (item.to !== "/" && location.pathname.startsWith(item.to));
                        return (
                            <ListItemButton
                                key={item.to}
                                component={RouterLink}
                                to={item.to}
                                selected={active}
                            >
                            <ListItemIcon sx={{ minWidth: 32 }}>{item.icon}</ListItemIcon>
                            <ListItemText primary={item.label} sx={{ ml: 1 }} />
                            </ListItemButton>
                        );
                    })}
                </List>
                <Box sx={{ flexGrow: 1 }} />
                <Divider />
                <List>
                    <ListItemButton onClick={() => setLogoutOpen(true)}>
                        <LogoutIcon fontSize="small" />
                        <ListItemText primary="Logout" sx={{ ml: 1 }} />
                    </ListItemButton>
                </List>
            </Drawer>

            <Box component="main" sx={{ flexGrow: 1, p: 3, ml: !isMobile && open ? `${drawerWidth}px` : 0 }}>
                <Toolbar />
                <Outlet context={{ user }} />
            </Box>

            <Dialog open={logoutOpen} onClose={() => setLogoutOpen(false)}>
                <DialogTitle>Sign out?</DialogTitle>
                <DialogContent>
                    <Typography variant="body2">This will end your session.</Typography>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setLogoutOpen(false)}>Cancel</Button>
                    <Button
                        color="primary"
                        onClick={() => logout().finally(() => {
                            setLogoutOpen(false);
                            onLogout?.();
                            navigate("/login", { replace: true });
                        })}
                    >
                        Logout
                    </Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
}
