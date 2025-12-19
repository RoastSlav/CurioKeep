"use client"

import type {ReactNode} from "react"
import {Button} from "../../components/ui/button"

export default function EmptyState({
                                       title,
                                       description,
                                       actionLabel,
                                       onAction,
                                       secondary,
}: {
    title: string
    description?: ReactNode
    actionLabel?: string
    onAction?: () => void
    secondary?: ReactNode
}) {
    return (
        <div className="border-4 border-dashed border-black dark:border-white p-12 text-center bg-muted">
            <div className="flex flex-col items-center justify-center gap-6">
                <h3 className="text-2xl font-bold uppercase">{title}</h3>
                {description && <p className="text-base max-w-md font-medium">{description}</p>}
                {actionLabel && onAction && (
                    <Button onClick={onAction} size="lg">
                        {actionLabel}
                    </Button>
                )}
                {secondary}
            </div>
        </div>
    )
}
