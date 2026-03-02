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
import { Calendar } from "@/components/ui/calendar";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Search, X, PenLine, CalendarIcon, ChevronDown } from "lucide-react";
import Link from "next/link";
import { format } from "date-fns";
import EntryCard from "@/components/EntryCard";

function JournalListContent() {
  const { isAuthenticated, isLoading: authLoading } = useAuth();
  const router = useRouter();
  const searchParams = useSearchParams();

  const [keyword, setKeyword] = useState(searchParams.get("q") || "");
  const [emotionFilter, setEmotionFilter] = useState(searchParams.get("emotion") || "");
  const [collectionFilter, setCollectionFilter] = useState(searchParams.get("collection") || "");
  const [dateFilter, setDateFilter] = useState(null);
  const [calendarOpen, setCalendarOpen] = useState(false);

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
    if (dateFilter) {
      const entryDate = entry.date ? new Date(entry.date).toDateString() : null;
      if (entryDate !== dateFilter.toDateString()) return false;
    }
    return true;
  });

  // Collect all unique emotions from entries
  const allEmotions = [...new Set(entries?.flatMap((e) => e.emotions || []) || [])];

  const hasFilters = keyword || emotionFilter || collectionFilter || dateFilter;

  const clearFilters = () => {
    setKeyword("");
    setEmotionFilter("");
    setCollectionFilter("");
    setDateFilter(null);
  };

  if (authLoading || !isAuthenticated) return null;

  return (
    <div className="container mx-auto px-4 py-8 max-w-7xl">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-4xl font-bold text-orange-600">My Journal Entries</h1>
        <Link href="/journal/new">
          <Button className="bg-orange-600 hover:bg-orange-700 text-white rounded-xl">
            <PenLine className="h-4 w-4 mr-1.5" /> New
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
            className="pl-9 rounded-sm"
          />
        </div>

        {/* Date Filter */}
        <Popover open={calendarOpen} onOpenChange={setCalendarOpen}>
          <PopoverTrigger asChild>
            <Button
              variant="outline"
              className={`h-10 border-gray-200 bg-transparent shadow md px-3 text-sm font-semibold ${
                dateFilter ? "text-gray-900" : "text-gray-500"
              }`}
            >
              <CalendarIcon className="h-4 w-4 mr-2 text-gray-600" />
              {dateFilter ? format(dateFilter, "MMM d, yyyy") : "Pick a date"}
            </Button>
          </PopoverTrigger>
          <PopoverContent className="w-auto p-0 bg-white/70 backdrop-blur-sm border border-gray-200" align="start">
            <Calendar
              mode="single"
              selected={dateFilter}
              onSelect={(date) => {
                setDateFilter(date);
                setCalendarOpen(false);
              }}
            />
          </PopoverContent>
        </Popover>

        {/* Emotion Filter */}
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button
              variant="outline"
              className={`h-10 bg-transparent shadow md border-gray-200 px-3 text-sm font-semibold ${
                emotionFilter ? "text-gray-900" : "text-gray-500"
              }`}
            >
              {emotionFilter
                ? emotionFilter.charAt(0).toUpperCase() + emotionFilter.slice(1)
                : "All emotions"}
              <ChevronDown className="h-4 w-4 ml-2 text-gray-400" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="start" className="max-h-60 overflow-y-auto bg-white/70 backdrop-blur-sm border border-gray-200">
            <DropdownMenuItem onClick={() => setEmotionFilter("")}>
              All emotions
            </DropdownMenuItem>
            {allEmotions.map((em) => (
              <DropdownMenuItem className="cursor-pointer" key={em} onClick={() => setEmotionFilter(em)}>
                {em.charAt(0).toUpperCase() + em.slice(1)}
              </DropdownMenuItem>
            ))}
          </DropdownMenuContent>
        </DropdownMenu>

        {/* Collection Filter */}
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button
              variant="outline"
              className={`h-10 bg-transparent shadow md border-gray-200 px-3 text-sm font-semibold ${
                collectionFilter ? "text-gray-900" : "text-gray-500"
              }`}
            >
              {collectionFilter
                ? collections?.find((c) => c.id === collectionFilter)?.name || "Collection"
                : "All collections"}
              <ChevronDown className="h-4 w-4 ml-2 text-gray-400" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="start" className="max-h-60 overflow-y-auto bg-white/70 backdrop-blur-smborder border-gray-200">
            <DropdownMenuItem onClick={() => setCollectionFilter("")}>
              All collections
            </DropdownMenuItem>
            {collections?.map((col) => (
              <DropdownMenuItem className="cursor-pointer bg-white/70 backdrop-blur-smborder border-gray-200" key={col.id} onClick={() => setCollectionFilter(col.id)}>
                {col.name}
              </DropdownMenuItem>
            ))}
          </DropdownMenuContent>
        </DropdownMenu>

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
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
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
            </>
          )}
        </div>
      )}
    </div>
  );
}

export default function JournalPage() {
  return (
    <Suspense fallback={<div className="container mx-auto px-4 py-8"><Skeleton className="h-96 rounded-2xl" /></div>}>
      <JournalListContent />
    </Suspense>
  );
}
