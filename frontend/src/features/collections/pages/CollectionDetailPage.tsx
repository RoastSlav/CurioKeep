import { getErrorMessage } from "@/api/errors";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import type { Collection, CollectionModule, Item, CollectionInvite } from "../../../api/types";
import { useToast } from "../../../components/toastContext";
import EmptyState from "../../../components/EmptyState";
import ErrorState from "../../../components/ErrorState";
import LoadingState from "../../../components/LoadingState";
import CollectionHeader from "../components/CollectionHeader";
import ModuleSelector from "../components/ModuleSelector";
import CollectionActionsMenu from "../components/CollectionActionsMenu";
import { getCollection, listCollectionModules } from "../api/collectionsApi";
import ItemsList from "../../items/components/ItemsList";
import { ItemFiltersDialog } from "../../items/components/ItemFiltersDialog";
import { ItemsPagination } from "../../items/components/ItemsPagination";
import { ItemsToolbar } from "../../items/components/ItemsToolbar";
import { changeItemState, deleteItem } from "../../items/api";
import { countActiveFilters, filterableFields } from "../../items/itemFilters";
import { sortOptions } from "../../items/itemSort";
import { useItemCounts } from "../../items/hooks/useItemCounts";
import { useItemList } from "../../items/hooks/useItemList";
import AddItemDialog from "../../items/components/AddItemDialog/AddItemDialog";
import { useModuleDetails } from "../../modules/hooks/useModuleDetails";
import CollectionSettingsDialog from "../components/CollectionSettingsDialog/CollectionSettingsDialog";
import { useCollectionModules } from "../hooks/useCollectionModules";
import { useCollectionMembers } from "../hooks/useCollectionMembers";
import {
  createCollectionInvite,
  listCollectionInvites,
  revokeCollectionInvite,
} from "../api/collectionInvitesApi";
import { useAuth } from "../../../auth/useAuth";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import StatsPanel from "../components/StatsPanel";
import { Alert, AlertDescription } from "@/components/ui/alert";

const SEARCH_DEBOUNCE_MS = 300;

