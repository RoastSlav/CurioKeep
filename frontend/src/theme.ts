import { createTheme } from "@mui/material/styles";

declare module "@mui/material/styles" {
    interface Palette {
        accent: Palette["primary"];
    }

    interface PaletteOptions {
        accent?: PaletteOptions["primary"];
    }
}

const theme = createTheme({
    palette: {
        mode: "light",
        primary: {
            main: "#7A37A7",
        },
        secondary: {
            main: "#8AA2A9",
        },
        accent: {
            main: "#000F08",
        },
        background: {
            default: "#f7f7fb",
            paper: "#ffffff",
        },
        text: {
            primary: "#000F08",
        },
    },
    shape: {
        borderRadius: 10,
    },
    components: {
        MuiCssBaseline: {
            styleOverrides: {
                body: {
                    backgroundColor: "#f7f7fb",
                },
            },
        },
        MuiCard: {
            defaultProps: { elevation: 0 },
            styleOverrides: {
                root: {
                    border: "1px solid rgba(0,0,0,0.06)",
                },
            },
        },
        MuiButton: {
            styleOverrides: {
                root: {
                    textTransform: "none",
                    fontWeight: 600,
                },
            },
        },
    },
});

export default theme;
