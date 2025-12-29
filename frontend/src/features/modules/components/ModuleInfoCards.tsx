"use client"

import type {ModuleDetails} from "../api/modulesApi"
import {Badge} from "../../../../components/ui/badge"

type Props = {
    module: ModuleDetails
}

export default function ModuleInfoCards({ module }: Props) {
    const meta = module.contract.meta
    const tags = meta?.tags ?? []
    const authors = meta?.authors ?? []

    const summaryItems = [
        {label: "Version", value: module.version},
        {label: "Source", value: module.source},
        {label: "Checksum", value: module.checksum},
        {label: "Updated", value: new Date(module.updatedAt).toLocaleString()},
    ]

    const metaItems = [
        {label: "Repository", value: meta?.repository ?? "—", href: meta?.repository},
        {label: "Homepage", value: meta?.homepage ?? "—", href: meta?.homepage},
        {label: "License", value: meta?.license ?? "—"},
        {label: "Min App", value: meta?.minAppVersion ?? "—"},
    ]

    return (
        <div className="grid gap-4 grid-cols-1 md:grid-cols-2">
            <div className="brutal-border brutal-shadow-sm bg-card p-4">
                <div className="flex flex-col gap-3">
                    <p className="text-sm font-bold text-muted-foreground uppercase border-b-2 border-border pb-2">
                        Module Summary
                    </p>
                    <div className="space-y-2">
                        {summaryItems.map((item) => (
                            <div key={item.label} className="flex justify-between items-start gap-2">
                                <span className="text-sm text-muted-foreground uppercase shrink-0">{item.label}</span>
                                <span
                                    className="text-sm text-card-foreground font-medium text-right break-all">{item.value}</span>
                            </div>
                        ))}
                    </div>
                </div>
            </div>

            <div className="brutal-border brutal-shadow-sm bg-card p-4">
                <div className="flex flex-col gap-3">
                    <p className="text-sm font-bold text-muted-foreground uppercase border-b-2 border-border pb-2">Metadata</p>
                    <div className="space-y-2">
                        {metaItems.map((item) => (
                            <div key={item.label} className="flex justify-between items-center gap-2">
                                <span className="text-sm text-muted-foreground uppercase">{item.label}</span>
                                {item.href ? (
                                    <a
                                        href={item.href}
                                        target="_blank"
                                        rel="noreferrer"
                                        className="text-sm text-primary hover:underline truncate max-w-[200px] font-medium"
                                    >
                                        {item.value}
                                    </a>
                                ) : (
                                    <span className="text-sm text-card-foreground font-medium">{item.value}</span>
                                )}
                            </div>
                        ))}
                    </div>
                </div>
            </div>

            {tags.length > 0 && (
                <div className="brutal-border brutal-shadow-sm bg-card p-4">
                    <div className="flex flex-col gap-3">
                        <p className="text-sm font-bold text-muted-foreground uppercase border-b-2 border-border pb-2">Tags</p>
                        <div className="flex flex-wrap gap-2">
                            {tags.map((tag) => (
                                <Badge key={tag} variant="outline" className="brutal-border uppercase">
                                    {tag}
                                </Badge>
                            ))}
                        </div>
                    </div>
                </div>
            )}

            {authors.length > 0 && (
                <div className="brutal-border brutal-shadow-sm bg-card p-4">
                    <div className="flex flex-col gap-3">
                        <p className="text-sm font-bold text-muted-foreground uppercase border-b-2 border-border pb-2">Authors</p>
                        <div className="space-y-2">
                            {authors.map((author, idx) => (
                                <div key={idx} className="flex flex-col">
                                    {author.name &&
                                        <span className="text-sm text-card-foreground font-medium">{author.name}</span>}
                                    {(author.email || author.url) && (
                                        <span className="text-xs text-muted-foreground">
                      {[author.email, author.url].filter(Boolean).join(" · ")}
                    </span>
                                    )}
                                </div>
                            ))}
                        </div>
                    </div>
                </div>
            )}
        </div>
    )
}
