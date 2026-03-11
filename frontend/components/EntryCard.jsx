"use client";

import Link from "next/link";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { X } from "lucide-react";

export default function EntryCard({ entry, collections, showCollection = true, onRemove }) {
  const date = entry.date
    ? new Date(entry.date).toLocaleDateString("en-US", {
        month: "short",
        day: "numeric",
        year: "numeric",
      })
    : "";

  let snippet = "";
  if (entry.content) {
    const plainText = entry.content
      .replace(/<\/p>/gi, "\n")
      .replace(/<br\s*\/?>/gi, "\n")
      .replace(/<[^>]*>/g, "");
    const firstLine = plainText
      .split(/\r?\n/)
      .find((line) => line.trim().length > 0) || "";

    snippet = firstLine.substring(0, 50);
  }

  const collectionName =
    entry.collectionName ||
    collections?.find((c) => c.id === entry.collectionId)?.name;

  return (
    <div className="relative">
      <Link href={`/journal/${entry.id}`}>
        <Card className="group relative overflow-hidden border border-gray-200 dark:border-gray-700 bg-gradient-to-br from-orange-50 to-white dark:from-gray-800 dark:to-gray-900 hover:shadow-lg transition-all duration-300 cursor-pointer shadow md">
          <CardContent className="p-6 relative">
            <div className="absolute left-0 top-0 h-full w-1 bg-gradient-to-b from-orange-400 to-orange-200 dark:from-orange-500 dark:to-orange-700 rounded-l-2xl" />
            <div className="flex items-start mb-2">
              <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 group-hover:text-orange-600 transition-colors">
                {entry.title}
              </h3>

              <span className="ml-auto text-sm text-gray-400 dark:text-gray-500 whitespace-nowrap">
                {date}
              </span>
            </div>
            {snippet && (
              <p className="text-sm text-gray-600 dark:text-gray-400 leading-relaxed mt-1 mb-4">{snippet}...</p>
            )}
            <div className="flex items-center gap-2 flex-wrap">
              {showCollection && collectionName && (
                <Badge className="bg-orange-100/60 dark:bg-orange-900/40 text-orange-700 dark:text-orange-300 border border-orange-200 dark:border-orange-800 text-xs px-2 py-0.5">
                  {collectionName}
                </Badge>
              )}
            </div>
          </CardContent>
        </Card>
      </Link>
      {onRemove && (
        <Button
          variant="ghost"
          size="icon"
          onClick={(e) => {
            e.preventDefault();
            e.stopPropagation();
            onRemove(entry.id);
          }}
          className="absolute top-2 right-2 h-7 w-7 text-gray-400 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-950 rounded-full z-10"
          title="Remove from collection"
        >
          <X className="h-3.5 w-3.5" />
        </Button>
      )}
    </div>
  );
}
