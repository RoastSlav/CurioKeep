"use client";

import { Plus, ExternalLink } from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";
import {
  clearCollectionsCache,
  createCollection,
  listCollections,
} from "../features/collections/api";
import type { Collection } from "../api/types";
import CollectionCard from "../components/CollectionCard";
import EmptyState from "../components/EmptyState";
import ErrorState from "../components/ErrorState";
import { useToast } from "../components/Toasts";
import StatCard from "../components/StatCard";
import { Button } from "../../components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "../../components/ui/dialog";
import { Input } from "../../components/ui/input";
import { Label } from "../../components/ui/label";
import { Textarea } from "../../components/ui/textarea";
import { Card, CardContent } from "../../components/ui/card";
import { Skeleton } from "../../components/ui/skeleton";

export default function DashboardPage() {
  const { showToast } = useToast();
  const [collections, setCollections] = useState<Collection[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const [createOpen, setCreateOpen] = useState<boolean>(false);
  const [createName, setCreateName] = useState<string>("");
  const [createDescription, setCreateDescription] = useState<string>("");
  const [creating, setCreating] = useState<boolean>(false);

  const loggedMissingStats = useRef<boolean>(false);
  const isFetchingRef = useRef<boolean>(false);

  const hasItemsCount = useMemo(
    () => collections.some((c) => c.itemsCount !== undefined),
    [collections]
  );

  const loadCollections = async (forceRefresh = false) => {
    if (isFetchingRef.current) return;
    isFetchingRef.current = true;
    setLoading(true);
    setError(null);
    try {
      const data = await listCollections({ forceRefresh });
      setCollections(data);
    } catch (err: any) {
      setError(err?.message || "Failed to load collections");
    } finally {
      isFetchingRef.current = false;
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadCollections();
  }, []);

  useEffect(() => {
    if (!hasItemsCount && !loggedMissingStats.current && !loading) {
      loggedMissingStats.current = true;
      console.info(
        "Item counts are not provided by the backend yet. Showing placeholders in the dashboard stats."
      );
    }
  }, [hasItemsCount, loading]);

  const collectionsCount = collections.length;
  const itemsCount = hasItemsCount
    ? collections.reduce((sum, c) => sum + (c.itemsCount ?? 0), 0)
    : undefined;

  const openCreateDialog = () => {
    setCreateOpen(true);
    setCreateName("");
    setCreateDescription("");
  };

  const handleCreate = async () => {
    if (!createName.trim()) {
      showToast("Collection name is required", "warning");
      return;
    }
    setCreating(true);
    try {
      const created = await createCollection({
        name: createName.trim(),
        description: createDescription.trim() || undefined,
      });
      setCollections((prev) => [created, ...prev]);
      clearCollectionsCache();
      setCreateOpen(false);
      showToast("Collection created", "success");
    } catch (err: any) {
      showToast(err?.message || "Failed to create collection", "error");
    } finally {
      setCreating(false);
    }
  };

  if (error) {
    return (
      <ErrorState
        title="Could not load dashboard"
        message={error}
        onRetry={() => loadCollections(true)}
      />
    );
  }

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row gap-6 sm:items-center justify-between">
        <div className="space-y-2">
          <h1 className="text-4xl md:text-5xl font-bold tracking-tight uppercase">
            Dashboard
          </h1>
          <p className="text-base font-medium">
            Overview of your collections and items
          </p>
        </div>
        <div className="flex flex-col sm:flex-row gap-4 w-full sm:w-auto">
          <Button
            onClick={openCreateDialog}
            className="w-full sm:w-auto min-w-[160px]"
          >
            <Plus className="w-4 h-4 mr-2" />
            Create collection
          </Button>
          <Button
            variant="outline"
            asChild
            className="w-full sm:w-auto min-w-[160px] bg-transparent"
          >
            <a
              href="https://github.com/RoastSlav/CurioKeep"
              target="_blank"
              rel="noreferrer"
            >
              Learn more
              <ExternalLink className="w-4 h-4 ml-2" />
            </a>
          </Button>
        </div>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
        <StatCard
          label="Collections"
          value={collectionsCount}
          loading={loading}
          variant="default"
        />
        <StatCard
          label="Total Items"
          value={itemsCount ?? "—"}
          loading={loading}
          hint={!hasItemsCount ? "Backend support needed" : undefined}
          variant="secondary"
        />
        <StatCard
          label="Quick Stats"
          value="—"
          loading={loading}
          hint="Backend support needed"
          variant="accent"
        />
      </div>

      {/* Collections Grid */}
      {loading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {[1, 2, 3].map((key) => (
            <Card key={key}>
              <CardContent className="pt-6">
                <Skeleton className="h-8 w-3/5 mb-2" />
                <Skeleton className="h-4 w-2/5 mb-2" />
                <Skeleton className="h-20 w-full mt-2" />
              </CardContent>
            </Card>
          ))}
        </div>
      ) : collections.length === 0 ? (
        <EmptyState
          title="No collections yet"
          description="Create your first collection to start organizing your items."
          actionLabel="Create collection"
          onAction={openCreateDialog}
          secondary={
            <Button variant="link" asChild>
              <a
                href="https://github.com/RoastSlav/CurioKeep"
                target="_blank"
                rel="noreferrer"
              >
                Learn about modules
                <ExternalLink className="w-4 h-4 ml-2" />
              </a>
            </Button>
          }
        />
      ) : (
        <div className="space-y-4">
          <h2 className="text-2xl font-bold uppercase tracking-wide">
            Your Collections
          </h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
            {collections.map((collection) => (
              <CollectionCard key={collection.id} collection={collection} />
            ))}
          </div>
        </div>
      )}

      {/* Create Dialog */}
      <Dialog
        open={createOpen}
        onOpenChange={(open) => !creating && setCreateOpen(open)}
      >
        <DialogContent className="sm:max-w-[500px]">
          <DialogHeader>
            <DialogTitle>Create collection</DialogTitle>
            <DialogDescription>
              Create a new collection to organize your items
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="create-name">Name *</Label>
              <Input
                id="create-name"
                value={createName}
                onChange={(e) => setCreateName(e.target.value)}
                placeholder="Enter collection name"
                autoFocus
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="create-description">Description</Label>
              <Textarea
                id="create-description"
                value={createDescription}
                onChange={(e) => setCreateDescription(e.target.value)}
                placeholder="Enter collection description"
                rows={3}
              />
            </div>
          </div>
          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setCreateOpen(false)}
              disabled={creating}
            >
              Cancel
            </Button>
            <Button
              onClick={handleCreate}
              disabled={creating}
              className="bg-secondary hover:bg-secondary-dark"
            >
              {creating ? "Creating..." : "Create"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
