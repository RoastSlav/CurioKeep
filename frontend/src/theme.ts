import {createTheme} from "@mui/material/styles"

declare module "@mui/material/styles" {
    interface Palette {
        accent: Palette["primary"]
    }

    interface PaletteOptions {
        accent?: PaletteOptions["primary"]
    }
}

const theme = createTheme({
    palette: {
        mode: "light",
        primary: {
            main: "#2D3748",
            light: "#4A5568",
            dark: "#1A202C",
        },
        secondary: {
            main: "#667EEA",
            light: "#7F9CF5",
            dark: "#5A67D8",
        },
        accent: {
            main: "#48BB78",
            light: "#68D391",
            dark: "#38A169",
        },
        background: {
            default: "#F7FAFC",
            paper: "#FFFFFF",
        },
        text: {
            primary: "#2D3748",
            secondary: "#718096",
        },
        divider: "#E2E8F0",
        error: {
            main: "#F56565",
            light: "#FC8181",
            dark: "#E53E3E",
        },
        warning: {
            main: "#ED8936",
            light: "#F6AD55",
            dark: "#DD6B20",
        },
        success: {
            main: "#48BB78",
            light: "#68D391",
            dark: "#38A169",
        },
    },
    shape: {
        borderRadius: 12,
    },
    typography: {
        fontFamily: '"Inter", "Segoe UI", "Roboto", "Helvetica", "Arial", sans-serif',
        h4: {
            fontWeight: 700,
            letterSpacing: "-0.02em",
        },
        h5: {
            fontWeight: 700,
            letterSpacing: "-0.01em",
        },
        h6: {
            fontWeight: 600,
            letterSpacing: "-0.01em",
        },
        body1: {
            lineHeight: 1.6,
        },
        body2: {
            lineHeight: 1.5,
        },
        button: {
            fontWeight: 600,
            letterSpacing: "0.01em",
        },
    },
    components: {
        MuiCssBaseline: {
            styleOverrides: {
                body: {
                    backgroundColor: "#F7FAFC",
                },
            },
        },
        MuiCard: {
            defaultProps: {elevation: 0},
            styleOverrides: {
                root: {
                    border: "1px solid #E2E8F0",
                    transition: "all 0.2s ease-in-out",
                    "&:hover": {
                        boxShadow: "0 4px 12px rgba(0, 0, 0, 0.08)",
                        transform: "translateY(-2px)",
                    },
                },
            },
        },
        MuiButton: {
            styleOverrides: {
                root: {
                    textTransform: "none",
                    fontWeight: 600,
                    padding: "10px 20px",
                    boxShadow: "none",
                    "&:hover": {
                        boxShadow: "0 2px 8px rgba(0, 0, 0, 0.1)",
                    },
                },
                contained: {
                    "&:hover": {
                        boxShadow: "0 4px 12px rgba(0, 0, 0, 0.15)",
                    },
                },
                sizeSmall: {
                    padding: "6px 14px",
                },
            },
        },
        MuiChip: {
            styleOverrides: {
                root: {
                    fontWeight: 600,
                    fontSize: "0.75rem",
                },
            },
        },
        MuiTextField: {
            styleOverrides: {
                root: {
                    "& .MuiOutlinedInput-root": {
                        backgroundColor: "#FFFFFF",
                        transition: "all 0.2s ease-in-out",
                        "&:hover": {
                            backgroundColor: "#F7FAFC",
                        },
                        "&.Mui-focused": {
                            backgroundColor: "#FFFFFF",
                        },
                    },
                },
            },
        },
        MuiPaper: {
            styleOverrides: {
                root: {
                    backgroundImage: "none",
                },
            },
    },
    },
})

export default theme
