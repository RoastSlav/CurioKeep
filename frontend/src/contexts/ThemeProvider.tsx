import { useEffect, useMemo, useState, type ReactNode } from "react"
import { ThemeContext, type Theme } from "./theme"

const STORAGE_KEY = "theme"

function readStoredTheme(): Theme {
  try {
    return localStorage.getItem(STORAGE_KEY) === "dark" ? "dark" : "light"
  } catch {
    return "light"
  }
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setThemeState] = useState<Theme>(readStoredTheme)

  useEffect(() => {
    document.documentElement.classList.toggle("dark", theme === "dark")
  }, [theme])

  const value = useMemo(
    () => ({
      theme,
      setTheme: (next: Theme) => {
        setThemeState(next)
        try {
          localStorage.setItem(STORAGE_KEY, next)
        } catch {
          // Storage can be blocked (private mode); the theme still applies for this session.
        }
      },
    }),
    [theme]
  )

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
}
