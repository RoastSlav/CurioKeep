"use client"

import {LogOut, User} from "lucide-react"
import {useNavigate} from "react-router-dom"
import {useAuth} from "../auth/useAuth"
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from "../../components/ui/dropdown-menu"
import {Avatar, AvatarFallback} from "../../components/ui/avatar"

export default function UserMenu() {
    const {user, logout} = useAuth()
    const navigate = useNavigate()

    const displayName = user?.displayName || user?.email || "User"
    const initial = (user?.displayName || user?.email || "?").charAt(0).toUpperCase()

    const handleProfile = () => {
        navigate("/profile")
    }

    const handleLogout = async () => {
        await logout()
        navigate("/login", {replace: true})
    }

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <button
                    className="focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-secondary rounded-full">
                    <Avatar className="w-9 h-9 border-2 border-border">
                        <AvatarFallback className="bg-secondary text-secondary-foreground text-sm font-bold">
                            {initial}
                        </AvatarFallback>
                    </Avatar>
                </button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-48">
                <DropdownMenuItem onClick={handleProfile}>
                    <User className="w-4 h-4 mr-2"/>
                    Profile
                </DropdownMenuItem>
                <DropdownMenuItem onClick={handleLogout}>
                    <LogOut className="w-4 h-4 mr-2"/>
                    Logout
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    )
}
