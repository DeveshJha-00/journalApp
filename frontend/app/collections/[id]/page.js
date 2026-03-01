"use client";

import { useAuth } from "@/lib/auth";
import { useRouter, useParams } from "next/navigation";
import { useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { collectionAPI } from "@/lib/api";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Badge } from "@/components/ui/badge";
import { ArrowLeft } from "lucide-react";
import Link from "next/link";

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
    <div className="container mx-auto px-4 py-8 max-w-4xl">
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
          <h1 className="text-2xl font-bold text-gray-900">
            {collection?.name || "Collection"}
          </h1>
          {collection?.description && (
            <p className="text-sm text-gray-500">{collection.description}</p>
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
        <div className="space-y-4">
          {entries.map((entry) => (
            <EntryCard key={entry.id} entry={entry} />
          ))}
        </div>
      ) : (
        <div className="text-center py-16 text-gray-400">
          <p className="mb-4">No entries in this collection yet.</p>
          <Link href="/journal/new">
            <Button className="bg-orange-600 hover:bg-orange-700 text-white rounded-xl">
              Write your first entry
            </Button>
          </Link>
        </div>
      )}
    </div>
  );
}

function EntryCard({ entry }) {
  const date = entry.date
    ? new Date(entry.date).toLocaleDateString("en-US", {
        month: "short",
        day: "numeric",
        year: "numeric",
      })
    : "";

  const snippet = entry.content
    ? entry.content.replace(/<[^>]*>/g, "").substring(0, 150)
    : "";

  return (
    <Link href={`/journal/${entry.id}`}>
      <Card className="rounded-2xl shadow-sm hover:shadow-md transition-shadow bg-white/80 backdrop-blur-sm cursor-pointer">
        <CardContent className="p-5">
          <div className="flex items-start justify-between mb-2">
            <h3 className="font-semibold text-gray-900">{entry.title}</h3>
            <span className="text-xs text-gray-400 whitespace-nowrap ml-3">{date}</span>
          </div>
          {snippet && (
            <p className="text-sm text-gray-500 line-clamp-2 mb-3">{snippet}...</p>
          )}
          <div className="flex items-center gap-2 flex-wrap">
            {entry.sentimentLabel && (
              <Badge variant="secondary" className="text-xs">
                {entry.sentimentLabel}
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
