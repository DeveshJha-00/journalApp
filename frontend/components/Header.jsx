"use client";

import Image from "next/image";
import Link from "next/link";
import { useAuth } from "@/lib/auth";
import { Button } from "@/components/ui/button";
import { LogOut, LayoutDashboard, BookOpen, FolderOpen, PenLine } from "lucide-react";

const Header = () => {
  const { isAuthenticated, isLoading, logout } = useAuth();

  return (
    <header className="sticky top-0 z-50 bg-white/80 backdrop-blur-md border-b border-orange-100">
      <div className="container mx-auto px-4 py-3 flex items-center justify-between">
        <Link href={isAuthenticated ? "/dashboard" : "/"} className="flex items-center gap-2">
          <Image
            src="/logo.png"
            alt="Journal App"
            width={200}
            height={60}
            className="h-9 w-auto object-contain"
          />
        </Link>

        {!isLoading && (
          <nav className="flex items-center gap-1">
            {isAuthenticated ? (
              <>
                <Link href="/dashboard">
                  <Button variant="ghost" size="sm" className="text-gray-700 hover:text-orange-600">
                    <LayoutDashboard className="h-4 w-4 mr-1.5" /> Dashboard
                  </Button>
                </Link>
                <Link href="/journal">
                  <Button variant="ghost" size="sm" className="text-gray-700 hover:text-orange-600">
                    <BookOpen className="h-4 w-4 mr-1.5" /> Entries
                  </Button>
                </Link>
                <Link href="/collections">
                  <Button variant="ghost" size="sm" className="text-gray-700 hover:text-orange-600">
                    <FolderOpen className="h-4 w-4 mr-1.5" /> Collections
                  </Button>
                </Link>
                <Link href="/journal/new">
                  <Button size="sm" className="bg-orange-600 hover:bg-orange-700 text-white ml-2">
                    <PenLine className="h-4 w-4 mr-1.5" /> Write New
                  </Button>
                </Link>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={logout}
                  className="text-gray-500 hover:text-red-600 ml-1"
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