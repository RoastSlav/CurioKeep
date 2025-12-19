"use client"

import {Trash2, Edit, ExternalLink} from "lucide-react"
import {Link as RouterLink} from "react-router-dom"
import type {Collection} from "../../../api/types"
import {Card, CardContent, CardFooter} from "../../../../components/ui/card"
import {Button} from "../../../../components/ui/button"
import {Badge} from "../../../../components/ui/badge"

export default function CollectionCard({
                                           collection,
                                           onEdit,
                                           onDelete,
}: {
    collection: Collection
    onEdit?: (collection: Collection) => void
    onDelete?: (collection: Collection) => void
}) {
    const role = collection.role?.toUpperCase()
    const isOwner = role === "OWNER"
    const isAdmin = role === "ADMIN" || isOwner

    return (
        <Card className="h-full flex flex-col brutal-card-hover overflow-visible">
            {/* Color stripe at top */}
            <div
                className="h-2 -mt-6 -mx-0 border-b-4 border-border"
                style={{backgroundColor: isOwner ? "var(--secondary)" : "var(--accent)"}}
            />
            <CardContent className="flex-1 pt-4">
                <div className="space-y-4">
                    <div className="flex items-start justify-between gap-2">
                        <h3 className="text-xl font-bold leading-tight flex-1 uppercase">{collection.name}</h3>
                        <Badge
                            variant={isOwner ? "default" : "secondary"}
                            className={isOwner ? "bg-secondary text-secondary-foreground" : ""}
                        >
                            {role || ""}
                        </Badge>
                    </div>
                    {collection.description && (
                        <p className="text-sm leading-relaxed text-muted-foreground">{collection.description}</p>
                    )}
                    <div className="pt-2">
                        <p className="text-xs font-mono font-bold text-muted-foreground">ID: {collection.id}</p>
                    </div>
                </div>
            </CardContent>
            <CardFooter className="gap-2 flex-wrap border-t-4 border-border pt-4">
                <Button asChild size="sm" className="flex-1 min-w-fit bg-secondary text-secondary-foreground">
                    <RouterLink to={`/collections/${collection.id}`}>
                        Open
                        <ExternalLink className="w-3.5 h-3.5 ml-1.5"/>
                    </RouterLink>
                </Button>
                {isAdmin && onEdit && (
                    <Button size="sm" variant="outline" onClick={() => onEdit(collection)}>
                        <Edit className="w-3.5 h-3.5 mr-1.5"/>
                        Edit
                    </Button>
                )}
                {isOwner && onDelete && (
                    <Button size="sm" variant="destructive" onClick={() => onDelete(collection)}>
                        <Trash2 className="w-3.5 h-3.5 mr-1.5"/>
                        Delete
                    </Button>
                )}
            </CardFooter>
        </Card>
    )
}
