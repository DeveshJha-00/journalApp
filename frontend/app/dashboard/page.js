"use client";

import { useAuth } from "@/lib/auth";
import { useRouter } from "next/navigation";
import {useState } from "react";
import { useEffect } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { userAPI, journalAPI } from "@/lib/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Toggle } from "@/components/ui/toggle";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
  DialogClose
} from "@/components/ui/dialog";
import {
  BookOpen,
  TrendingUp,
  Smile,
  FileText,
  ArrowRight,
  Pencil,
} from "lucide-react";
import MoodChart from "@/components/MoodChart";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import Link from "next/link";
import { toast } from "sonner";

function getMoodMessage(avgMood) {
  if (avgMood >= 8) return { text: "You've been feeling great!", color: "text-green-600" };
  if (avgMood >= 6) return { text: "Your mood is steady. Keep it up!", color: "text-blue-600" };
  if (avgMood >= 4) return { text: "Your mood has dipped slightly. Stay mindful.", color: "text-amber-600" };
  if (avgMood > 0) return { text: "It's been a tough stretch. Be gentle with yourself.", color: "text-orange-600" };
  return {text: "Start journaling to get analysis!", color: "text-orange-600" };
}

export default function DashboardPage() {
  const { isAuthenticated, isLoading: authLoading, login: authLogin } = useAuth();
  const router = useRouter();
  const [period, setPeriod] = useState("7d");
  const [editingUsername, setEditingUsername] = useState(false);
  const [newUsername, setNewUsername] = useState("");

  useEffect(() => {
    if (!authLoading && !isAuthenticated) router.push("/auth");
  }, [isAuthenticated, authLoading, router]);

  const { data: analytics, isLoading: analyticsLoading } = useQuery({
    queryKey: ["analytics", period],
    queryFn: () => userAPI.getAnalytics(period).then((r) => r.data),
    enabled: isAuthenticated,
  });

  const { data: reports, isLoading: reportsLoading } = useQuery({
    queryKey: ["reports"],
    queryFn: () => userAPI.getReports().then((r) => r.data),
    enabled: isAuthenticated,
  });

  const { data: currentUser, isLoading: userLoading } = useQuery({
    queryKey: ["currentUser"],
    queryFn: () => userAPI.getMe().then((r) => r.data),
    enabled: isAuthenticated,
  });

  const queryClient = useQueryClient();

  const sentimentMutation = useMutation({
    mutationFn: (enabled) => userAPI.toggleSentimentAnalysis(enabled),
    onSuccess: (res) => {
      queryClient.invalidateQueries({ queryKey: ["currentUser"] });
      toast.success(
        res.data.sentimentAnalysis
          ? "Sentiment analysis enabled — you'll receive biweekly reports."
          : "Sentiment analysis disabled — biweekly reports paused."
      );
    },
    onError: () => {
      toast.error("Failed to update sentiment analysis preference.");
    },
  });

  const usernameMutation = useMutation({
    mutationFn: (userName) => userAPI.updateUsername(userName),
    onSuccess: (res) => {
      // Update JWT since username changed
      if (res.data.token) {
        authLogin(res.data.token);
      }
      queryClient.invalidateQueries({ queryKey: ["currentUser"] });
      setEditingUsername(false);
      toast.success(`Username updated to "${res.data.userName}"`);
    },
    onError: (err) => {
      const msg = err.response?.data;
      toast.error(typeof msg === "string" ? msg : "Failed to update username.");
    },
  });

  if (authLoading || !isAuthenticated) return null;

  const mood = analytics ? getMoodMessage(analytics.avgMood) : null;

  return (
    <div className="container mx-auto px-4 py-8 max-w-7xl">

      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-4xl font-bold gradient-title text-orange-600">Dashboard</h1>
          {!userLoading && currentUser && (
            <div className="flex items-center gap-2 mt-1">
              {editingUsername ? (
                <form
                  className="flex items-center gap-2"
                  onSubmit={(e) => {
                    e.preventDefault();
                    if (newUsername.trim() && newUsername.trim() !== currentUser.userName) {
                      usernameMutation.mutate(newUsername.trim());
                    } else {
                      setEditingUsername(false);
                    }
                  }}
                >
                  <Input
                    value={newUsername}
                    onChange={(e) => setNewUsername(e.target.value)}
                    className="h-7 w-48 text-sm rounded-lg"
                    autoFocus
                    disabled={usernameMutation.isPending}
                  />
                  <Button
                    type="submit"
                    size="sm"
                    className="h-7 px-3 text-xs bg-orange-600 hover:bg-orange-700"
                    disabled={usernameMutation.isPending}
                  >
                    {usernameMutation.isPending ? "..." : "Save"}
                  </Button>
                  <Button
                    type="button"
                    size="sm"
                    variant="ghost"
                    className="h-7 px-2 text-xs"
                    onClick={() => setEditingUsername(false)}
                  >
                    Cancel
                  </Button>
                </form>
              ) : (
                <div className="flex items-center gap-1.5">
                  <span className="text-lg text-gray-500">
                    Welcome, <span className="font-medium text-gray-700">{currentUser.userName}</span>
                  </span>
                  <button
                    onClick={() => {
                      setNewUsername(currentUser.userName);
                      setEditingUsername(true);
                    }}
                    className="text-gray-400 hover:text-orange-600 transition-colors cursor-pointer"
                    title="Edit username"
                  >
                    <Pencil className="h-4 w-4" />
                  </button>
                </div>
              )}
            </div>
          )}
        </div>

        <Select value={period} onValueChange={setPeriod}>
          <SelectTrigger className="w-[140px]">
            <SelectValue />
          </SelectTrigger>
          <SelectContent className="bg-white/70 backdrop-blur-sm border border-gray-200">
            <SelectItem value="7d">Last 7 Days</SelectItem>
            <SelectItem value="15d">Last 15 Days</SelectItem>
            <SelectItem value="30d">Last 30 Days</SelectItem>
          </SelectContent>
        </Select>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* ===== Main Content (2/3) ===== */}
        <div className="lg:col-span-2 space-y-6">
          {/* Stats Row */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            {/* Total Entries */}
            <Card className="bg-transparent shadow-md">
              <CardContent className="pt-2 pb-2">
                <div className="flex items-center gap-2">
                  <BookOpen className="h-4 w-4 text-orange-600" />
                  <p className="text-2x1 font-semibold text-gray-800">
                    Total Entries
                  </p>
                </div>

                {analyticsLoading ? (
                  <Skeleton className="h-7 w-12 mt-2" />
                ) : (
                  <>
                    <p className="text-3xl font-bold text-gray-900 mt-1">
                      {analytics?.totalEntries ?? 0}
                    </p>

                    <p className="text-xs text-gray-500 mt-1">
                      ~ {analytics?.entriesPerDay?.toFixed(1) ?? "0"} entries per day
                    </p>
                  </>
                )}
              </CardContent>
            </Card>         

            {/* Average Mood */}
            <Card className="bg-transparent shadow-md">
            <CardContent className="pt-2 pb-2">
              <div className="flex items-center gap-2">
                <TrendingUp className="h-4 w-4 text-blue-600" />
                <p className="text-2x1 font-semibold text-gray-800">
                  Average Mood
                </p>
              </div>

              {analyticsLoading ? (
                <Skeleton className="h-7 w-16 mt-2" />
              ) : (
                <>
                  <p className="text-3xl font-bold text-gray-900 mt-1">
                    {analytics?.avgMood?.toFixed(1) ?? "—"} / 10
                  </p>
                  <p className="text-xs text-gray-500 mt-1">
                    Overall mood score
                  </p>
                </>
              )}
            </CardContent>
          </Card>

            {/* Mood Summary */}
            <Card className=" bg-transparent shadow-md">
            <CardContent className="pt-2 pb-2">
              <div className="flex items-center gap-2">
                <Smile className="h-4 w-4 text-green-600" />
                <p className="text-2x1 font-semibold text-gray-800">
                  Mood Summary
                </p>
              </div>

              {analyticsLoading ? (
                <Skeleton className="h-5 w-40 mt-2" />
              ) : (
                <p className="text-3x1 font-bold text-gray-700 mt-2 leading-relaxed">
                  {mood?.text ?? "Start journaling!"}
                </p>
              )}
            </CardContent>
          </Card>

          </div>

          {/* Mood Timeline Chart */}
          <Card className="bg-transparent shadow-md">
            <CardHeader className="pb-2">
              <CardTitle className="text-lg font-semibold">Mood Timeline</CardTitle>
            </CardHeader>
            <CardContent>
              {analyticsLoading ? (
                <Skeleton className="h-64 w-full rounded-md" />
              ) : analytics?.moodTimeline?.length > 0 ? (
                <MoodChart data={analytics.moodTimeline} period={period} />
              ) : (
                <div className="h-64 flex items-center justify-center text-gray-400">
                  No mood data yet. Start journaling to see trends!
                </div>
              )}
            </CardContent>
          </Card>

        </div>

        {/* ===== Sidebar — Reports (1/3) ===== */}
        <div className="space-y-6 h-full">
          <Card className="shadow-md bg-transparent h-full">
            <CardHeader className="pb-2">
              <div className="flex items-center justify-between">
                <CardTitle className="text-lg font-semibold flex items-center gap-2">
                  <FileText className="h-5 w-5 text-orange-600" />
                  Biweekly Reports
                </CardTitle>
                {userLoading ? (
                  <Skeleton className="h-8 w-14 rounded-md" />
                ) : (
                  <Toggle
                    pressed={currentUser?.sentimentAnalysis ?? false}
                    onPressedChange={(pressed) => sentimentMutation.mutate(pressed)}
                    disabled={sentimentMutation.isPending}
                    aria-label="Toggle sentiment analysis"
                    className={`px-6 py-2 font-medium rounded-full border transition-colors ${
                      currentUser?.sentimentAnalysis
                        ? "bg-orange-100 text-orange-700 border-orange-200 data-[state=on]:bg-orange-100 data-[state=on]:text-orange-700"
                        : "bg-gray-100 text-gray-500 border-gray-200"
                    }`}
                  >
                    {currentUser?.sentimentAnalysis ? "On" : "Off"}
                  </Toggle>
                )}
              </div>
              <p className="text-xs text-gray-500 mt-1">
                {currentUser?.sentimentAnalysis
                  ? "Your journal entries are being analyzed biweekly."
                  : "Turn on to receive biweekly mental health reports."}
              </p>
            </CardHeader>
            <CardContent>
              {reportsLoading ? (
                <div className="space-y-3">
                  {[1, 2, 3].map((i) => (
                    <Skeleton key={i} className="h-20 w-full"/>
                  ))}
                </div>
              ) : reports?.length > 0 ? (
                <div className="space-y-3">
                  {reports.slice(0, 5).map((report) => (
                    <ReportCard key={report.id} report={report} />
                  ))}
                </div>
              ) : (
                <p className="text-gray-400 text-sm py-6 text-center">
                  No reports yet. Reports are generated biweekly once you have enough entries.
                </p>
              )}
            </CardContent>
          </Card>

        </div>
      </div>
    </div>
  );
}

