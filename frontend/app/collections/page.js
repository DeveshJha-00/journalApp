"use client";

import { useAuth } from "@/lib/auth";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { collectionAPI } from "@/lib/api";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
  DialogClose,
} from "@/components/ui/dialog";
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
import { toast } from "sonner";
import { Plus, Pencil, Trash2, FolderOpen } from "lucide-react";
import Link from "next/link";

const COLORS = [
  "#ea580c", "#dc2626", "#2563eb", "#16a34a", "#9333ea",
  "#ca8a04", "#0891b2", "#e11d48", "#4f46e5", "#059669",
];

export default function CollectionsPage() {
  const { isAuthenticated, isLoading: authLoading } = useAuth();
  const router = useRouter();
  const queryClient = useQueryClient();

  useEffect(() => {
    if (!authLoading && !isAuthenticated) router.push("/auth");
  }, [isAuthenticated, authLoading, router]);

  const { data: collections, isLoading } = useQuery({
    queryKey: ["collections"],
    queryFn: () => collectionAPI.getAll().then((r) => r.data),
    enabled: isAuthenticated,
  });

  const deleteMutation = useMutation({
    mutationFn: (id) => collectionAPI.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["collections"] });
      toast.success("Collection deleted");
    },
    onError: () => toast.error("Failed to delete collection"),
  });

  if (authLoading || !isAuthenticated) return null;

  return (
    <div className="container mx-auto px-4 py-8 max-w-5xl">
      <div className="flex items-center justify-between mb-8">
        <h1 className="text-2xl font-bold text-gray-900">Collections</h1>
      </div>

      {isLoading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {[1, 2, 3].map((i) => (
            <Skeleton key={i} className="h-36 rounded-2xl" />
          ))}
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {collections?.map((col) => (
            <CollectionCard
              key={col.id}
              collection={col}
              onDelete={() => deleteMutation.mutate(col.id)}
            />
          ))}
          <CreateCollectionCard />
        </div>
      )}
    </div>
  );
}

function CollectionCard({ collection, onDelete }) {
  const accentColor = collection.color || "#ea580c";

  return (
    <Card className="rounded-2xl shadow-md bg-white/80 backdrop-blur-sm hover:shadow-lg transition-shadow group relative overflow-hidden">
      <div
        className="absolute top-0 left-0 w-full h-1"
        style={{ backgroundColor: accentColor }}
      />
      <CardContent className="p-5 pt-6">
        <div className="flex items-start justify-between mb-3">
          <Link href={`/collections/${collection.id}`} className="flex-1">
            <h3 className="font-semibold text-gray-900 text-lg hover:text-orange-600 transition-colors">
              {collection.name}
            </h3>
          </Link>
          <div className="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
            <EditCollectionDialog collection={collection} />
            <AlertDialog>
              <AlertDialogTrigger asChild>
                <Button variant="ghost" size="icon" className="h-7 w-7 text-gray-400 hover:text-red-600">
                  <Trash2 className="h-3.5 w-3.5" />
                </Button>
              </AlertDialogTrigger>
              <AlertDialogContent className="rounded-2xl">
                <AlertDialogHeader>
                  <AlertDialogTitle>Delete collection?</AlertDialogTitle>
                  <AlertDialogDescription>
                    This will delete the collection &ldquo;{collection.name}&rdquo;. Entries in this collection will not be deleted.
                  </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                  <AlertDialogCancel className="rounded-xl">Cancel</AlertDialogCancel>
                  <AlertDialogAction
                    onClick={onDelete}
                    className="bg-red-600 hover:bg-red-700 rounded-xl"
                  >
                    Delete
                  </AlertDialogAction>
                </AlertDialogFooter>
              </AlertDialogContent>
            </AlertDialog>
          </div>
        </div>
        {collection.description && (
          <p className="text-sm text-gray-500 line-clamp-2 mb-3">{collection.description}</p>
        )}
        <Link href={`/collections/${collection.id}`}>
          <Button variant="ghost" size="sm" className="text-orange-600 hover:text-orange-700 hover:bg-orange-50 p-0 h-auto text-xs">
            View entries →
          </Button>
        </Link>
      </CardContent>
    </Card>
  );
}

