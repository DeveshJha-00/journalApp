"use client";

import { useAuth } from "@/lib/auth";
import { useRouter } from "next/navigation";
import { useEffect, useState, useCallback, useRef } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { journalAPI, collectionAPI } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
  DialogClose,
} from "@/components/ui/dialog";
import { toast } from "sonner";
import { Save, Send, Plus } from "lucide-react";
import TipTapEditor from "@/components/TipTapEditor";

const DRAFT_KEY = "journal-draft";

export default function NewJournalPage() {
  const { isAuthenticated, isLoading: authLoading } = useAuth();
  const router = useRouter();
  const queryClient = useQueryClient();

  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [collectionId, setCollectionId] = useState("");
  const [saving, setSaving] = useState(false);
  const lastSavedRef = useRef(null);

  useEffect(() => {
    if (!authLoading && !isAuthenticated) router.push("/auth");
  }, [isAuthenticated, authLoading, router]);

  // Load draft on mount
  useEffect(() => {
    try {
      const draft = localStorage.getItem(DRAFT_KEY);
      if (draft) {
        const parsed = JSON.parse(draft);
        setTitle(parsed.title || "");
        setContent(parsed.content || "");
        setCollectionId(parsed.collectionId || "");
      }
    } catch {}
  }, []);

  // Auto-save draft every 5s
  useEffect(() => {
    const interval = setInterval(() => {
      if (title || content) {
        const draft = JSON.stringify({ title, content, collectionId });
        if (draft !== lastSavedRef.current) {
          localStorage.setItem(DRAFT_KEY, draft);
          lastSavedRef.current = draft;
        }
      }
    }, 5000);
    return () => clearInterval(interval);
  }, [title, content, collectionId]);

  const { data: collections } = useQuery({
    queryKey: ["collections"],
    queryFn: () => collectionAPI.getAll().then((r) => r.data),
    enabled: isAuthenticated,
  });

  const publishMutation = useMutation({
    mutationFn: (data) => journalAPI.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["journals"] });
      queryClient.invalidateQueries({ queryKey: ["analytics"] });
      localStorage.removeItem(DRAFT_KEY);
      toast.success("Entry published!");
      router.push("/journal");
    },
    onError: () => toast.error("Failed to publish entry"),
  });

  const handleSaveDraft = () => {
    localStorage.setItem(DRAFT_KEY, JSON.stringify({ title, content, collectionId }));
    toast.success("Draft saved!");
  };

  const handlePublish = () => {
    if (!title.trim()) {
      toast.error("Title is required");
      return;
    }
    publishMutation.mutate({
      title: title.trim(),
      content,
      collectionId: collectionId || null,
    });
  };

  if (authLoading || !isAuthenticated) return null;

  return (
    <div className="container mx-auto px-4 py-8 max-w-3xl">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">New Entry</h1>

      <div className="space-y-4">
        {/* Title */}
        <Input
          placeholder="Entry title..."
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          className="rounded-xl text-lg font-medium h-12 bg-white/80"
        />

        {/* Collection Selector */}
        <div className="flex items-center gap-2">
          <select
            value={collectionId}
            onChange={(e) => setCollectionId(e.target.value)}
            className="flex-1 h-10 rounded-xl border border-gray-200 bg-white/80 px-3 text-sm text-gray-700 focus:outline-none focus:ring-2 focus:ring-orange-500"
          >
            <option value="">No collection</option>
            {collections?.map((col) => (
              <option key={col.id} value={col.id}>
                {col.name}
              </option>
            ))}
          </select>
          <InlineCreateCollection />
        </div>

        {/* TipTap Editor */}
        <div className="bg-white/80 backdrop-blur-sm rounded-2xl border border-gray-200 overflow-hidden min-h-[400px]">
          <TipTapEditor content={content} onChange={setContent} />
        </div>

        {/* Action Buttons */}
        <div className="flex justify-end gap-3 pt-2">
          <Button
            variant="outline"
            onClick={handleSaveDraft}
            className="rounded-xl border-orange-200 hover:bg-orange-50"
          >
            <Save className="h-4 w-4 mr-1.5" /> Save Draft
          </Button>
          <Button
            onClick={handlePublish}
            disabled={publishMutation.isPending}
            className="bg-orange-600 hover:bg-orange-700 text-white rounded-xl"
          >
            <Send className="h-4 w-4 mr-1.5" />
            {publishMutation.isPending ? "Publishing..." : "Publish"}
          </Button>
        </div>
      </div>
    </div>
  );
}

function InlineCreateCollection() {
  const [open, setOpen] = useState(false);
  const [name, setName] = useState("");
  const queryClient = useQueryClient();

  const createMutation = useMutation({
    mutationFn: (data) => collectionAPI.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["collections"] });
      toast.success("Collection created!");
      setOpen(false);
      setName("");
    },
    onError: () => toast.error("Failed to create collection"),
  });

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button variant="outline" size="icon" className="rounded-xl h-10 w-10 border-orange-200 hover:bg-orange-50">
          <Plus className="h-4 w-4 text-orange-600" />
        </Button>
      </DialogTrigger>
      <DialogContent className="rounded-2xl">
        <DialogHeader>
          <DialogTitle>Quick Create Collection</DialogTitle>
        </DialogHeader>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            if (name.trim()) createMutation.mutate({ name: name.trim() });
          }}
          className="space-y-4"
        >
          <Input
            placeholder="Collection name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
            className="rounded-xl"
          />
          <div className="flex justify-end gap-2">
            <DialogClose asChild>
              <Button variant="ghost" className="rounded-xl">Cancel</Button>
            </DialogClose>
            <Button
              type="submit"
              disabled={createMutation.isPending}
              className="bg-orange-600 hover:bg-orange-700 text-white rounded-xl"
            >
              Create
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
