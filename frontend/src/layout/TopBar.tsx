import { AppBar, IconButton, Stack, Toolbar, Typography } from "@mui/material";
import MenuIcon from "@mui/icons-material/Menu";
import UserMenu from "./UserMenu";

export default function TopBar({ onMenuToggle, drawerWidth, isDesktop }: { onMenuToggle: () => void; drawerWidth: number; isDesktop: boolean }) {
    return (
        <AppBar
            position="fixed"
            color="default"
            elevation={1}
            sx={{
                zIndex: (theme) => theme.zIndex.drawer + 1,
                width: isDesktop ? `calc(100% - ${drawerWidth}px)` : "100%",
                ml: isDesktop ? `${drawerWidth}px` : 0,
            }}
        >
            <Toolbar>
                <Stack direction="row" spacing={1.5} alignItems="center" sx={{ flex: 1 }}>
                    {!isDesktop && (
                        <IconButton color="inherit" edge="start" onClick={onMenuToggle} aria-label="Open navigation">
                            <MenuIcon />
                        </IconButton>
                    )}
                    <Typography variant="h6" fontWeight={700} noWrap>
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
