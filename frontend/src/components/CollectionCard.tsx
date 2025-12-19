import {Link as RouterLink} from "react-router-dom"
import type {Collection} from "../api/types"
import {Card, CardContent} from "../../components/ui/card"
import {Badge} from "../../components/ui/badge"

export default function CollectionCard({ collection }: { collection: Collection }) {
    const isOwner = collection.role?.toUpperCase() === "OWNER"

    const stats = [] as { label: string; value: number }[]
    if (collection.itemsCount !== undefined) {
        stats.push({label: "Items", value: collection.itemsCount})
    }
    if (collection.modulesCount !== undefined) {
        stats.push({label: "Modules", value: collection.modulesCount})
    }

    return (
        <RouterLink to={`/collections/${collection.id}`} className="block h-full">
            <Card
                className="h-full brutal-border brutal-shadow-sm hover:translate-x-0.5 hover:translate-y-0.5 hover:shadow-none transition-all cursor-pointer bg-card border-primary/20 hover:border-primary/40">
                <CardContent className="p-4 flex flex-col gap-3">
                    <div className="flex items-start justify-between gap-2">
                        <h3 className="text-lg font-bold leading-tight text-card-foreground">{collection.name}</h3>
                        <Badge
                            variant={isOwner ? "default" : "outline"}
                            className={isOwner ? "bg-primary text-primary-foreground" : "border-border text-muted-foreground"}
            >
                            {isOwner ? "Owner" : "Shared"}
                        </Badge>
                    </div>
                    {collection.description &&
                        <p className="text-sm text-muted-foreground">{collection.description}</p>}
                    {stats.length ? (
                        <div className="flex flex-wrap gap-4">
                            {stats.map((stat) => (
                                <div key={stat.label} className="flex flex-col">
                                    <span className="text-xs text-muted-foreground">{stat.label}</span>
                                    <span className="text-sm font-semibold text-card-foreground">{stat.value}</span>
                                </div>
                            ))}
                        </div>
                    ) : (
                        <p className="text-xs text-muted-foreground">ID: {collection.id}</p>
                    )}
                </CardContent>
            </Card>
        </RouterLink>
    )
}
