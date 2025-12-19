"use client"

import type {ReactNode} from "react"
import {Alert, AlertDescription, AlertTitle} from "../../components/ui/alert"
import {Button} from "../../components/ui/button"
import {AlertCircle} from "lucide-react"

type Props = {
    title?: string
    message?: ReactNode
    onRetry?: () => void
}

export default function ErrorState({ title = "Something went wrong", message, onRetry }: Props) {
    return (
        <Alert variant="destructive" className="border-red-200">
            <AlertCircle className="h-4 w-4"/>
            <AlertTitle>{title}</AlertTitle>
            {message && <AlertDescription className="text-text-secondary">{message}</AlertDescription>}
            {onRetry && (
                <div className="mt-3">
                    <Button
                        variant="outline"
                        size="sm"
                        onClick={onRetry}
                        className="border-red-300 hover:bg-red-50 bg-transparent"
                    >
                        Retry
                    </Button>
                </div>
            )}
        </Alert>
    )
}
