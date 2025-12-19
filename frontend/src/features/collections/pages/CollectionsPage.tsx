"use client"

import {Plus} from "lucide-react"
import {useEffect, useMemo, useState} from "react"
import {useNavigate} from "react-router-dom"
import type {Collection, CreateCollectionRequest} from "../../../api/types"
import EmptyState from "../../../components/EmptyState"
import ErrorState from "../../../components/ErrorState"
import LoadingState from "../../../components/LoadingState"
import ConfirmDialog from "../../../components/ConfirmDialog"
import {useToast} from "../../../components/Toasts"
import CollectionCard from "../components/CollectionCard"
import CreateCollectionDialog from "../components/CreateCollectionDialog"
import EditCollectionDialog from "../components/EditCollectionDialog"
import {Button} from "../../../../components/ui/button"
import {
    createCollection,
    deleteCollection,
    listCollections,
    updateCollection,
    type UpdateCollectionRequest,
} from "../api"

export default function CollectionsPage() {
    const navigate = useNavigate()
    const {showToast} = useToast()

    const [collections, setCollections] = useState<Collection[]>([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)

    const [createOpen, setCreateOpen] = useState(false)
    const [editTarget, setEditTarget] = useState<Collection | null>(null)
    const [confirmDelete, setConfirmDelete] = useState<Collection | null>(null)

    useEffect(() => {
        const load = async () => {
            setLoading(true)
            setError(null)
            try {
                const data = await listCollections()
                setCollections(data)
            } catch (err: any) {
                setError(err?.message || "Failed to load collections")
            } finally {
                setLoading(false)
            }
        }

        void load()
    }, [])

    const hasCollections = useMemo(() => collections.length > 0, [collections])

    const handleCreate = async (payload: CreateCollectionRequest) => {
        try {
            const created = await createCollection(payload)
            setCollections((prev) => [created, ...prev])
            setCreateOpen(false)
            showToast("Collection created", "success")
            navigate(`/collections/${created.id}`)
        } catch (err: any) {
            showToast(err?.message || "Failed to create collection", "error")
            throw err
        }
    }

    const handleEdit = async (payload: UpdateCollectionRequest) => {
        if (!editTarget) return
        try {
            const updated = await updateCollection(editTarget.id, payload)
            setCollections((prev) => prev.map((c) => (c.id === updated.id ? updated : c)))
            setEditTarget(null)
            showToast("Collection updated", "success")
        } catch (err: any) {
            showToast(err?.message || "Failed to update collection", "error")
            throw err
        }
    }

    const handleDelete = async () => {
        if (!confirmDelete) return
        const target = confirmDelete
        try {
            await deleteCollection(target.id)
            setCollections((prev) => prev.filter((c) => c.id !== target.id))
            showToast("Collection deleted", "success")
        } catch (err: any) {
            showToast(err?.message || "Failed to delete collection", "error")
        } finally {
            setConfirmDelete(null)
        }
    }

    if (loading) return <LoadingState message="Loading collections..."/>
    if (error)
        return <ErrorState title="Could not load collections" message={error} onRetry={() => window.location.reload()}/>

    return (
        <div className="space-y-8">
            {/* Header */}
            <div className="flex flex-col sm:flex-row gap-6 sm:items-center justify-between">
                <div className="space-y-2">
                    <h1 className="text-3xl md:text-4xl font-bold tracking-tight uppercase">Collections</h1>
                    <p className="text-base text-text-secondary">Manage collections you own or have been shared with
                        you</p>
                </div>
                <Button
                    onClick={() => setCreateOpen(true)}
                    className="w-full sm:w-auto min-w-[160px] bg-secondary hover:bg-secondary-dark"
                >
                    <Plus className="w-4 h-4 mr-2"/>
                    New collection
                </Button>
            </div>

            {/* Grid */}
            {hasCollections ? (
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                    {collections.map((collection) => (
                        <CollectionCard
                            key={collection.id}
                            collection={collection}
                            onEdit={(c) => setEditTarget(c)}
                            onDelete={(c) => setConfirmDelete(c)}
                        />
                    ))}
                </div>
            ) : (
                <EmptyState
                    title="No collections yet"
                    description="Create your first collection to start organizing."
                    actionLabel="Create collection"
                    onAction={() => setCreateOpen(true)}
                    secondary={
                        <div>
                            <p className="text-xs text-text-secondary">Tip: you can share collections with teammates
                                once created.</p>
                        </div>
                    }
                />
            )}

            <CreateCollectionDialog open={createOpen} onClose={() => setCreateOpen(false)} onCreate={handleCreate}/>
            <EditCollectionDialog
                open={Boolean(editTarget)}
                collection={editTarget}
                onClose={() => setEditTarget(null)}
                onSave={handleEdit}
            />
            <ConfirmDialog
                open={Boolean(confirmDelete)}
                title="Delete collection?"
                message="This action cannot be undone. Items inside the collection may also be removed."
                confirmLabel="Delete"
                onConfirm={handleDelete}
                onClose={() => setConfirmDelete(null)}
            />
        </div>
    )
}
