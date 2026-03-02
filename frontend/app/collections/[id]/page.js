"use client";

import { useAuth } from "@/lib/auth";
import { useRouter, useParams } from "next/navigation";
import { useEffect, useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { collectionAPI, journalAPI } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { Card, CardContent } from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
  DialogClose,
} from "@/components/ui/dialog";
import { ArrowLeft, Plus, Search, Check } from "lucide-react";
import Link from "next/link";
import { toast } from "sonner";
import EntryCard from "@/components/EntryCard";

export default function CollectionDetailPage() {
  const { isAuthenticated, isLoading: authLoading } = useAuth();
  const router = useRouter();
  const params = useParams();
  const collectionId = params.id;
  const queryClient = useQueryClient();

  useEffect(() => {
    if (!authLoading && !isAuthenticated) router.push("/auth");
  }, [isAuthenticated, authLoading, router]);

  const { data: collection } = useQuery({
    queryKey: ["collection", collectionId],
    queryFn: () => collectionAPI.getById(collectionId).then((r) => r.data),
    enabled: isAuthenticated && !!collectionId,
  });

  const { data: entries, isLoading } = useQuery({
    queryKey: ["collection-entries", collectionId],
    queryFn: () => collectionAPI.getEntries(collectionId).then((r) => r.data),
    enabled: isAuthenticated && !!collectionId,
  });

  const removeMutation = useMutation({
    mutationFn: (entryId) => journalAPI.removeCollection(entryId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["collection-entries", collectionId] });
      queryClient.invalidateQueries({ queryKey: ["journals"] });
      toast.success("Entry removed from collection");
    },
    onError: () => toast.error("Failed to remove entry"),
  });

  if (authLoading || !isAuthenticated) return null;

  return (
    <div className="container mx-auto px-4 py-8 max-w-7xl">
      <div className="flex items-center gap-3 mb-8">
        <Button
          variant="ghost"
          size="icon"
          onClick={() => router.back()}
          className="rounded-xl"
        >
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <div className="flex-1">
          <div className="flex justify-between items-center gap-3">
            <h1 className="text-4xl font-bold text-orange-600">
              {collection?.name || "Collection"}
            </h1>
            <AddEntryDialog collectionId={collectionId} existingEntryIds={entries?.map((e) => e.id) || []} />
          </div>
          {collection?.description && (
            <p className="text-md text-black-500">{collection.description}</p>
          )}
        </div>
      </div>

      {isLoading ? (
        <div className="space-y-4">
          {[1, 2, 3].map((i) => (
            <Skeleton key={i} className="h-24 rounded-2xl" />
          ))}
        </div>
      ) : entries?.length > 0 ? (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {entries.map((entry) => (
            <EntryCard
              key={entry.id}
              entry={entry}
              showCollection={false}
              onRemove={(id) => removeMutation.mutate(id)}
            />
          ))}
        </div>
      ) : (
        <div className="text-center py-16 text-gray-400">
          <p className="mb-4">No entries in this collection yet.</p>
          <Link href="/journal/new">
            <Button className="bg-orange-600 hover:bg-orange-700 text-white">
              Write your first entry
            </Button>
          </Link>
        </div>
      )}
    </div>
  );
}

