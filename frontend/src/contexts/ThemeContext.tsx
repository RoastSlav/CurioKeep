"use client"

import type React from "react"
import { createContext, useContext, useEffect, useState } from "react"

type Theme = "light" | "dark"

interface ThemeContextType {
  theme: Theme
  setTheme: (theme: Theme) => void
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined)

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const [theme, setThemeState] = useState<Theme>(() => {
    if (typeof window !== "undefined") {
      const storedTheme = localStorage.getItem("theme") as Theme | null
      return storedTheme || "light"
    }
    return "light"
  })

  useEffect(() => {
    // Apply theme on mount
    applyTheme(theme)
    console.log("[v0] Theme initialized:", theme)
  }, [theme])

  const setTheme = (newTheme: Theme) => {
    console.log("[v0] Theme changing from", theme, "to", newTheme)
    setThemeState(newTheme)
    localStorage.setItem("theme", newTheme)
    applyTheme(newTheme)
  }

  const applyTheme = (theme: Theme) => {
    const root = document.documentElement

    if (theme === "dark") {
      root.classList.add("dark")
      console.log("[v0] Dark mode applied")
    } else {
      root.classList.remove("dark")
      console.log("[v0] Light mode applied")
    }
  }

  return <ThemeContext.Provider value={{ theme, setTheme }}>{children}</ThemeContext.Provider>
}

export function useTheme() {
  const context = useContext(ThemeContext)
  if (!context) {
    throw new Error("useTheme must be used within ThemeProvider")
  }
  return context
}
