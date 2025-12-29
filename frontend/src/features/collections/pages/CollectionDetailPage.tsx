"use client";

import { Search } from "lucide-react";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import type {
  Collection,
  CollectionModule,
  Item,
  ModuleDetails,
  CollectionInvite,
} from "../../../api/types";
import { useToast } from "../../../components/Toasts";
import EmptyState from "../../../components/EmptyState";
import ErrorState from "../../../components/ErrorState";
import LoadingState from "../../../components/LoadingState";
import CollectionHeader from "../components/CollectionHeader";
import ModuleSelector from "../components/ModuleSelector";
import CollectionActionsMenu from "../components/CollectionActionsMenu";
import { getCollection, listCollectionModules } from "../api";
import ItemsList from "../../items/components/ItemsList";
import { changeItemState, deleteItem, listItems } from "../../items/api";
import AddItemDialog from "../../items/components/AddItemDialog/AddItemDialog";
import { getModuleDetails } from "../../modules/api";
import CollectionSettingsDialog from "../components/CollectionSettingsDialog/CollectionSettingsDialog";
import { useCollectionModules } from "../hooks/useCollectionModules";
import { useCollectionMembers } from "../hooks/useCollectionMembers";
import {
  createCollectionInvite,
  listCollectionInvites,
  revokeCollectionInvite,
} from "../api/collectionInvitesApi";
import { useAuth } from "../../../auth/useAuth";
import StatsPanel from "../components/StatsPanel";
import { Input } from "../../../../components/ui/input";
import { Alert, AlertDescription } from "../../../../components/ui/alert";

