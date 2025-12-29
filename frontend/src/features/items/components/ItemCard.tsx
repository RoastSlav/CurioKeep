"use client"

import type {Item} from "../../../api/types"
import {Card, CardContent} from "../../../../components/ui/card"
import {Badge} from "../../../../components/ui/badge"
import {Avatar, AvatarFallback} from "../../../../components/ui/avatar"

export default function ItemCard({ item }: { item: Item }) {
    const firstIdentifier = item.identifiers?.[0]
    const imageUrl = item.attributes?.providerImageUrl as string | undefined
    const title = (item.attributes?.title as string | undefined) || item.id

    return (
        <Card className="h-full">
            {imageUrl ? (
                <div className="h-40 border-b-2 border-border overflow-hidden">
                    <img src={imageUrl || "/placeholder.svg"} alt={title} className="w-full h-full object-cover"/>
                </div>
            ) : (
                <div className="h-40 border-b-2 border-border flex items-center justify-center bg-muted">
                    <Avatar className="w-16 h-16">
                        <AvatarFallback className="text-xl font-bold">{(title || "?").substring(0, 1)}</AvatarFallback>
                    </Avatar>
                </div>
            )}
            <CardContent className="p-4 space-y-3">
                <div className="flex items-center justify-between gap-2">
                    <p className="font-bold truncate">{item.id}</p>
                    <Badge variant="secondary">{item.stateKey}</Badge>
                </div>
                {firstIdentifier && (
                    <p className="text-sm text-muted-foreground">
                        {firstIdentifier.type}: {firstIdentifier.value}
                    </p>
                )}
                <div className="flex gap-4">
                    <span className="text-xs text-muted-foreground">Created: {item.createdAt || ""}</span>
                    <span className="text-xs text-muted-foreground">Updated: {item.updatedAt || ""}</span>
                </div>
            </CardContent>
        </Card>
    )
}
