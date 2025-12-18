import { AppBar, IconButton, Stack, Toolbar, Typography } from "@mui/material";
import MenuIcon from "@mui/icons-material/Menu";
import { Link as RouterLink } from "react-router-dom";
import UserMenu from "./UserMenu";

export default function TopBar({ onMenuToggle, drawerWidth, isDesktop }: { onMenuToggle: () => void; drawerWidth: number; isDesktop: boolean }) {
    return (
        <AppBar
            position="fixed"
            color="primary"
            elevation={1}
            sx={{
                zIndex: (theme) => theme.zIndex.drawer + 1,
                width: isDesktop ? `calc(100% - ${drawerWidth}px)` : "100%",
                ml: isDesktop ? `${drawerWidth}px` : 0,
            }}
        >
            <Toolbar>
                <Stack direction="row" spacing={1.5} alignItems="center" sx={{ flex: 1, minWidth: 0 }}>
                    {!isDesktop && (
                        <IconButton
                            color="inherit"
                            edge="start"
                            onClick={onMenuToggle}
                            aria-label="Open navigation"
                            sx={{ display: { xs: "inline-flex", md: "none" } }}
                        >
                            <MenuIcon />
                        </IconButton>
                    )}
                    <Typography
                        variant="h6"
                        fontWeight={700}
                        noWrap
                        component={RouterLink}
                        to="/"
                        sx={{ color: "inherit", textDecoration: "none", minWidth: 0, maxWidth: { xs: 200, sm: 260, md: "unset" } }}
                    >
                        CurioKeep
                    </Typography>
                </Stack>
                <Stack direction="row" spacing={1} alignItems="center">
                    <UserMenu />
                </Stack>
            </Toolbar>
        </AppBar>
    );
}
