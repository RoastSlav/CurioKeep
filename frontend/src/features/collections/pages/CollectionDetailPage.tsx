import { Alert, Paper, Stack, Typography } from "@mui/material";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import type { Collection, CollectionModule, Item, ModuleDetails, ModuleStateDef } from "../../../api/types";
import { useToast } from "../../../components/Toasts";
import EmptyState from "../../../components/EmptyState";
import ErrorState from "../../../components/ErrorState";
import LoadingState from "../../../components/LoadingState";
import CollectionHeader from "../components/CollectionHeader";
import ModuleSelector from "../components/ModuleSelector";
import CollectionActionsMenu from "../components/CollectionActionsMenu";
import { getCollection, listCollectionModules } from "../api";
import ItemsList from "../../items/components/ItemsList";
import { changeItemState, listItems } from "../../items/api";
import AddItemDialog from "../../items/components/AddItemDialog/AddItemDialog";
import { getModuleDetails } from "../../modules/api";

export default function CollectionDetailPage() {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const { showToast } = useToast();

    const [collection, setCollection] = useState<Collection | null>(null);
    const [modules, setModules] = useState<CollectionModule[]>([]);
    const [activeModuleKey, setActiveModuleKey] = useState<string | null>(null);
    const [items, setItems] = useState<Item[]>([]);
    const [moduleDetails, setModuleDetails] = useState<ModuleDetails | null>(null);

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [itemsLoading, setItemsLoading] = useState(false);
    const [itemsError, setItemsError] = useState<string | null>(null);
    const [moduleError, setModuleError] = useState<string | null>(null);
    const [addOpen, setAddOpen] = useState(false);

    const role = collection?.role?.toUpperCase();
    const canAddItems = role === "OWNER" || role === "ADMIN" || role === "EDITOR";

    const defaultStateKey = useMemo(() => {
        const states = moduleDetails?.contract.states ?? [];
        return [...states].sort((a: ModuleStateDef, b: ModuleStateDef) => (a.order ?? 0) - (b.order ?? 0))[0]?.key;
    }, [moduleDetails]);

    const loadCollection = useCallback(async () => {
        if (!id) return;
        setLoading(true);
        setError(null);
        try {
            const [col, mods] = await Promise.all([getCollection(id), listCollectionModules(id)]);
            setCollection(col);
            setModules(mods);
            setActiveModuleKey((prev) => prev || mods[0]?.moduleKey || null);
        } catch (err: any) {
            setError(err?.message || "Failed to load collection");
        } finally {
            setLoading(false);
        }
    }, [id]);

    useEffect(() => {
        void loadCollection();
    }, [loadCollection]);

    const activeModule = useMemo(
        () => modules.find((m) => m.moduleKey === activeModuleKey) || modules[0],
        [activeModuleKey, modules]
    );

    const fetchModuleAndItems = async (moduleKey: string) => {
        if (!id) return;
        const module = modules.find((m) => m.moduleKey === moduleKey);
        if (!module) return;

        setModuleDetails(null);
        setModuleError(null);
        setItems([]);
        setItemsLoading(true);
        setItemsError(null);

        try {
            const [details, page] = await Promise.all([getModuleDetails(moduleKey), listItems(id, module.moduleId)]);
            setModuleDetails(details);
            setItems(page.content);
        } catch (err: any) {
            const message = err?.message || "Failed to load module or items";
            setModuleError(message);
            setItemsError(message);
        } finally {
            setItemsLoading(false);
        }
    };

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

    const handleChangeState = async (item: Item, stateKey: string) => {
        if (!id) return;
        const snapshot = items;
        setItems((prev) => prev.map((i) => (i.id === item.id ? { ...i, stateKey } : i)));
        try {
            const updated = await changeItemState(id, item.id, stateKey);
            setItems((prev) => prev.map((i) => (i.id === updated.id ? updated : i)));
            showToast("State updated", "success");
        } catch (err: any) {
            setItems(snapshot);
            showToast(err?.message || "Failed to update state", "error");
        }
    };

    const handleItemCreated = (item: Item) => {
        setItems((prev) => [item, ...prev]);
        showToast("Item added", "success");
    };

    const handleOpenSettings = () => {
        showToast("Collection settings coming soon", "info");
    };

    if (loading) return <LoadingState message="Loading collection..." />;
    if (error || !collection || !id)
        return <ErrorState title="Could not load collection" message={error || "Collection not found"} onRetry={loadCollection} />;

    return (
        <Stack spacing={3}>
            <CollectionHeader
                collection={collection}
                actions={<CollectionActionsMenu role={collection.role} onAddItem={canAddItems ? handleAddItem : undefined} onOpenSettings={handleOpenSettings} />}
            />

            <Paper variant="outlined" sx={{ p: 2 }}>
                <Stack spacing={1.5}>
                    <Typography variant="subtitle1" fontWeight={700}>
                        Modules
                    </Typography>
                    <ModuleSelector modules={modules} activeModuleKey={activeModuleKey} onChange={(key) => setActiveModuleKey(key)} />
                    {moduleError && <Alert severity="error">{moduleError}</Alert>}
                </Stack>
            </Paper>

            {!modules.length ? (
                <EmptyState title="No modules enabled" description="Ask an admin to enable modules for this collection." />
            ) : (
                <ItemsList
                    items={items}
                    loading={itemsLoading}
                    error={itemsError}
                    moduleName={moduleDetails?.name || activeModule?.name || activeModule?.moduleKey}
                    moduleDefinition={moduleDetails?.contract}
                    canAdd={canAddItems}
                    onAdd={canAddItems ? handleAddItem : undefined}
                    onRetry={activeModuleKey ? () => fetchModuleAndItems(activeModuleKey) : undefined}
                    role={collection.role}
                    onChangeState={canAddItems ? handleChangeState : undefined}
                    onItemClick={(item) => navigate(`/collections/${id}/items/${item.id}`)}
                />
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
        </Stack>
    );
}
