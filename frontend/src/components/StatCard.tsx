import type {ReactNode} from "react"
import {Card, CardContent} from "../../components/ui/card"
import {Skeleton} from "../../components/ui/skeleton"

export default function StatCard({
                                     label,
                                     value,
                                     loading,
                                     hint,
                                     variant = "default",
}: {
    label: string
    value: ReactNode
    loading?: boolean
    hint?: ReactNode
    variant?: "default" | "secondary" | "accent"
}) {
    const variantClasses = {
        default: "bg-primary/5 border-primary/20 dark:bg-primary/10 dark:border-primary/30",
        secondary: "bg-secondary/5 border-secondary/20 dark:bg-secondary/10 dark:border-secondary/30",
        accent: "bg-accent/5 border-accent/20 dark:bg-accent/10 dark:border-accent/30",
    }

    const bgClass = variantClasses[variant]

    return (
        <Card className={`relative overflow-visible brutal-border ${bgClass}`}>
            <div
                className={`absolute -top-2 -left-2 w-full h-full -z-10 brutal-border dark:shadow-lg ${
                    variant === "default"
                        ? "bg-primary/10 dark:bg-primary/15"
                        : variant === "secondary"
                            ? "bg-secondary/10 dark:bg-secondary/15"
                            : "bg-accent/10 dark:bg-accent/15"
                }`}
            />
            <CardContent className="pt-6">
                <div className="space-y-3">
                    <p className="text-xs font-bold uppercase tracking-widest">{label}</p>
                    {loading ? (
                        <Skeleton className="w-20 h-12"/>
                    ) : (
                        <div className="text-5xl font-bold tracking-tight leading-none font-mono">{value}</div>
                    )}
                    {hint && <p className="text-xs font-medium opacity-70">{hint}</p>}
                </div>
            </CardContent>
        </Card>
    )
}