function AddEntryDialog({ collectionId, existingEntryIds }) {
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState("");
  const [selected, setSelected] = useState([]);
  const queryClient = useQueryClient();

  const { data: allEntries, isLoading } = useQuery({
    queryKey: ["journals"],
    queryFn: () => journalAPI.getAll().then((r) => r.data),
    enabled: open,
  });

  // Filter out entries already in this collection, and apply search
  const available = allEntries?.filter((entry) => {
    if (existingEntryIds.includes(entry.id)) return false;
    if (search) {
      const q = search.toLowerCase();
      return (
        entry.title?.toLowerCase().includes(q) ||
        entry.content?.toLowerCase().includes(q)
      );
    }
    return true;
  });

  const toggleSelect = (id) => {
    setSelected((prev) =>
      prev.includes(id) ? prev.filter((s) => s !== id) : [...prev, id]
    );
  };

  const addMutation = useMutation({
    mutationFn: async (entryIds) => {
      const results = await Promise.allSettled(
        entryIds.map((id) => journalAPI.assignCollection(id, collectionId))
      );
      const failed = results.filter((r) => r.status === "rejected").length;
      if (failed > 0) throw new Error(`${failed} entries failed to add`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["collection-entries", collectionId] });
      queryClient.invalidateQueries({ queryKey: ["journals"] });
      toast.success(`${selected.length} ${selected.length === 1 ? "entry" : "entries"} added to collection`);
      setSelected([]);
      setOpen(false);
    },
    onError: (err) => toast.error(err.message || "Failed to add entries"),
  });

  const handleAdd = () => {
    if (selected.length === 0) return;
    addMutation.mutate(selected);
  };

  return (
    <Dialog open={open} onOpenChange={(v) => { setOpen(v); if (!v) { setSearch(""); setSelected([]); } }}>
      <DialogTrigger asChild>
        <Button size="sm" className="bg-orange-600 hover:bg-orange-700 text-white h-8 px-3 py-5 text-md">
          <Plus className="h-3.5 w-3.5 mr-1" /> Add Entry
        </Button>
      </DialogTrigger>
      <DialogContent className="bg-white/80 backdrop-blur-sm border border-gray-200 max-w-lg max-h-[80vh] flex flex-col">
        <DialogHeader>
          <DialogTitle>Add Entries to Collection</DialogTitle>
        </DialogHeader>

        {/* Search */}
        <div className="relative mt-2">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
          <Input
            placeholder="Search entries..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="pl-9 rounded-sm"
          />
        </div>

        {/* Entry list */}
        <div className="flex-1 overflow-y-auto space-y-2 mt-2 max-h-[50vh] pr-1">
          {isLoading ? (
            <div className="space-y-2">
              {[1, 2, 3].map((i) => (
                <Skeleton key={i} className="h-16 rounded-xl" />
              ))}
            </div>
          ) : available?.length > 0 ? (
            available.map((entry) => {
              const isSelected = selected.includes(entry.id);
              const date = entry.date
                ? new Date(entry.date).toLocaleDateString("en-US", { month: "short", day: "numeric" })
                : "";

              let snippet = "";
              if (entry.content) {
                const plainText = entry.content
                  .replace(/<\/p>/gi, "\n")
                  .replace(/<br\s*\/?>/gi, "\n")
                  .replace(/<[^>]*>/g, "");
                const firstLine = plainText.split(/\r?\n/).find((l) => l.trim().length > 0) || "";
                snippet = firstLine.substring(0, 60);
              }

              return (
                <button
                  key={entry.id}
                  type="button"
                  onClick={() => toggleSelect(entry.id)}
                  className={`w-full text-left p-3 rounded-lg border transition-all cursor-pointer ${
                    isSelected
                      ? "border-orange-400 bg-orange-50 ring-1 ring-orange-300"
                      : "border-gray-200  hover:border-orange-200 hover:bg-orange-100/30"
                  }`}
                >
                  <div className="flex items-start gap-3">
                    <div className={`mt-0.5 flex-shrink-0 h-5 w-5 rounded-md border-2 flex items-center justify-center transition-colors ${
                      isSelected ? "bg-orange-600 border-orange-600" : "border-gray-300"
                    }`}>
                      {isSelected && <Check className="h-3 w-3 text-white" />}
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2">
                        <span className="font-medium text-sm text-gray-900 truncate">{entry.title}</span>
                        {date && <span className="text-xs text-gray-400 whitespace-nowrap">{date}</span>}
                      </div>
                      {snippet && (
                        <p className="text-xs text-gray-500 mt-0.5 truncate">{snippet}</p>
                      )}
                    </div>
                  </div>
                </button>
              );
            })
          ) : (
            <p className="text-sm text-gray-400 text-center py-8">
              {search ? "No matching entries found." : "No entries available to add."}
            </p>
          )}
        </div>

        {/* Footer */}
        <div className="flex items-center justify-between pt-3 border-t border-gray-100">
          <span className="text-sm text-gray-500">
            {selected.length} {selected.length === 1 ? "entry" : "entries"} selected
          </span>
          <div className="flex gap-2">
            <DialogClose asChild>
              <Button variant="ghost" className="hover:bg-orange-100">Cancel</Button>
            </DialogClose>
            <Button
              onClick={handleAdd}
              disabled={selected.length === 0 || addMutation.isPending}
              className="bg-orange-600 hover:bg-orange-700 text-white"
            >
              {addMutation.isPending ? "Adding..." : "Add"}
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}