function ReportCard({ report }) {
  const date = report.generatedAt
    ? new Date(report.generatedAt).toLocaleDateString("en-US", {
        month: "short",
        day: "numeric",
        year: "numeric",
      })
    : "Unknown date";

  return (
    <Dialog>

      <DialogTrigger asChild>
        <button className="cursor-pointer w-full text-left p-3 rounded-xl border-2 shadow md hover:bg-orange-50/50 transition-colors">
          <div className="flex items-center justify-between mb-1">
            <span className="font-medium text-gray-900">{date}</span>
            {report.avgMood && (
              <Badge variant="secondary" className="bg-orange-50 text-orange-700 text-sm">
                Mood: {report.avgMood.toFixed(1)}
              </Badge>
            )}
          </div>
          <p className="text-sm text-gray-500">
            {report.totalEntries} entries analyzed
          </p>
          {report.topEmotions?.length > 0 && (
            <div className="flex gap-1 mt-2">
              {report.topEmotions.slice(0, 2).map((e) => (
                <Badge key={e} variant="secondary" className="text-[14px] py-0 px-1.5">
                  {e.charAt(0).toUpperCase() + e.slice(1)}
                </Badge>
              ))}
            </div>
          )}
        </button>
      </DialogTrigger>

      <DialogContent className="max-w-5xl max-h-[85vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Report — {date}</DialogTitle>
        </DialogHeader>
        <div
          className="prose prose-sm max-w-none prose-headings:text-orange-800"
          dangerouslySetInnerHTML={{ __html: report.reportContent }}
        />
        <div className="mt-6 flex justify-end">
        <DialogClose asChild>
          <Button variant="outline">Close</Button>
        </DialogClose>
      </div>
      </DialogContent>
      
    </Dialog>
  );
}
