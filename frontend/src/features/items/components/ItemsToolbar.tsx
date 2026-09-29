import { ArrowDown, ArrowUp, Search, SlidersHorizontal } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import type { ItemSort } from "../api"
import { DEFAULT_SORT, defaultDirection, type SortOption } from "../itemSort"

type ItemsToolbarProps = {
    search: string
    onSearchChange: (search: string) => void
    sort: ItemSort | null
    sortOptions: SortOption[]
    onSortChange: (sort: ItemSort) => void
    activeFilterCount: number
    /** Absent when the module has no field that can be filtered, which hides the button. */
    onOpenFilters?: () => void
}

export function ItemsToolbar({ search, onSearchChange, sort, sortOptions, onSortChange, activeFilterCount, onOpenFilters }: ItemsToolbarProps) {
    const current = sort ?? DEFAULT_SORT
    const ascending = current.direction === "asc"

    return (
        <div className="flex flex-wrap items-center gap-3">
            <div className="relative min-w-56 flex-1">
                <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" aria-hidden="true" />
                <Input
                    type="search"
                    aria-label="Search items"
                    placeholder="Search items"
                    value={search}
                    onChange={(event) => onSearchChange(event.target.value)}
                    className="pl-9"
                />
            </div>
            <Select value={current.field} onValueChange={(field) => onSortChange({ field, direction: defaultDirection(field) })}>
                <SelectTrigger aria-label="Sort by" className="w-44">
                    <SelectValue />
                </SelectTrigger>
                <SelectContent>
                    {sortOptions.map((option) => (
                        <SelectItem key={option.value} value={option.value}>
                            {option.label}
                        </SelectItem>
                    ))}
                </SelectContent>
            </Select>
            <Button
                variant="outline"
                size="icon"
                aria-label={ascending ? "Sorted ascending, switch to descending" : "Sorted descending, switch to ascending"}
                onClick={() => onSortChange({ field: current.field, direction: ascending ? "desc" : "asc" })}
            >
                {ascending ? <ArrowUp /> : <ArrowDown />}
            </Button>
            {onOpenFilters ? (
                <Button variant="outline" onClick={onOpenFilters}>
                    <SlidersHorizontal />
                    Filters
                    {activeFilterCount > 0 ? <Badge aria-label={`${activeFilterCount} active`}>{activeFilterCount}</Badge> : null}
                </Button>
            ) : null}
        </div>
    )
}
