import { createTheme } from "@mui/material/styles";

const primary = "#7A37A7";
const secondary = "#8AA2A9";
const accent = "#000F08";

const theme = createTheme({
    palette: {
        primary: { main: primary },
        secondary: { main: secondary },
        background: {
            default: "#f7f7fb",
            paper: "#ffffff",
        },
        text: {
            primary: accent,
            secondary: "#2f3640",
        },
    },
    shape: { borderRadius: 12 },
    typography: {
        fontFamily: "'Inter', 'Segoe UI', system-ui, -apple-system, sans-serif",
        h1: { fontWeight: 700 },
        h2: { fontWeight: 700 },
        h3: { fontWeight: 700 },
        h4: { fontWeight: 700 },
        button: { textTransform: "none", fontWeight: 600 },
    },
    components: {
        MuiButton: {
            styleOverrides: {
                root: {
                    borderRadius: 10,
                },
            },
        },
        MuiPaper: {
            styleOverrides: {
                root: {
                    borderRadius: 14,
                },
            },
        },
    },
});

export default theme;
export { primary, secondary, accent };
