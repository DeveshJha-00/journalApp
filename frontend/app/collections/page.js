"use client";

import { useAuth } from "@/lib/auth";
import { useRouter, useSearchParams } from "next/navigation";
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
import { Plus, Pencil, Trash2, FolderOpen, Search, CalendarIcon, X, ArrowRight } from "lucide-react";
import Link from "next/link";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { Calendar } from "@/components/ui/calendar";
import { format } from "date-fns";

export default function CollectionsPage() {
  const { isAuthenticated, isLoading: authLoading } = useAuth();
  const router = useRouter();
  const queryClient = useQueryClient();
  const searchParams = useSearchParams();

  const [keyword, setKeyword] = useState(searchParams.get("q") || "");
  const [collectionFilter, setCollectionFilter] = useState(searchParams.get("collection") || "");
  const [dateFilter, setDateFilter] = useState(null);
  const [calendarOpen, setCalendarOpen] = useState(false);

  useEffect(() => {
    if (!authLoading && !isAuthenticated) router.push("/auth");
  }, [isAuthenticated, authLoading, router]);

  const { data: collections, isLoading } = useQuery({
    queryKey: ["collections"],
    queryFn: () => collectionAPI.getAll().then((r) => r.data),
    enabled: isAuthenticated,
  });

  // Client-side filtering
  const filtered = collections?.filter((collection) => {
    if (keyword) {
      const q = keyword.toLowerCase();
      const matchTitle = collection.name?.toLowerCase().includes(q);
      if (!matchTitle) return false;
    }
    if (collectionFilter) {
      if (collection.id !== collectionFilter) return false;
    }
    if (dateFilter) {
      const entryDate = collection.createdDate ? new Date(collection.createdDate).toDateString() : null;
      if (entryDate !== dateFilter.toDateString()) return false;
    }
    return true;
  });

  const hasFilters = collectionFilter || dateFilter;

  const clearFilters = () => {
    setCollectionFilter("");
    setDateFilter(null);
  };

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
    <div className="container mx-auto px-4 py-8 max-w-7xl">
      <div className="flex items-center justify-between mb-8">
        <h1 className="text-4xl font-bold text-orange-600">My Collections</h1>
      </div>

      {/* Filters */}
      <div className="flex flex-wrap items-center gap-3 mb-6">

        {/* Search */}
        <div className="relative flex-1 min-w-[200px]">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
            <Input
              placeholder="Search Collections..."
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

            {hasFilters && (
            <Button variant="ghost" size="sm" onClick={clearFilters} className="text-gray-500 hover:text-red-600 rounded-xl">
              <X className="h-4 w-4 mr-1" /> Clear
            </Button>
          )}
      </div>

      {/* Collections List */}
      {isLoading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {[1, 2, 3].map((i) => (
            <Skeleton key={i} className="h-36 rounded-2xl" />
          ))}
        </div>
      ) : filtered?.length > 0 ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {filtered.map((collection) => (
            <CollectionCard 
            key={collection.id} 
            collection={collection} 
            onDelete={() => deleteMutation.mutate(collection.id)} />
          ))}
          <CreateCollectionCard />
        </div>
      ) :(
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          <CreateCollectionCard />
        </div>
      )}
    </div>
  );
}

