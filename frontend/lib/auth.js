"use client";

import { createContext, useContext, useState, useEffect, useCallback } from "react";
import { useRouter } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const router = useRouter();
  const queryClient = useQueryClient();

  useEffect(() => {
    const stored = localStorage.getItem("token");
    if (stored) {
      setToken(stored);
    }
    setIsLoading(false);
  }, []);

  const login = useCallback((jwt) => {
    localStorage.setItem("token", jwt);
    setToken(jwt);
    queryClient.clear();
    router.push("/dashboard");
  }, [router, queryClient]);

  const logout = useCallback(() => {
    localStorage.removeItem("token");
    setToken(null);
    queryClient.clear();
    router.push("/");
  }, [router, queryClient]);

  const isAuthenticated = !!token;

  return (
    <AuthContext.Provider value={{ token, isAuthenticated, isLoading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
