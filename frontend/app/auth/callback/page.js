"use client";

import { useEffect } from "react";
import { useSearchParams } from "next/navigation";
import { useAuth } from "@/lib/auth";
import { toast } from "sonner";
import { Suspense } from "react";

function CallbackHandler() {
  const searchParams = useSearchParams();
  const { login } = useAuth();

  useEffect(() => {
    const token = searchParams.get("token");
    const error = searchParams.get("error");

    if (error) {
      toast.error("Authentication failed: " + error);
      window.location.href = "/auth";
      return;
    }

    if (token) {
      login(token);
      toast.success("Welcome!");
    } else {
      toast.error("No token received");
      window.location.href = "/auth";
    }
  }, [searchParams, login]);

  return (
    <div className="min-h-[80vh] flex items-center justify-center">
      <div className="text-center">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-orange-600 mx-auto mb-4" />
        <p className="text-gray-600 dark:text-gray-400">Signing you in...</p>
      </div>
    </div>
  );
}

export default function AuthCallbackPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-[80vh] flex items-center justify-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-orange-600" />
        </div>
      }
    >
      <CallbackHandler />
    </Suspense>
  );
}