export default function CollectionDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { showToast } = useToast();

  const [collection, setCollection] = useState<Collection | null>(null);
  const [modules, setModules] = useState<CollectionModule[]>([]);
  const [activeModuleKey, setActiveModuleKey] = useState<string | null>(null);
  const [items, setItems] = useState<Item[]>([]);
  const [itemCounts, setItemCounts] = useState<Record<string, number>>({});
  const [moduleDetails, setModuleDetails] = useState<ModuleDetails | null>(
    null
  );
  const [moduleItemsCache, setModuleItemsCache] = useState<
    Record<string, Item[]>
  >({});
  const [moduleDetailsCache, setModuleDetailsCache] = useState<
    Record<string, ModuleDetails>
  >({});

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [itemsLoading, setItemsLoading] = useState(false);
  const [itemsError, setItemsError] = useState<string | null>(null);
  const [moduleError, setModuleError] = useState<string | null>(null);
  const [addOpen, setAddOpen] = useState(false);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [invites, setInvites] = useState<CollectionInvite[]>([]);
  const [invitesLoaded, setInvitesLoaded] = useState(false);
  const prefetchedModules = useRef<Set<string>>(new Set());
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [batchBusy, setBatchBusy] = useState(false);
  const [search, setSearch] = useState("");
  const [stateFilter, setStateFilter] = useState<string | null>(null);
  const { user } = useAuth();

  const {
    availableModules,
    enabledModules,
    loading: modulesLoading,
    saving: modulesSaving,
    error: modulesError,
    refresh: refreshModules,
    enable: enableModule,
    disable: disableModule,
    setEnabledModules,
  } = useCollectionModules(id);

  const {
    members,
    loaded: membersLoaded,
    loading: membersLoading,
    saving: membersSaving,
    error: membersError,
    refresh: refreshMembers,
    changeRole,
    remove,
  } = useCollectionMembers(id, { auto: false });

  useEffect(() => {
    setInvites([]);
    setInvitesLoaded(false);
    setModuleItemsCache({});
    setModuleDetailsCache({});
    prefetchedModules.current = new Set();
  }, [id]);

  const loadCollection = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    setError(null);
    try {
      const [col, mods] = await Promise.all([
        getCollection(id),
        listCollectionModules(id),
      ]);
      setCollection(col);
      setModules(mods);
      setActiveModuleKey((prev) => prev || mods[0]?.moduleKey || null);
      setEnabledModules(mods);
    } catch (err: any) {
      setError(err?.message || "Failed to load collection");
    } finally {
      setLoading(false);
    }
  }, [id, setEnabledModules]);

  useEffect(() => {
    void loadCollection();
  }, [loadCollection]);

  useEffect(() => {
    if (enabledModules.length) {
      setModules(enabledModules);
    }
  }, [enabledModules]);

  const loadInvites = useCallback(
    async (force = false) => {
      if (!id) return;
      if (invitesLoaded && !force) return;
      try {
        const data = await listCollectionInvites(id);
        setInvites(data);
        setInvitesLoaded(true);
      } catch (err) {
        // ignore silently to keep settings usable
      }
    },
    [id, invitesLoaded]
  );

  const activeModule = useMemo(
    () => modules.find((m) => m.moduleKey === activeModuleKey) || modules[0],
    [activeModuleKey, modules]
  );

  const canAddItems = useMemo(() => {
    return (
      !!collection &&
      ["OWNER", "ADMIN", "EDITOR"].includes((collection as any).role)
    );
  }, [collection]);

  const defaultStateKey = moduleDetails?.contract?.states?.[0]?.key || "OWNED";

  const fetchModuleAndItems = async (
    moduleKey: string,
    forceRefresh = false
  ) => {
    if (!id) return;
    const module = modules.find((m) => m.moduleKey === moduleKey);
    if (!module) return;

    const cachedItems = moduleItemsCache[moduleKey];
    const cachedDetails = moduleDetailsCache[moduleKey];

    if (!forceRefresh && cachedItems) {
      setItems(cachedItems);
      setModuleDetails(cachedDetails ?? null);
      setModuleError(null);
      setItemsError(null);
      setItemsLoading(false);
      if (cachedDetails) return;
    }

    const detailsPromise = cachedDetails
      ? Promise.resolve(cachedDetails)
      : getModuleDetails(moduleKey);

    const shouldFetchItems = forceRefresh || !cachedItems;
    const itemsPromise = shouldFetchItems
      ? listItems(id, module.moduleId, { forceRefresh })
      : Promise.resolve(null);

    setModuleError(null);
    setItemsError(null);
    setItemsLoading(shouldFetchItems);

    try {
      const [details, page] = await Promise.all([detailsPromise, itemsPromise]);

      setModuleDetails(details as ModuleDetails);
      setModuleDetailsCache((prev) => ({
        ...prev,
        [module.moduleKey]: details as ModuleDetails,
      }));

      if (page) {
        const resolvedPage = page as Awaited<ReturnType<typeof listItems>>;
        setItems(resolvedPage.content);
        setModuleItemsCache((prev) => ({
          ...prev,
          [module.moduleKey]: resolvedPage.content,
        }));
        setItemCounts((prev) => ({
          ...prev,
          [module.moduleKey]:
            resolvedPage.totalElements ?? resolvedPage.content.length,
        }));
      }
    } catch (err: any) {
      const message = err?.message || "Failed to load module or items";
      setModuleError(message);
      setItemsError(message);
    } finally {
      setItemsLoading(false);
    }
  };

  useEffect(() => {
    setSelectedIds([]);
    setStateFilter(null);
  }, [activeModuleKey, items.length]);

  useEffect(() => {
    if (activeModuleKey && id) {
      void fetchModuleAndItems(activeModuleKey);
    }
  }, [activeModuleKey, id, modules]);

  const handleAddItem = () => {
    if (!moduleDetails) {
      showToast("Module is still loading", "warning");
      return;
    }
    setAddOpen(true);
  };

  const toggleItemSelection = (itemId: string, checked: boolean) => {
    setSelectedIds((prev) => {
      if (checked) {
        if (prev.includes(itemId)) return prev;
        return [...prev, itemId];
      }
      return prev.filter((idValue) => idValue !== itemId);
    });
  };

  const handleToggleAll = (ids: string[]) => setSelectedIds(ids);
  const clearSelection = () => setSelectedIds([]);

  const handleChangeState = async (item: Item, stateKey: string) => {
    if (!id) return;
    const snapshot = items;
    setItems((prev) => {
      const next = prev.map((i) => (i.id === item.id ? { ...i, stateKey } : i));
      if (activeModuleKey) {
        setModuleItemsCache((cache) => ({
          ...cache,
          [activeModuleKey]: next,
        }));
      }
      return next;
    });
    try {
      const updated = await changeItemState(id, item.id, stateKey);
      setItems((prev) => {
        const next = prev.map((i) => (i.id === updated.id ? updated : i));
        if (activeModuleKey) {
          setModuleItemsCache((cache) => ({
            ...cache,
            [activeModuleKey]: next,
          }));
        }
        return next;
      });
      showToast("State updated", "success");
    } catch (err: any) {
      setItems(snapshot);
      if (activeModuleKey) {
        setModuleItemsCache((cache) => ({
          ...cache,
          [activeModuleKey]: snapshot,
        }));
      }
      showToast(err?.message || "Failed to update state", "error");
    }
  };

  const handleItemCreated = (item: Item) => {
    setItems((prev) => {
      const next = [item, ...prev];
      if (activeModuleKey) {
        setModuleItemsCache((cache) => ({
          ...cache,
          [activeModuleKey]: next,
        }));
      }
      return next;
    });
    if (activeModuleKey) {
      setItemCounts((prev) => ({
        ...prev,
        [activeModuleKey]: (prev[activeModuleKey] || 0) + 1,
      }));
    }
    showToast("Item added", "success");
  };

  const handleOpenSettings = () => {
    setSettingsOpen(true);
    if (!membersLoaded) {
      void refreshMembers();
    }
    void loadInvites();
  };

  const handleCloseSettings = () => setSettingsOpen(false);

  const handleCreateInvite = async (role: string, expiresInDays?: number) => {
    if (!id) throw new Error("Missing collection id");
    const resp = await createCollectionInvite(id, {
      role: role as any,
      expiresInDays,
    });
    setInvites((prev) => [resp, ...prev]);
    showToast("Invite created", "success");
    return resp;
  };

  const handleChangeRole = async (userId: string, role: string) => {
    try {
      await changeRole(userId, role as any);
      showToast("Role updated", "success");
    } catch (err: any) {
      showToast(err?.message || "Failed to update role", "error");
    }
  };

  const handleBatchStateChange = async (stateKey: string) => {
    if (!id || !selectedIds.length) return;
    setBatchBusy(true);
    const failures: string[] = [];
    const successes: string[] = [];

    for (const itemId of selectedIds) {
      const original = items.find((i) => i.id === itemId);
      if (!original) {
        failures.push(itemId);
        continue;
      }

      setItems((prev) => {
        const next = prev.map((i) =>
          i.id === itemId ? { ...i, stateKey } : i
        );
        if (activeModuleKey) {
          setModuleItemsCache((cache) => ({
            ...cache,
            [activeModuleKey]: next,
          }));
        }
        return next;
      });
      try {
        const updated = await changeItemState(id, itemId, stateKey);
        setItems((prev) => {
          const next = prev.map((i) => (i.id === updated.id ? updated : i));
          if (activeModuleKey) {
            setModuleItemsCache((cache) => ({
              ...cache,
              [activeModuleKey]: next,
            }));
          }
          return next;
        });
        successes.push(itemId);
      } catch (err: any) {
        failures.push(itemId);
        setItems((prev) => {
          const next = prev.map((i) => (i.id === itemId ? original : i));
          if (activeModuleKey) {
            setModuleItemsCache((cache) => ({
              ...cache,
              [activeModuleKey]: next,
            }));
          }
          return next;
        });
      }
    }

    setSelectedIds(failures);
    setBatchBusy(false);

    if (failures.length && successes.length) {
      showToast(
        `Updated ${successes.length}, failed ${failures.length}`,
        "warning"
      );
    } else if (failures.length) {
      showToast(`Failed to update ${failures.length} item(s)`, "error");
    } else {
      showToast(`Updated ${successes.length} item(s)`, "success");
    }
  };

  const handleBatchDelete = async () => {
    if (!id || !selectedIds.length) return;
    setBatchBusy(true);
    const failures: string[] = [];
    const deleted: string[] = [];

    for (const itemId of selectedIds) {
      try {
        await deleteItem(id, itemId);
        deleted.push(itemId);
        setItems((prev) => {
          const next = prev.filter((i) => i.id !== itemId);
          if (activeModuleKey) {
            setModuleItemsCache((cache) => ({
              ...cache,
              [activeModuleKey]: next,
            }));
          }
          return next;
        });
        if (activeModuleKey) {
          setItemCounts((prev) => ({
            ...prev,
            [activeModuleKey]: Math.max(
              (prev[activeModuleKey] ?? items.length) - 1,
              0
            ),
          }));
        }
      } catch (err: any) {
        failures.push(itemId);
      }
    }

    setSelectedIds(failures);
    setBatchBusy(false);

    if (failures.length && deleted.length) {
      showToast(
        `Deleted ${deleted.length}, failed ${failures.length}`,
        "warning"
      );
    } else if (failures.length) {
      showToast(`Failed to delete ${failures.length} item(s)`, "error");
    } else {
      showToast(`Deleted ${deleted.length} item(s)`, "success");
    }
  };

  const handleRevokeInvite = (token: string) => {
    if (!id) return;
    void (async () => {
      try {
        await revokeCollectionInvite(id, token);
        setInvites((prev) => prev.filter((invite) => invite.token !== token));
        showToast("Invite revoked", "success");
      } catch (err: any) {
        showToast(err?.message || "Failed to revoke invite", "error");
      }
    })();
  };

  const handleRemoveMember = async (userId: string) => {
    try {
      await remove(userId);
      showToast("Member removed", "success");
    } catch (err: any) {
      showToast(err?.message || "Failed to remove member", "error");
    }
  };

  const filteredItems = useMemo(() => {
    let base = items;
    if (stateFilter) {
      base = base.filter((item) => item.stateKey === stateFilter);
    }
    if (!search.trim()) return base;
    const q = search.toLowerCase();
    return base.filter((item) => {
      const title =
        (item.attributes?.title as string) ||
        (item.attributes?.name as string) ||
        "";
      const identifiers = (item.identifiers || [])
        .map((id) => `${id.type} ${id.value}`)
        .join(" ");
      return [title, item.id, identifiers]
        .filter(Boolean)
        .some((val) => val.toLowerCase().includes(q));
    });
  }, [items, search, stateFilter]);

  const itemCountsByModule = useMemo(
    () => ({
      ...modules.reduce((acc, m) => ({ ...acc, [m.moduleKey]: 0 }), {}),
      ...itemCounts,
    }),
    [modules, itemCounts]
  );

  useEffect(() => {
    if (!id || !modules.length) return;
    let cancelled = false;

    const preload = async () => {
      for (const mod of modules) {
        if (prefetchedModules.current.has(mod.moduleKey)) continue;
        if (moduleItemsCache[mod.moduleKey]) {
          prefetchedModules.current.add(mod.moduleKey);
          continue;
        }
        try {
          const page = await listItems(id, mod.moduleId);
          if (cancelled) return;
          prefetchedModules.current.add(mod.moduleKey);
          setModuleItemsCache((prev) => ({
            ...prev,
            [mod.moduleKey]: page.content,
          }));
          setItemCounts((prev) => ({
            ...prev,
            [mod.moduleKey]: page.totalElements ?? page.content.length,
          }));
        } catch (err) {
          // ignore background preload errors
        }
      }
    };

    void preload();

    return () => {
      cancelled = true;
    };
  }, [id, modules, moduleItemsCache]);

  if (loading) return <LoadingState message="Loading collection..." />;
  if (error || !collection || !id)
    return (
      <ErrorState
        title="Could not load collection"
        message={error || "Collection not found"}
        onRetry={loadCollection}
      />
    );

  return (
    <div className="space-y-6">
      <CollectionHeader
        collection={collection}
        actions={
          <CollectionActionsMenu
            role={collection.role}
            onAddItem={canAddItems ? handleAddItem : undefined}
            onOpenSettings={handleOpenSettings}
          />
        }
      />

      {modules.length > 1 && (
        <ModuleSelector
          modules={modules}
          activeModuleKey={activeModuleKey}
          onChange={(key) => setActiveModuleKey(key)}
          itemCounts={itemCountsByModule}
        />
      )}
      {moduleError && (
        <Alert variant="destructive">
          <AlertDescription>{moduleError}</AlertDescription>
        </Alert>
      )}

      <StatsPanel
        items={items}
        states={moduleDetails?.contract?.states}
        activeState={stateFilter}
        onFilterChange={(stateKey) => setStateFilter(stateKey)}
      />

      {!modules.length ? (
        <EmptyState
          title="No modules enabled"
          description="Click the Collection Settings button to enable modules for this collection."
        />
      ) : (
        <div className="space-y-4">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
            <Input
              placeholder="Search items"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="pl-9"
            />
          </div>
          <ItemsList
            items={filteredItems}
            loading={itemsLoading}
            error={itemsError}
            moduleName={
              moduleDetails?.name ||
              activeModule?.name ||
              activeModule?.moduleKey
            }
            moduleDefinition={moduleDetails?.contract}
            canAdd={canAddItems}
            onAdd={canAddItems ? handleAddItem : undefined}
            onRetry={
              activeModuleKey
                ? () => fetchModuleAndItems(activeModuleKey, true)
                : undefined
            }
            role={collection.role}
            onChangeState={canAddItems ? handleChangeState : undefined}
            onItemClick={(item) =>
              navigate(`/collections/${id}/items/${item.id}`)
            }
            selectedIds={selectedIds}
            onToggleItem={toggleItemSelection}
            onToggleAll={handleToggleAll}
            onClearSelection={clearSelection}
            onBatchChangeState={handleBatchStateChange}
            onBatchDelete={handleBatchDelete}
            batchBusy={batchBusy}
          />
        </div>
      )}

      {id && moduleDetails && activeModule ? (
        <AddItemDialog
          open={addOpen}
          onClose={() => setAddOpen(false)}
          moduleDefinition={moduleDetails.contract}
          moduleId={activeModule.moduleId}
          collectionId={id}
          defaultState={defaultStateKey}
          onCreated={handleItemCreated}
        />
      ) : null}

      <CollectionSettingsDialog
        open={settingsOpen}
        onClose={handleCloseSettings}
        currentUserId={user?.id}
        availableModules={availableModules}
        enabledModules={enabledModules}
        members={members}
        invites={invites}
        loadingModules={modulesLoading}
        savingModules={modulesSaving}
        modulesError={modulesError}
        loadingMembers={membersLoading}
        savingMembers={membersSaving}
        membersError={membersError}
        onRefreshModules={() => void refreshModules()}
        onRefreshMembers={() => void refreshMembers()}
        onEnableModule={(moduleKey) =>
          enableModule(moduleKey).then(() => undefined)
        }
        onDisableModule={(moduleKey) =>
          disableModule(moduleKey).then(() => undefined)
        }
        onChangeRole={handleChangeRole}
        onRemoveMember={handleRemoveMember}
        onCreateInvite={(role, expiresInDays) =>
          handleCreateInvite(role, expiresInDays)
        }
        onRevokeInvite={handleRevokeInvite}
      />
    </div>
  );
}