function CollectionCard({ collection, onDelete }) {
  const router = useRouter();

  const date = collection.createdDate
    ? new Date(collection.createdDate).toLocaleDateString("en-US", {
        month: "short",
        day: "numeric",
        year: "numeric",
      })
    : "";

  return (
    <div
      onClick={() => router.push(`/collections/${collection.id}`)}
      className="block relative group transition-transform duration-300 hover:-translate-y-1 cursor-pointer"
    >
      {/* Stack Layer 2 */}
      <div className="absolute inset-0 translate-x-2 translate-y-2 rounded-sm bg-orange-100/40 border border-orange-100 transition-all duration-300 group-hover:translate-x-3 group-hover:translate-y-3" />

      {/* Stack Layer 1 */}
      <div className="absolute inset-0 translate-x-1 translate-y-1 bg-white rounded-sm border border-gray-200 shadow-sm transition-all duration-300 group-hover:translate-x-2 group-hover:translate-y-2" />

      {/* /* Main Card */} 
        <Card className="relative overflow-hidden border border-gray-200 rounded-sm bg-gradient-to-br from-orange-100 via-orange-50 to-white hover:shadow-xl transition-all duration-300 cursor-pointer">
          
          {/* Left gradient stripe */}
        <div className="absolute left-0 top-0 h-full w-1 bg-gradient-to-b from-orange-400 to-orange-200 rounded-l-2xl" />

        <CardContent className="p-6 relative">
          
          {/* Header */}
          <div className="flex items-start mb-2">
            <h3 className="text-lg font-semibold text-gray-900 group-hover:text-orange-600 transition-colors">
              {collection.name}
            </h3>

            {date && (
              <span className="ml-auto text-sm text-gray-400 whitespace-nowrap">
                {date}
              </span>
            )}
          </div>

          {/* Description */}
          {collection.description && (
            <p className="text-sm text-gray-600 leading-relaxed mt-1 mb-4 line-clamp-2">
              {collection.description}
            </p>
          )}

          {/* Footer */}
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-1 text-orange-600 group-hover:text-orange-700 transition-colors text-sm font-medium">
              <span>View entries</span>
              <ArrowRight className="h-4 w-4 transition-transform duration-200 group-hover:translate-x-1" />
            </div>

            <AlertDialog>
              <AlertDialogTrigger asChild>
                <button
                  onClick={(e) => e.stopPropagation()}
                  className="p-1.5 rounded-md text-gray-400 hover:text-red-600 hover:bg-red-50 transition-colors z-10 cursor-pointer"
                  title="Delete collection"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </AlertDialogTrigger>
              <AlertDialogContent className="bg-white/80 backdrop-blur-sm border border-gray-200" onClick={(e) => e.stopPropagation()}>
                <AlertDialogHeader>
                  <AlertDialogTitle>Delete &quot;{collection.name}&quot;?</AlertDialogTitle>
                  <AlertDialogDescription>
                    This will delete the collection. All entries in this collection will be kept but unassigned.
                  </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                  <AlertDialogCancel>Cancel</AlertDialogCancel>
                  <AlertDialogAction
                    onClick={(e) => { e.stopPropagation(); onDelete(); }}
                    className="bg-red-600 hover:bg-red-700 text-white"
                  >
                    Delete
                  </AlertDialogAction>
                </AlertDialogFooter>
              </AlertDialogContent>
            </AlertDialog>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

function CreateCollectionCard() {
  const [open, setOpen] = useState(false);
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const queryClient = useQueryClient();

  const createMutation = useMutation({
    mutationFn: (data) => collectionAPI.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["collections"] });
      toast.success("Collection created!");
      setOpen(false);
      setName("");
      setDescription("");
    },
    onError: () => toast.error("Failed to create collection"),
  });

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!name.trim()) return;
    createMutation.mutate({ name, description });
  };

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <button className="cursor-pointer rounded-sm border-2 border-dashed border-orange-200 hover:border-orange-400 bg-white/50 hover:bg-orange-50/50 transition-all p-5 flex flex-col items-center justify-center gap-2 min-h-[140px]">
          <Plus className="h-8 w-8 text-orange-400" />
          <span className="text-sm font-medium text-orange-600">New Collection</span>
        </button>
      </DialogTrigger>
      <DialogContent className="bg-white/70 backdrop-blur-smborder border-gray-200">
        <DialogHeader>
          <DialogTitle>Create Collection</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4">
          <Input
            placeholder="Collection name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
            className="mt-4"
          />
          <Textarea
            placeholder="Description (optional)"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            className="resize-none"
            rows={2}
          />
          <div className="flex justify-end gap-2">
            <DialogClose asChild>
              <Button variant="ghost" className="hover:bg-orange-100">Cancel</Button>
            </DialogClose>
            <Button
              type="submit"
              disabled={createMutation.isPending}
              className="bg-orange-600 hover:bg-orange-700 text-white"
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
    updateMutation.mutate({ name, description });
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
