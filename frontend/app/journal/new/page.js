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

import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { toast } from "sonner";
import { Save, Send, Plus, ChevronDown } from "lucide-react";
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
    <div className="container mx-auto px-4 py-8 max-w-8xl">
      <h1 className="text-4xl font-bold text-orange-600 mb-6">What's on your mind?</h1>

      <div className="space-y-4">
        {/* Title */}
        <Input
          placeholder="Entry title..."
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          className="rounded-xl text-lg font-medium h-12"
        />

        {/* Collection Selector */}
        <div className="flex items-center gap-2">

          <div className="flex-1">
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button
                    variant="outline"
                    className={`w-full h-10 justify-between bg-transparent border-gray-200 px-3 font-medium ${
                      collectionId ? "text-gray-900" : "text-gray-500"
                    }`}
                  >
                    {collectionId
                      ? collections?.find((c) => c.id === collectionId)?.name || "Collection"
                      : "No collection"}

                    <ChevronDown className="h-4 w-4" />
                  </Button>
                </DropdownMenuTrigger>

                <DropdownMenuContent
                  align="start"
                  className="w-[var(--radix-dropdown-menu-trigger-width)] max-h-60 overflow-y-auto bg-white/80 backdrop-blur-sm border border-gray-200"
                >
                  <DropdownMenuItem onClick={() => setCollectionId("")}>
                    No collection
                  </DropdownMenuItem>

                  {collections?.map((col) => (
                    <DropdownMenuItem
                      key={col.id}
                      onClick={() => setCollectionId(col.id)}
                    >
                      {col.name}
                    </DropdownMenuItem>
                  ))}
                </DropdownMenuContent>
              </DropdownMenu>
            </div>

          <InlineCreateCollection />
        </div>

        {/* TipTap Editor */}
        <div className="backdrop-blur-sm rounded-sm border border-gray-200 overflow-hidden min-h-[400px]">
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
    <Dialog open={open} onOpenChange={setOpen} className="bg-transparent">
      <DialogTrigger asChild>
        <Button variant="outline" size="icon" className="cursor-pointer rounded-md h-10 w-10 border-orange-200 hover:bg-orange-50">
          <Plus className="h-4 w-4 text-orange-600" />
        </Button>
      </DialogTrigger>
      <DialogContent className="bg-white/70 backdrop-blur-smborder border-gray-200">
        <DialogHeader className="mb-5">
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
          />
          <div className="flex justify-end gap-2">
            <DialogClose asChild>
              <Button variant="ghost" className="cursor-pointer">Cancel</Button>
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
