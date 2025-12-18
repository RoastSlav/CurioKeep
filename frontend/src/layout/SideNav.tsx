import { Divider, List, ListItemButton, ListItemIcon, ListItemText, ListSubheader, Stack, Toolbar, Typography } from "@mui/material";
import DashboardIcon from "@mui/icons-material/Dashboard";
import FolderIcon from "@mui/icons-material/Folder";
import ExtensionIcon from "@mui/icons-material/Extension";
import HubIcon from "@mui/icons-material/Hub";
import GroupIcon from "@mui/icons-material/Group";
import MailIcon from "@mui/icons-material/Mail";
import BookIcon from "@mui/icons-material/Book";
import { useLocation, Link as RouterLink } from "react-router-dom";
import type { ReactElement } from "react";

const SHOW_DOCS_LINK = false;

type NavItem = { label: string; to: string; icon: ReactElement };

const mainNav: NavItem[] = [
    { label: "Dashboard", to: "/", icon: <DashboardIcon fontSize="small" /> },
    { label: "Collections", to: "/collections", icon: <FolderIcon fontSize="small" /> },
    { label: "Modules", to: "/modules", icon: <ExtensionIcon fontSize="small" /> },
    { label: "Providers", to: "/providers", icon: <HubIcon fontSize="small" /> },
];

const adminNav: NavItem[] = [
    { label: "Admin Invites", to: "/admin/invites", icon: <MailIcon fontSize="small" /> },
    { label: "Users", to: "/admin/users", icon: <GroupIcon fontSize="small" /> },
];

export default function SideNav({ isAdmin, onNavigate }: { isAdmin?: boolean; onNavigate?: () => void }) {
    const location = useLocation();

    const isActive = (path: string) => {
        if (path === "/") return location.pathname === "/";
        return location.pathname === path || location.pathname.startsWith(`${path}/`);
    };

    const renderNavList = (items: NavItem[]) => (
        <List disablePadding>
            {items.map((item) => (
                <ListItemButton
                    key={item.to}
                    component={RouterLink}
                    to={item.to}
                    selected={isActive(item.to)}
                    onClick={onNavigate}
                >
                    <ListItemIcon sx={{ minWidth: 36 }}>
                        {item.icon}
                    </ListItemIcon>
                    <ListItemText primary={item.label} primaryTypographyProps={{ fontWeight: 600 }} />
                </ListItemButton>
            ))}
        </List>
    );

    return (
        <Stack sx={{ height: "100%" }}>
            <Toolbar>
                <Typography variant="h6" fontWeight={700}>
                    CurioKeep
                </Typography>
            </Toolbar>
            <Divider />
            {renderNavList(mainNav)}
            {isAdmin && (
                <>
                    <Divider sx={{ my: 1 }} />
                    <ListSubheader component="div" inset sx={{ fontWeight: 700 }}>
                        Admin
                    </ListSubheader>
                    {renderNavList(adminNav)}
                </>
            )}
            {SHOW_DOCS_LINK && (
                <>
                    <Divider sx={{ mt: "auto" }} />
                    <List disablePadding>
                        <ListItemButton component="a" href="https://github.com/RoastSlav/CurioKeep" target="_blank" rel="noreferrer">
                            <ListItemIcon sx={{ minWidth: 36 }}>
                                <BookIcon fontSize="small" />
                            </ListItemIcon>
                            <ListItemText primary="Docs" primaryTypographyProps={{ fontWeight: 600 }} />
                        </ListItemButton>
                    </List>
                </>
            )}
        </Stack>
    );
}
