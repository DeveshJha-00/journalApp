"use client";

import Image from "next/image";
import Link from "next/link";
import { useAuth } from "@/lib/auth";
import { Button } from "@/components/ui/button";
import { LogOut, LayoutDashboard, BookOpen, FolderOpen, PenLine, Sun, Moon } from "lucide-react";
import { useTheme } from "next-themes";
import { useEffect, useState } from "react";

const Header = () => {
  const { isAuthenticated, isLoading, logout } = useAuth();
  const { theme, setTheme } = useTheme();
  const [mounted, setMounted] = useState(false);

  useEffect(() => setMounted(true), []);

  return (
    <header className="sticky top-0 z-50 backdrop-blur-md border-b border-orange-100 dark:border-gray-800">
      <div className="container mx-auto px-4 py-1 flex items-center justify-between">
        <Link href={isAuthenticated ? "/dashboard" : "/"} className="flex items-center gap-2">
          <Image
            src="/logo_2.svg"
            alt="Journal App"
            width={300}
            height={100}
            className="h-16 w-auto object-contain bg-white/80 dark:bg-gray-900/80 backdrop-blur-sm rounded-xl"
          />
        </Link>

        {!isLoading && (
          <nav className="flex items-center gap-1">
            {/* Dark mode toggle */}
            {mounted && (
              <Button
                variant="ghost"
                size="icon"
                onClick={() => setTheme(theme === "dark" ? "light" : "dark")}
                className="text-gray-700 dark:text-gray-300 hover:text-orange-600 dark:hover:text-orange-400"
                title={theme === "dark" ? "Switch to light mode" : "Switch to dark mode"}
              >
                {theme === "dark" ? <Sun className="h-5 w-5" /> : <Moon className="h-5 w-5" />}
              </Button>
            )}

            {isAuthenticated ? (
              <>
                <Link href="/dashboard">
                  <Button variant="ghost" className="text-gray-700 dark:text-gray-300 hover:text-orange-600 dark:hover:text-orange-400">
                    <LayoutDashboard className="h-4 w-4 mr-1.5" /> Dashboard
                  </Button>
                </Link>
                <Link href="/journal">
                  <Button variant="ghost"className="text-gray-700 dark:text-gray-300 hover:text-orange-600 dark:hover:text-orange-400">
                    <BookOpen className="h-4 w-4 mr-1.5" /> Entries
                  </Button>
                </Link>
                <Link href="/collections">
                  <Button variant="ghost" className="text-gray-700 dark:text-gray-300 hover:text-orange-600 dark:hover:text-orange-400">
                    <FolderOpen className="h-4 w-4 mr-1.5" /> Collections
                  </Button>
                </Link>
                <Link href="/journal/new">
                  <Button className="bg-orange-600 hover:bg-orange-700 text-white ml-2">
                    <PenLine className="h-4 w-4 mr-1.5" /> Write New
                  </Button>
                </Link>
                <Button
                  variant="ghost"
                  onClick={logout}
                  className="text-gray-500 dark:text-gray-400 hover:text-red-600 ml-1"
                >
                  <LogOut className="h-4 w-4" />
                </Button>
              </>
            ) : (
              <Link href="/auth">
                <Button size="sm" className="bg-orange-600 hover:bg-orange-700 text-white">
                  Get Started
                </Button>
              </Link>
            )}
          </nav>
        )}
      </div>
    </header>
  );
};

export default Header;