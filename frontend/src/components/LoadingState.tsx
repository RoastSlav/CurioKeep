import {Loader2} from "lucide-react"
import type {ReactNode} from "react"

export default function LoadingState({ message }: { message?: ReactNode }) {
    return (
        <div className="flex flex-col items-center justify-center gap-3 py-12">
            <Loader2 className="w-8 h-8 animate-spin text-secondary"/>
            {message && <p className="text-sm text-text-secondary">{message}</p>}
        </div>
    )
}
