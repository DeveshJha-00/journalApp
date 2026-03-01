"use client";

import { useAuth } from "@/lib/auth";
import { useRouter, useParams } from "next/navigation";
import { useEffect, useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { journalAPI, collectionAPI } from "@/lib/api";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/tabs";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
import { ArrowLeft, Pencil, Trash2, Save, X } from "lucide-react";
import { toast } from "sonner";
import TipTapEditor from "@/components/TipTapEditor";

export default function JournalEntryPage() {
  const { isAuthenticated, isLoading: authLoading } = useAuth();
  const router = useRouter();
  const params = useParams();
  const entryId = params.id;
  const queryClient = useQueryClient();

  const [editing, setEditing] = useState(false);
  const [editTitle, setEditTitle] = useState("");
  const [editContent, setEditContent] = useState("");
  const [editCollectionId, setEditCollectionId] = useState("");

  useEffect(() => {
    if (!authLoading && !isAuthenticated) router.push("/auth");
  }, [isAuthenticated, authLoading, router]);

  const { data: entry, isLoading } = useQuery({
    queryKey: ["journal", entryId],
    queryFn: () => journalAPI.getById(entryId).then((r) => r.data),
    enabled: isAuthenticated && !!entryId,
  });

  const { data: collections } = useQuery({
    queryKey: ["collections"],
    queryFn: () => collectionAPI.getAll().then((r) => r.data),
    enabled: isAuthenticated,
  });

  const updateMutation = useMutation({
    mutationFn: (data) => journalAPI.update(entryId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["journal", entryId] });
      queryClient.invalidateQueries({ queryKey: ["journals"] });
      toast.success("Entry updated!");
      setEditing(false);
    },
    onError: () => toast.error("Failed to update entry"),
  });

  const deleteMutation = useMutation({
    mutationFn: () => journalAPI.delete(entryId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["journals"] });
      toast.success("Entry deleted");
      router.push("/journal");
    },
    onError: () => toast.error("Failed to delete entry"),
  });

  const startEditing = () => {
    setEditTitle(entry.title);
    setEditContent(entry.content || "");
    setEditCollectionId(entry.collectionId || "");
    setEditing(true);
  };

  const handleSaveEdit = () => {
    if (!editTitle.trim()) {
      toast.error("Title is required");
      return;
    }
    updateMutation.mutate({
      title: editTitle.trim(),
      content: editContent,
      collectionId: editCollectionId || "",
    });
  };

  if (authLoading || !isAuthenticated) return null;

  if (isLoading) {
    return (
      <div className="container mx-auto px-4 py-8 max-w-3xl">
        <Skeleton className="h-8 w-48 mb-6" />
        <Skeleton className="h-64 rounded-2xl" />
      </div>
    );
  }

  if (!entry) {
    return (
      <div className="container mx-auto px-4 py-16 text-center text-gray-400">
        Entry not found.
      </div>
    );
  }

  const date = entry.date
    ? new Date(entry.date).toLocaleDateString("en-US", {
        weekday: "long",
        month: "long",
        day: "numeric",
        year: "numeric",
      })
    : "";

  const moodScore = entry.sentimentScore != null
    ? ((entry.sentimentScore + 1) * 4.5 + 1).toFixed(1)
    : null;

  const collectionName =
    entry.collectionName ||
    collections?.find((c) => c.id === entry.collectionId)?.name;

  return (
    <div className="container mx-auto px-4 py-8 max-w-7xl">
      {/* Top bar */}
      <div className="flex items-center justify-between mb-6">
        <Button
          variant="ghost"
          onClick={() => router.back()}
          className="rounded-xl text-gray-600"
        >
          <ArrowLeft className="h-4 w-4 mr-1.5" /> Back
        </Button>
        <div className="flex gap-2">
          {!editing && (
            <Button variant="outline" size="sm" onClick={startEditing} className="rounded-xl border-orange-200">
              <Pencil className="h-3.5 w-3.5 mr-1.5" /> Edit
            </Button>
          )}
          <AlertDialog>
            <AlertDialogTrigger asChild>
              <Button variant="ghost" size="sm" className="text-red-500 hover:text-red-700 rounded-xl">
                <Trash2 className="h-3.5 w-3.5 mr-1.5" /> Delete
              </Button>
            </AlertDialogTrigger>
            <AlertDialogContent className="rounded-2xl">
              <AlertDialogHeader>
                <AlertDialogTitle>Delete this entry?</AlertDialogTitle>
                <AlertDialogDescription>
                  This action cannot be undone.
                </AlertDialogDescription>
              </AlertDialogHeader>
              <AlertDialogFooter>
                <AlertDialogCancel className="rounded-xl">Cancel</AlertDialogCancel>
                <AlertDialogAction
                  onClick={() => deleteMutation.mutate()}
                  className="bg-red-600 hover:bg-red-700 rounded-xl"
                >
                  Delete
                </AlertDialogAction>
              </AlertDialogFooter>
            </AlertDialogContent>
          </AlertDialog>
        </div>
      </div>

      {editing ? (
        /* ===== Edit Mode ===== */
        <div className="space-y-4">
          <Input
            value={editTitle}
            onChange={(e) => setEditTitle(e.target.value)}
            className="rounded-xl text-lg font-medium h-12 bg-white/80"
          />
          <select
            value={editCollectionId}
            onChange={(e) => setEditCollectionId(e.target.value)}
            className="w-full h-10 rounded-xl border border-gray-200 bg-white/80 px-3 text-sm text-gray-700"
          >
            <option value="">No collection</option>
            {collections?.map((col) => (
              <option key={col.id} value={col.id}>{col.name}</option>
            ))}
          </select>
          <div className="bg-white/80 backdrop-blur-sm rounded-2xl border border-gray-200 overflow-hidden min-h-[300px]">
            <TipTapEditor content={editContent} onChange={setEditContent} />
          </div>
          <div className="flex justify-end gap-3">
            <Button variant="ghost" onClick={() => setEditing(false)} className="rounded-xl">
              <X className="h-4 w-4 mr-1.5" /> Cancel
            </Button>
            <Button
              onClick={handleSaveEdit}
              disabled={updateMutation.isPending}
              className="bg-orange-600 hover:bg-orange-700 text-white rounded-xl"
            >
              <Save className="h-4 w-4 mr-1.5" />
              {updateMutation.isPending ? "Saving..." : "Save"}
            </Button>
          </div>
        </div>
      ) : (
        /* ===== View Mode ===== */
        <Card className="rounded-2xl shadow-md bg-white/80 backdrop-blur-sm">
          <CardContent className="p-0">
            <Tabs defaultValue="entry" className="w-full">
              <div className="px-6 pt-5 pb-0">
                <TabsList className="bg-gray-100/80 rounded-lg p-1">
                  <TabsTrigger value="entry" className="rounded-md px-5 data-[state=active]:bg-white data-[state=active]:shadow-sm">
                    Entry
                  </TabsTrigger>
                  <TabsTrigger value="analysis" className="rounded-md px-5 data-[state=active]:bg-white data-[state=active]:shadow-sm">
                    Analysis
                  </TabsTrigger>
                </TabsList>
              </div>

              <TabsContent value="entry" className="px-6 pb-6 pt-4 mt-0">
                <h1 className="text-2xl font-bold text-gray-900 mb-2">{entry.title}</h1>
                <div className="flex items-center gap-3 text-sm text-gray-400 mb-6">
                  <span>{date}</span>
                  {collectionName && (
                    <Badge className="bg-orange-50 text-orange-700 border-orange-200">
                      {collectionName}
                    </Badge>
                  )}
                </div>
                <div
                  className="prose prose-sm max-w-none prose-headings:text-gray-900 prose-p:text-gray-700"
                  dangerouslySetInnerHTML={{ __html: entry.content || "<p>No content</p>" }}
                />
              </TabsContent>

              <TabsContent value="analysis" className="px-6 pb-6 pt-4 mt-0">
                {entry.sentimentLabel ? (
                  <div className="max-w-lg mx-auto text-center space-y-6 py-4">
                    {/* Mood Score — large visual */}
                    {moodScore && (
                      <div>
                        <div className="relative inline-flex items-center justify-center w-24 h-24 rounded-full bg-gradient-to-br from-orange-100 to-amber-50 border-4 border-orange-200 shadow-sm">
                          <span className="text-2xl font-bold text-orange-700">{moodScore}</span>
                        </div>
                        <p className="text-sm text-gray-500 mt-2">Mood Score</p>
                      </div>
                    )}

                    {/* Sentiment Label */}
                    <div className="flex items-center justify-center gap-2">
                      <span className="text-xs font-medium uppercase tracking-wider text-gray-400">Sentiment</span>
                      <Badge className="bg-orange-50 text-orange-700 border-orange-200 text-sm px-3 py-1 capitalize">
                        {entry.sentimentLabel}
                      </Badge>
                    </div>

                    {/* Emotions */}
                    {entry.emotions?.length > 0 && (
                      <div>
                        <span className="text-xs font-medium uppercase tracking-wider text-gray-400">Emotions Detected</span>
                        <div className="flex flex-wrap justify-center gap-2 mt-3">
                          {entry.emotions.map((e) => (
                            <Badge
                              key={e}
                              className="bg-gradient-to-r from-orange-50 to-amber-50 text-orange-700 border border-orange-200 px-3 py-1 text-sm capitalize shadow-sm"
                            >
                              {e}
                            </Badge>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                ) : (
                  <div className="text-center py-10 text-gray-400">
                    <p className="text-sm">No sentiment analysis available for this entry.</p>
                  </div>
                )}
              </TabsContent>
            </Tabs>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
