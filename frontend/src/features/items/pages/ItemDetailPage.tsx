import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import LoadingState from "../../../components/LoadingState";
import ErrorState from "../../../components/ErrorState";
import { getCollection, listCollectionModules } from "../../collections/api";
import { getItem } from "../api";
import { getModuleDetails } from "../../modules/api";
import type { Collection, ModuleDefinition } from "../../../api/types";
import ItemDetailModal from "../components/ItemDetailModal";

export default function ItemDetailPage() {
    const { id: collectionId, itemId } = useParams<{ id: string; itemId: string }>();
    const navigate = useNavigate();
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [collection, setCollection] = useState<Collection | null>(null);
    const [moduleDefinition, setModuleDefinition] = useState<ModuleDefinition | null>(null);

    useEffect(() => {
        const load = async () => {
            if (!collectionId || !itemId) return;
            setLoading(true);
            setError(null);
            try {
                const [col, item, mods] = await Promise.all([
                    getCollection(collectionId),
                    getItem(collectionId, itemId),
                    listCollectionModules(collectionId),
                ]);
                setCollection(col);
                const moduleRef = mods.find((m) => m.moduleId === item.moduleId);
                if (moduleRef) {
                    const moduleDetails = await getModuleDetails(moduleRef.moduleKey);
                    setModuleDefinition(moduleDetails.contract);
                }
            } catch (err: any) {
                setError(err?.message || "Failed to load item");
            } finally {
                setLoading(false);
            }
        };
        void load();
    }, [collectionId, itemId]);

    if (!collectionId || !itemId) return <ErrorState title="Invalid route" message="Missing identifiers" />;
    if (loading) return <LoadingState message="Loading item..." />;
    if (error) return <ErrorState title="Could not load item" message={error} onRetry={() => window.location.reload()} />;
    if (!collection) return <ErrorState title="Missing collection" message="Collection not found" />;

    return (
        <ItemDetailModal
            open
            collectionId={collectionId}
            itemId={itemId}
            moduleDefinition={moduleDefinition}
            role={collection.role}
            onClose={() => navigate(`/collections/${collectionId}`)}
            onDeleted={() => navigate(`/collections/${collectionId}`)}
        />
    );
}
