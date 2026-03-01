"use client";

import { useAuth } from "@/lib/auth";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useState, Suspense } from "react";
import { useQuery } from "@tanstack/react-query";
import { journalAPI, collectionAPI } from "@/lib/api";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Search, X, PenLine } from "lucide-react";
import Link from "next/link";

function JournalListContent() {
  const { isAuthenticated, isLoading: authLoading } = useAuth();
  const router = useRouter();
  const searchParams = useSearchParams();

  const [keyword, setKeyword] = useState(searchParams.get("q") || "");
  const [emotionFilter, setEmotionFilter] = useState(searchParams.get("emotion") || "");
  const [collectionFilter, setCollectionFilter] = useState(searchParams.get("collection") || "");

  useEffect(() => {
    if (!authLoading && !isAuthenticated) router.push("/auth");
  }, [isAuthenticated, authLoading, router]);

  const { data: entries, isLoading } = useQuery({
    queryKey: ["journals"],
    queryFn: () => journalAPI.getAll().then((r) => r.data),
    enabled: isAuthenticated,
  });

  const { data: collections } = useQuery({
    queryKey: ["collections"],
    queryFn: () => collectionAPI.getAll().then((r) => r.data),
    enabled: isAuthenticated,
  });

  // Client-side filtering
  const filtered = entries?.filter((entry) => {
    if (keyword) {
      const q = keyword.toLowerCase();
      const matchTitle = entry.title?.toLowerCase().includes(q);
      const matchContent = entry.content?.toLowerCase().includes(q);
      const matchKeywords = entry.keywords?.some((k) => k.toLowerCase().includes(q));
      if (!matchTitle && !matchContent && !matchKeywords) return false;
    }
    if (emotionFilter) {
      if (!entry.emotions?.some((e) => e.toLowerCase() === emotionFilter.toLowerCase())) return false;
    }
    if (collectionFilter) {
      if (entry.collectionId !== collectionFilter) return false;
    }
    return true;
  });

  // Collect all unique emotions from entries
  const allEmotions = [...new Set(entries?.flatMap((e) => e.emotions || []) || [])];

  const hasFilters = keyword || emotionFilter || collectionFilter;

  const clearFilters = () => {
    setKeyword("");
    setEmotionFilter("");
    setCollectionFilter("");
  };

  if (authLoading || !isAuthenticated) return null;

  return (
    <div className="container mx-auto px-4 py-8 max-w-4xl">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Journal Entries</h1>
        <Link href="/journal/new">
          <Button className="bg-orange-600 hover:bg-orange-700 text-white rounded-xl">
            <PenLine className="h-4 w-4 mr-1.5" /> Write
          </Button>
        </Link>
      </div>

      {/* Filters */}
      <div className="flex flex-wrap items-center gap-3 mb-6">
        <div className="relative flex-1 min-w-[200px]">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
          <Input
            placeholder="Search entries..."
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            className="pl-9 rounded-xl bg-white/80"
          />
        </div>
        <select
          value={emotionFilter}
          onChange={(e) => setEmotionFilter(e.target.value)}
          className="h-10 rounded-xl border border-gray-200 bg-white/80 px-3 text-sm text-gray-700 focus:outline-none focus:ring-2 focus:ring-orange-500"
        >
          <option value="">All emotions</option>
          {allEmotions.map((em) => (
            <option key={em} value={em}>{em}</option>
          ))}
        </select>
        <select
          value={collectionFilter}
          onChange={(e) => setCollectionFilter(e.target.value)}
          className="h-10 rounded-xl border border-gray-200 bg-white/80 px-3 text-sm text-gray-700 focus:outline-none focus:ring-2 focus:ring-orange-500"
        >
          <option value="">All collections</option>
          {collections?.map((col) => (
            <option key={col.id} value={col.id}>{col.name}</option>
          ))}
        </select>
        {hasFilters && (
          <Button variant="ghost" size="sm" onClick={clearFilters} className="text-gray-500 hover:text-red-600 rounded-xl">
            <X className="h-4 w-4 mr-1" /> Clear
          </Button>
        )}
      </div>

      {/* Entries List */}
      {isLoading ? (
        <div className="space-y-4">
          {[1, 2, 3, 4].map((i) => (
            <Skeleton key={i} className="h-24 rounded-2xl" />
          ))}
        </div>
      ) : filtered?.length > 0 ? (
        <div className="space-y-4">
          {filtered.map((entry) => (
            <EntryCard key={entry.id} entry={entry} collections={collections} />
          ))}
        </div>
      ) : (
        <div className="text-center py-16 text-gray-400">
          {hasFilters ? (
            <p>No entries match your filters.</p>
          ) : (
            <>
              <p className="mb-4">No journal entries yet.</p>
              <Link href="/journal/new">
                <Button className="bg-orange-600 hover:bg-orange-700 text-white rounded-xl">
                  Write your first entry
                </Button>
              </Link>
            </>
          )}
        </div>
      )}
    </div>
  );
}

function EntryCard({ entry, collections }) {
  const date = entry.date
    ? new Date(entry.date).toLocaleDateString("en-US", {
        month: "short",
        day: "numeric",
        year: "numeric",
      })
    : "";

  const snippet = entry.content
    ? entry.content.replace(/<[^>]*>/g, "").substring(0, 180)
    : "";

  const collectionName =
    entry.collectionName ||
    collections?.find((c) => c.id === entry.collectionId)?.name;

  return (
    <Link href={`/journal/${entry.id}`}>
      <Card className="rounded-2xl shadow-sm hover:shadow-md transition-shadow bg-white/80 backdrop-blur-sm cursor-pointer">
        <CardContent className="p-5">
          <div className="flex items-start justify-between mb-2">
            <h3 className="font-semibold text-gray-900 hover:text-orange-600 transition-colors">
              {entry.title}
            </h3>
            <span className="text-xs text-gray-400 whitespace-nowrap ml-3">{date}</span>
          </div>
          {snippet && (
            <p className="text-sm text-gray-500 line-clamp-2 mb-3">{snippet}...</p>
          )}
          <div className="flex items-center gap-2 flex-wrap">
            {collectionName && (
              <Badge className="bg-orange-50 text-orange-700 border-orange-200 text-xs">
                {collectionName}
              </Badge>
            )}
            {entry.sentimentLabel && (
              <Badge variant="secondary" className="text-xs">
                {entry.sentimentLabel}
              </Badge>
            )}
            {entry.sentimentScore != null && (
              <Badge variant="outline" className="text-xs">
                mood: {((entry.sentimentScore + 1) * 4.5 + 1).toFixed(1)}
              </Badge>
            )}
            {entry.emotions?.slice(0, 3).map((e) => (
              <Badge key={e} variant="outline" className="text-xs">
                {e}
              </Badge>
            ))}
          </div>
        </CardContent>
      </Card>
    </Link>
  );
}

export default function JournalPage() {
  return (
    <Suspense fallback={<div className="container mx-auto px-4 py-8"><Skeleton className="h-96 rounded-2xl" /></div>}>
      <JournalListContent />
    </Suspense>
  );
}