function CreateCollectionCard() {
  const [open, setOpen] = useState(false);
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [color, setColor] = useState(COLORS[0]);
  const queryClient = useQueryClient();

  const createMutation = useMutation({
    mutationFn: (data) => collectionAPI.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["collections"] });
      toast.success("Collection created!");
      setOpen(false);
      setName("");
      setDescription("");
      setColor(COLORS[0]);
    },
    onError: () => toast.error("Failed to create collection"),
  });

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!name.trim()) return;
    createMutation.mutate({ name, description, color });
  };

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <button className="rounded-2xl border-2 border-dashed border-orange-200 hover:border-orange-400 bg-white/50 hover:bg-orange-50/50 transition-all p-5 flex flex-col items-center justify-center gap-2 min-h-[140px]">
          <Plus className="h-8 w-8 text-orange-400" />
          <span className="text-sm font-medium text-orange-600">New Collection</span>
        </button>
      </DialogTrigger>
      <DialogContent className="rounded-2xl">
        <DialogHeader>
          <DialogTitle>Create Collection</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4">
          <Input
            placeholder="Collection name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
            className="rounded-xl"
          />
          <Textarea
            placeholder="Description (optional)"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            className="rounded-xl resize-none"
            rows={2}
          />
          <div>
            <p className="text-sm text-gray-500 mb-2">Color</p>
            <div className="flex gap-2 flex-wrap">
              {COLORS.map((c) => (
                <button
                  key={c}
                  type="button"
                  onClick={() => setColor(c)}
                  className={`w-7 h-7 rounded-full border-2 transition-all ${
                    color === c ? "border-gray-900 scale-110" : "border-transparent"
                  }`}
                  style={{ backgroundColor: c }}
                />
              ))}
            </div>
          </div>
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

function EditCollectionDialog({ collection }) {
  const [open, setOpen] = useState(false);
  const [name, setName] = useState(collection.name);
  const [description, setDescription] = useState(collection.description || "");
  const [color, setColor] = useState(collection.color || COLORS[0]);
  const queryClient = useQueryClient();

  const updateMutation = useMutation({
    mutationFn: (data) => collectionAPI.update(collection.id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["collections"] });
      toast.success("Collection updated");
      setOpen(false);
    },
    onError: () => toast.error("Failed to update collection"),
  });

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!name.trim()) return;
    updateMutation.mutate({ name, description, color });
  };

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button variant="ghost" size="icon" className="h-7 w-7 text-gray-400 hover:text-orange-600">
          <Pencil className="h-3.5 w-3.5" />
        </Button>
      </DialogTrigger>
      <DialogContent className="rounded-2xl">
        <DialogHeader>
          <DialogTitle>Edit Collection</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4">
          <Input
            placeholder="Collection name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
            className="rounded-xl"
          />
          <Textarea
            placeholder="Description (optional)"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            className="rounded-xl resize-none"
            rows={2}
          />
          <div>
            <p className="text-sm text-gray-500 mb-2">Color</p>
            <div className="flex gap-2 flex-wrap">
              {COLORS.map((c) => (
                <button
                  key={c}
                  type="button"
                  onClick={() => setColor(c)}
                  className={`w-7 h-7 rounded-full border-2 transition-all ${
                    color === c ? "border-gray-900 scale-110" : "border-transparent"
                  }`}
                  style={{ backgroundColor: c }}
                />
              ))}
            </div>
          </div>
          <div className="flex justify-end gap-2">
            <DialogClose asChild>
              <Button variant="ghost" className="rounded-xl">Cancel</Button>
            </DialogClose>
            <Button
              type="submit"
              disabled={updateMutation.isPending}
              className="bg-orange-600 hover:bg-orange-700 text-white rounded-xl"
            >
              Save
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
