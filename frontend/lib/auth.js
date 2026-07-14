"use client";

import { createContext, useContext, useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import { authAPI } from "@/lib/api";

const AuthContext = createContext(null);
const COOKIE_AUTH_TOKEN = "__cookie_auth__";

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => getInitialToken());
  const [isLoading] = useState(false);
  const router = useRouter();
  const queryClient = useQueryClient();

  const login = useCallback((jwt) => {
    localStorage.setItem("token", jwt);
    setToken(jwt);
    queryClient.clear();
    router.push("/dashboard");
  }, [router, queryClient]);

  const loginWithCookie = useCallback(() => {
    localStorage.removeItem("token");
    localStorage.setItem("authMode", "cookie");
    setToken(COOKIE_AUTH_TOKEN);
    queryClient.clear();
    router.push("/dashboard");
  }, [router, queryClient]);

  const logout = useCallback(async () => {
    localStorage.removeItem("token");
    localStorage.removeItem("authMode");
    setToken(null);
    queryClient.clear();
    try {
      await authAPI.clearOAuthCookie();
    } catch {
      // Logout is client-side only; JWT revocation is intentionally not implemented.
    }
    router.push("/");
  }, [router, queryClient]);

  const isAuthenticated = !!token;

  return (
    <AuthContext.Provider value={{ token, isAuthenticated, isLoading, login, loginWithCookie, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

function getInitialToken() {
  if (typeof window === "undefined") {
    return null;
  }

  const stored = localStorage.getItem("token");
  if (stored) {
    return stored;
  }

  return localStorage.getItem("authMode") === "cookie" ? COOKIE_AUTH_TOKEN : null;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
