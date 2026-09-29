import { ChevronLeft, ChevronRight } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { PAGE_SIZES } from "../api"

type ItemsPaginationProps = {
    page: number
    size: number
    total: number
    disabled?: boolean
    onPageChange: (page: number) => void
    onSizeChange: (size: number) => void
}

export function ItemsPagination({ page, size, total, disabled, onPageChange, onSizeChange }: ItemsPaginationProps) {
    if (total === 0) return null

    const lastPage = Math.max(0, Math.ceil(total / size) - 1)
    const first = page * size + 1
    const last = Math.min(total, (page + 1) * size)

    return (
        <nav aria-label="Pagination" className="flex flex-wrap items-center justify-between gap-3">
            <p className="text-sm text-muted-foreground" aria-live="polite">
                Showing {first}–{last} of {total}
            </p>
            <div className="flex items-center gap-2">
                <Select value={String(size)} onValueChange={(value) => onSizeChange(Number(value))} disabled={disabled}>
                    <SelectTrigger aria-label="Items per page" className="w-24">
                        <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                        {PAGE_SIZES.map((option) => (
                            <SelectItem key={option} value={String(option)}>
                                {option} / page
                            </SelectItem>
                        ))}
                    </SelectContent>
                </Select>
                <Button variant="outline" size="icon-sm" aria-label="Previous page" disabled={disabled || page <= 0} onClick={() => onPageChange(page - 1)}>
                    <ChevronLeft />
                </Button>
                <span className="text-sm">
                    Page {page + 1} of {lastPage + 1}
                </span>
                <Button variant="outline" size="icon-sm" aria-label="Next page" disabled={disabled || page >= lastPage} onClick={() => onPageChange(page + 1)}>
                    <ChevronRight />
                </Button>
            </div>
        </nav>
    )
}
