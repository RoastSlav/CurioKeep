"use client"

import {Menu, Moon, Sun} from "lucide-react"
import {useTheme} from "../contexts/ThemeContext"
import UserMenu from "./UserMenu"
import {Button} from "../../components/ui/button"

export default function TopBar({onMenuToggle, drawerWidth}: { onMenuToggle: () => void; drawerWidth: number }) {
    const {theme, setTheme} = useTheme()

    const toggleTheme = () => {
        setTheme(theme === "dark" ? "light" : "dark")
    }

    return (
        <header
            className="fixed top-0 right-0 left-0 md:left-[260px] z-40 h-16 md:h-[72px] bg-card border-b-4 border-border">
            <div className="h-full flex items-center px-4 md:px-6">
                <div className="flex items-center gap-3 flex-1 min-w-0">
                    <Button variant="ghost" size="icon" onClick={onMenuToggle} aria-label="Open navigation"
                            className="md:hidden">
                        <Menu className="w-5 h-5"/>
                    </Button>
                </div>
                <div className="flex items-center gap-2">
                    <Button variant="ghost" size="icon" onClick={toggleTheme} aria-label="Toggle dark mode">
                        {theme === "dark" ? <Sun className="w-5 h-5"/> : <Moon className="w-5 h-5"/>}
                    </Button>
                    <UserMenu/>
                </div>
            </div>
        </header>
    )
}
