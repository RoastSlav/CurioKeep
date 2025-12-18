import { Avatar, IconButton, ListItemIcon, Menu, MenuItem, Tooltip, Typography } from "@mui/material";
import { Logout, Person } from "@mui/icons-material";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../auth/useAuth";

export default function UserMenu() {
    const { user, logout } = useAuth();
    const navigate = useNavigate();
    const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);

    const open = Boolean(anchorEl);
    const displayName = user?.displayName || user?.email || "User";
    const initial = (user?.displayName || user?.email || "?").charAt(0).toUpperCase();

    const handleOpen = (event: React.MouseEvent<HTMLElement>) => setAnchorEl(event.currentTarget);
    const handleClose = () => setAnchorEl(null);

    const handleProfile = () => {
        handleClose();
        navigate("/profile");
    };

    const handleLogout = async () => {
        handleClose();
        await logout();
        navigate("/login", { replace: true });
    };

    return (
        <>
            <Tooltip title={displayName}>
                <IconButton onClick={handleOpen} size="small" sx={{ ml: 1 }} aria-controls={open ? "user-menu" : undefined} aria-haspopup="true" aria-expanded={open ? "true" : undefined}>
                    <Avatar sx={{ width: 32, height: 32 }}>
                        {initial}
                    </Avatar>
                </IconButton>
            </Tooltip>
            <Menu
                anchorEl={anchorEl}
                id="user-menu"
                open={open}
                onClose={handleClose}
                onClick={handleClose}
                transformOrigin={{ horizontal: "right", vertical: "top" }}
                anchorOrigin={{ horizontal: "right", vertical: "bottom" }}
            >
                <MenuItem onClick={handleProfile}>
                    <ListItemIcon>
                        <Person fontSize="small" />
                    </ListItemIcon>
                    <Typography>Profile</Typography>
                </MenuItem>
                <MenuItem onClick={handleLogout}>
                    <ListItemIcon>
                        <Logout fontSize="small" />
                    </ListItemIcon>
                    <Typography>Logout</Typography>
                </MenuItem>
            </Menu>
        </>
    );
}
