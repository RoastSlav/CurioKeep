import type {ReactNode} from "react"
import type {Collection} from "../../../api/types"
import {Badge} from "../../../../components/ui/badge"

export default function CollectionHeader({ collection, actions }: { collection: Collection; actions?: ReactNode }) {
    const role = collection.role?.toUpperCase()
    const isOwner = role === "OWNER"

    return (
        <div className="flex flex-col md:flex-row gap-4 md:items-center justify-between">
            <div className="space-y-2 flex-1 min-w-0">
                <div className="flex items-center gap-2 flex-wrap">
                    <h1 className="text-3xl md:text-4xl font-bold tracking-tight uppercase">{collection.name}</h1>
                    {role && (
                        <Badge variant={isOwner ? "default" : "secondary"} className={isOwner ? "bg-primary" : ""}>
                            {role}
                        </Badge>
                    )}
                </div>
                {collection.description && (
                    <p className="text-base text-text-secondary leading-relaxed">{collection.description}</p>
                )}
            </div>
            {actions}
        </div>
    )
}