export default function CollectionDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { showToast } = useToast();

  const [collection, setCollection] = useState<Collection | null>(null);
  const [modules, setModules] = useState<CollectionModule[]>([]);
  const [activeModuleKey, setActiveModuleKey] = useState<string | null>(null);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [addOpen, setAddOpen] = useState(false);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [filtersOpen, setFiltersOpen] = useState(false);
  const [invites, setInvites] = useState<CollectionInvite[]>([]);
  const [invitesLoaded, setInvitesLoaded] = useState(false);
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [batchBusy, setBatchBusy] = useState(false);
  const [searchInput, setSearchInput] = useState("");
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
    } catch (err) {
      setError(getErrorMessage(err, "Failed to load collection"));
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
      } catch {
        // ignore silently to keep settings usable
      }
    },
    [id, invitesLoaded]
  );

  const activeModule = useMemo(
    () => modules.find((m) => m.moduleKey === activeModuleKey) || modules[0],
    [activeModuleKey, modules]
  );

  const search = useDebouncedValue(searchInput.trim(), SEARCH_DEBOUNCE_MS);
  const { details: moduleDetails, error: moduleError } = useModuleDetails(activeModule?.moduleKey);
  const list = useItemList(id, activeModule?.moduleId, search);
  const { counts, reload: reloadCounts } = useItemCounts(id);

  const canAddItems = useMemo(() => {
    return (
      !!collection &&
      ["OWNER", "ADMIN", "EDITOR"].includes(collection.role)
    );
  }, [collection]);

  const defaultStateKey = moduleDetails?.contract?.states?.[0]?.key || "OWNED";
  const moduleCounts = activeModule ? counts[activeModule.moduleId] : undefined;
  const itemCountsByModule = useMemo(
    () => Object.fromEntries(modules.map((m) => [m.moduleKey, counts[m.moduleId]?.total ?? 0])),
    [modules, counts]
  );
  const filterFields = useMemo(() => filterableFields(moduleDetails?.contract.fields), [moduleDetails]);
  const activeFilterCount = countActiveFilters(list.filters);
  const filtersActive = search !== "" || list.states.length > 0 || activeFilterCount > 0;

  // Selection belongs to the page on screen: an id that is no longer listed (another page, module or filter) is dropped.
  const visibleSelectedIds = useMemo(
    () => selectedIds.filter((selected) => list.items.some((item) => item.id === selected)),
    [selectedIds, list.items]
  );

  const refreshItems = () => {
    list.reload();
    reloadCounts();
  };

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
    list.patchItem({ ...item, stateKey });
    try {
      await changeItemState(id, item.id, stateKey);
      showToast("State updated", "success");
      refreshItems();
    } catch (err) {
      list.patchItem(item);
      showToast(getErrorMessage(err, "Failed to update state"), "error");
    }
  };

  const handleItemCreated = () => {
    refreshItems();
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
      role,
      expiresInDays,
    });
    setInvites((prev) => [resp, ...prev]);
    showToast("Invite created", "success");
    return resp;
  };

  const handleChangeRole = async (userId: string, role: string) => {
    try {
      await changeRole(userId, role);
      showToast("Role updated", "success");
    } catch (err) {
      showToast(getErrorMessage(err, "Failed to update role"), "error");
    }
  };

  const handleBatchStateChange = async (stateKey: string) => {
    if (!id || !visibleSelectedIds.length) return;
    setBatchBusy(true);
    const failures: string[] = [];
    const successes: string[] = [];

    for (const itemId of visibleSelectedIds) {
      try {
        await changeItemState(id, itemId, stateKey);
        successes.push(itemId);
      } catch {
        failures.push(itemId);
      }
    }

    setSelectedIds(failures);
    setBatchBusy(false);
    refreshItems();

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
    if (!id || !visibleSelectedIds.length) return;
    setBatchBusy(true);
    const failures: string[] = [];
    const deleted: string[] = [];

    for (const itemId of visibleSelectedIds) {
      try {
        await deleteItem(id, itemId);
        deleted.push(itemId);
      } catch {
        failures.push(itemId);
      }
    }

    setSelectedIds(failures);
    setBatchBusy(false);
    refreshItems();

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
      } catch (err) {
        showToast(getErrorMessage(err, "Failed to revoke invite"), "error");
      }
    })();
  };

  const handleRemoveMember = async (userId: string) => {
    try {
      await remove(userId);
      showToast("Member removed", "success");
    } catch (err) {
      showToast(getErrorMessage(err, "Failed to remove member"), "error");
    }
  };

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
        total={moduleCounts?.total ?? 0}
        counts={moduleCounts?.byState ?? {}}
        states={moduleDetails?.contract?.states}
        activeState={list.states[0] ?? null}
        onFilterChange={(stateKey) => list.setStates(stateKey ? [stateKey] : [])}
      />

      {!modules.length ? (
        <EmptyState
          title="No modules enabled"
          description="Click the Collection Settings button to enable modules for this collection."
        />
      ) : (
        <div className="space-y-4">
          <ItemsToolbar
            search={searchInput}
            onSearchChange={setSearchInput}
            sort={list.sort}
            sortOptions={sortOptions(moduleDetails?.contract.fields)}
            onSortChange={list.setSort}
            activeFilterCount={activeFilterCount}
            onOpenFilters={filterFields.length ? () => setFiltersOpen(true) : undefined}
          />
          <ItemsList
            items={list.items}
            loading={list.loading}
            refreshing={list.fetching && !list.loading}
            filtersActive={filtersActive}
            error={list.error}
            moduleName={
              moduleDetails?.name ||
              activeModule?.name ||
              activeModule?.moduleKey
            }
            moduleDefinition={moduleDetails?.contract}
            canAdd={canAddItems}
            onAdd={canAddItems ? handleAddItem : undefined}
            onRetry={list.reload}
            role={collection.role}
            onChangeState={canAddItems ? handleChangeState : undefined}
            onItemClick={(item) =>
              navigate(`/collections/${id}/items/${item.id}`)
            }
            selectedIds={visibleSelectedIds}
            onToggleItem={toggleItemSelection}
            onToggleAll={handleToggleAll}
            onClearSelection={clearSelection}
            onBatchChangeState={handleBatchStateChange}
            onBatchDelete={handleBatchDelete}
            batchBusy={batchBusy}
          />
          <ItemsPagination
            page={list.page}
            size={list.size}
            total={list.total}
            disabled={list.fetching}
            onPageChange={list.setPage}
            onSizeChange={list.setSize}
          />
        </div>
      )}

      <ItemFiltersDialog
        open={filtersOpen}
        onOpenChange={setFiltersOpen}
        fields={filterFields}
        filters={list.filters}
        onApply={list.setFilters}
      />

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
