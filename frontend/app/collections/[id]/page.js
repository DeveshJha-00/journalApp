"use client";

import { useAuth } from "@/lib/auth";
import { useRouter, useParams } from "next/navigation";
import { useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { collectionAPI } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { ArrowLeft } from "lucide-react";
import Link from "next/link";
import EntryCard from "@/components/EntryCard";

export default function CollectionDetailPage() {
  const { isAuthenticated, isLoading: authLoading } = useAuth();
  const router = useRouter();
  const params = useParams();
  const collectionId = params.id;

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
        <div>
          <h1 className="text-4xl font-bold text-orange-600">
            {collection?.name || "Collection"}
          </h1>
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
            <EntryCard key={entry.id} entry={entry} showCollection={false} />
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


