import { createTheme } from "@mui/material/styles";

const theme = createTheme({
    palette: {
        primary: {
            main: "#2f6fed",
        },
        secondary: {
            main: "#00a38c",
        },
        background: {
            default: "#f7f9fc",
        },
    },
    shape: {
        borderRadius: 10,
    },
    components: {
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
