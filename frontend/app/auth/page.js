"use client";

import { useState } from "react";
import { useAuth } from "@/lib/auth";
import { authAPI } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { toast } from "sonner";
import { useRouter } from "next/navigation";
import { useEffect } from "react";

export default function AuthPage() {
  const [isLogin, setIsLogin] = useState(true);
  const [userName, setUserName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [sentimentAnalysis, setSentimentAnalysis] = useState(false);
  const [loading, setLoading] = useState(false);
  const { login, isAuthenticated, isLoading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!isLoading && isAuthenticated) {
      router.push("/dashboard");
    }
  }, [isAuthenticated, isLoading, router]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      if (isLogin) {
        const res = await authAPI.login(userName, password);
        login(res.data);
        toast.success("Welcome back!");
      } else {
        if (!email) {
          toast.error("Email is required for signup");
          setLoading(false);
          return;
        }
        await authAPI.signup(userName, password, email, sentimentAnalysis);
        toast.success("Account created! Logging you in...");
        // Auto-login after signup
        const res = await authAPI.login(userName, password);
        login(res.data);
      }
    } catch (err) {
      const msg =
        err.response?.data || (isLogin ? "Invalid credentials" : "Signup failed");
      toast.error(typeof msg === "string" ? msg : "Something went wrong");
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleLogin = () => {
    window.location.href = authAPI.googleOAuthUrl();
  };

  if (isLoading) return null;

  return (
    <div className="min-h-[80vh] flex items-center justify-center px-4">
      <Card className="w-full max-w-md rounded-2xl shadow-lg bg-white/90 dark:bg-gray-900/90 backdrop-blur-sm">
        <CardHeader className="text-center pb-2">
          <CardTitle className="text-2xl font-bold">
            {isLogin ? "Welcome Back" : "Create Account"}
          </CardTitle>
          <p className="text-gray-500 dark:text-gray-400 text-sm mt-1">
            {isLogin
              ? "Sign in to continue journaling"
              : "Start your journaling journey"}
          </p>
        </CardHeader>
        <CardContent className="space-y-4 pt-4">
          {/* Google OAuth */}
          <Button
            variant="secondary"
            className="w-full rounded-xl py-5 border-gray-200 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-800"
            onClick={handleGoogleLogin}
          >
            <svg className="h-5 w-5 mr-2" viewBox="0 0 24 24">
              <path
                d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z"
                fill="#4285F4"
              />
              <path
                d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                fill="#34A853"
              />
              <path
                d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
                fill="#FBBC05"
              />
              <path
                d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
                fill="#EA4335"
              />
            </svg>
            Continue with Google
          </Button>

          <div className="relative">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-gray-200 dark:border-gray-700" />
            </div>
            <div className="relative flex justify-center text-xs uppercase">
              <span className="bg-white dark:bg-gray-900 px-2 text-gray-400">or</span>
            </div>
          </div>

          {/* Email/Password Form */}
          <form onSubmit={handleSubmit} className="space-y-3">
            <Input
              placeholder="Username"
              value={userName}
              onChange={(e) => setUserName(e.target.value)}
              required
              className="rounded-xl"
            />
            {!isLogin && (
              <Input
                type="email"
                placeholder="Email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                className="rounded-xl"
              />
            )}
            <Input
              type="password"
              placeholder="Password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              className="rounded-xl"
            />
            {!isLogin && (
              <label className="flex items-start gap-3 p-3 rounded-xl border border-gray-200 dark:border-gray-700 cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors">
                <input
                  type="checkbox"
                  checked={sentimentAnalysis}
                  onChange={(e) => setSentimentAnalysis(e.target.checked)}
                  className="mt-0.5 h-4 w-4 rounded accent-orange-600"
                />
                <div>
                  <p className="text-sm font-medium text-gray-800 dark:text-gray-200">Enable Sentiment Analysis</p>
                  <p className="text-xs text-gray-500 dark:text-gray-400 mt-0.5">
                    Receive biweekly mental health reports based on your journal entries. You can change this later.
                  </p>
                </div>
              </label>
            )}
            <Button
              type="submit"
              disabled={loading}
              className="w-full bg-orange-600 hover:bg-orange-700 text-white rounded-xl py-5"
            >
              {loading
                ? "Please wait..."
                : isLogin
                ? "Sign In"
                : "Create Account"}
            </Button>
          </form>

          <p className="text-center text-sm text-gray-500 dark:text-gray-400">
            {isLogin ? "Don't have an account?" : "Already have an account?"}{" "}
            <button 
              onClick={() => setIsLogin(!isLogin)}
              className="text-orange-600 hover:underline font-medium cursor-pointer"
            >
              {isLogin ? "Sign up" : "Sign in"}
            </button>
          </p>
        </CardContent>
      </Card>
    </div>
  );
}
