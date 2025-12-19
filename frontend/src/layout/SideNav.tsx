"use client";

import {
  LayoutDashboard,
  Folder,
  PuzzleIcon,
  Network,
  Mail,
  Users,
  BookOpen,
} from "lucide-react";
import { useLocation, Link as RouterLink } from "react-router-dom";
import type { ReactElement } from "react";
import { cn } from "../../lib/utils";

const SHOW_DOCS_LINK = false;

type NavItem = { label: string; to: string; icon: ReactElement };

const mainNav: NavItem[] = [
  {
    label: "Dashboard",
    to: "/",
    icon: <LayoutDashboard className="w-4 h-4" />,
  },
  {
    label: "Collections",
    to: "/collections",
    icon: <Folder className="w-4 h-4" />,
  },
  {
    label: "Modules",
    to: "/modules",
    icon: <PuzzleIcon className="w-4 h-4" />,
  },
  {
    label: "Providers",
    to: "/providers",
    icon: <Network className="w-4 h-4" />,
  },
];

const adminNav: NavItem[] = [
  { label: "Users", to: "/admin/users", icon: <Users className="w-4 h-4" /> },
];

export default function SideNav({
  isAdmin,
  onNavigate,
}: {
  isAdmin?: boolean;
  onNavigate?: () => void;
}) {
  const location = useLocation();

  const isActive = (path: string) => {
    if (path === "/") return location.pathname === "/";
    return (
      location.pathname === path || location.pathname.startsWith(`${path}/`)
    );
  };

  const renderNavList = (items: NavItem[]) => (
    <nav className="space-y-2 px-3">
      {items.map((item) => {
        const active = isActive(item.to);
        return (
          <RouterLink
            key={item.to}
            to={item.to}
            onClick={onNavigate}
            className={cn(
              "flex items-center gap-3 px-4 py-3 text-sm font-bold uppercase tracking-wide transition-all border-2 border-border",
              active
                ? "bg-secondary text-secondary-foreground brutal-shadow-sm"
                : "bg-card text-card-foreground hover:bg-muted brutal-shadow-hover"
            )}
          >
            <span>{item.icon}</span>
            <span>{item.label}</span>
          </RouterLink>
        );
      })}
    </nav>
  );

  return (
    <div className="h-full bg-sidebar text-sidebar-foreground flex flex-col">
      <div className="h-16 md:h-[72px] flex items-center px-6 border-b-4 border-border">
        <h1 className="text-xl font-bold tracking-tight uppercase">
          CurioKeep
        </h1>
      </div>

      <div className="py-4">{renderNavList(mainNav)}</div>

      {isAdmin && (
        <>
          <div className="border-t-2 border-border my-2" />
          <div className="px-6 py-2">
            <h2 className="text-xs font-bold uppercase tracking-wider text-muted-foreground">
              Admin
            </h2>
          </div>
          {renderNavList(adminNav)}
        </>
      )}

      {SHOW_DOCS_LINK && (
        <>
          <div className="border-t-2 border-border mt-auto" />
          <nav className="px-3 py-3">
            <a
              href="https://github.com/RoastSlav/CurioKeep"
              target="_blank"
              rel="noreferrer"
              className="flex items-center gap-3 px-4 py-3 text-sm font-bold uppercase tracking-wide bg-card text-card-foreground hover:bg-muted transition-all border-2 border-border brutal-shadow-hover"
            >
              <BookOpen className="w-4 h-4" />
              <span>Docs</span>
            </a>
          </nav>
        </>
      )}
    </div>
  );
}
