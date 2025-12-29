"use client"

import type React from "react"

import {useMemo, useState} from "react"
import {Copy} from "lucide-react"
import type {CollectionInvite} from "../../../../api/types"
import type {CreateCollectionInviteRequest} from "../../../../api/types"
import {Button} from "../../../../../components/ui/button"
import {Input} from "../../../../../components/ui/input"
import {Label} from "../../../../../components/ui/label"
import {Alert, AlertDescription} from "../../../../../components/ui/alert"
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from "../../../../../components/ui/select"

const ROLES: Array<CreateCollectionInviteRequest["role"]> = ["VIEWER", "EDITOR", "ADMIN"]

type Props = {
    onCreate: (payload: CreateCollectionInviteRequest) => Promise<CollectionInvite>
}

export default function InviteMemberForm({ onCreate }: Props) {
    const [role, setRole] = useState<CreateCollectionInviteRequest["role"]>("VIEWER")
    const [days, setDays] = useState<number | "">(7)
    const [link, setLink] = useState<string | null>(null)
    const [error, setError] = useState<string | null>(null)
    const [loading, setLoading] = useState(false)

    const fullUrl = useMemo(() => {
        if (!link) return null
        if (typeof window === "undefined") return `/invites/collection/${link}`
        return `${window.location.origin}/invites/collection/${link}`
    }, [link])

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault()
        setLoading(true)
        setError(null)
        try {
            const payload: CreateCollectionInviteRequest = {
                role,
                expiresInDays: days === "" ? undefined : Number(days),
            }
            const resp = await onCreate(payload)
            setLink(resp.token)
        } catch (err: any) {
            setError(err?.message || "Failed to create invite")
        } finally {
            setLoading(false)
        }
    }

    const copyLink = () => {
        if (!fullUrl) return
        void navigator.clipboard.writeText(fullUrl)
    }

    return (
        <form onSubmit={handleSubmit} className="space-y-4">
            <h3 className="text-lg font-bold uppercase">Invite Member</h3>

            <div className="flex flex-col sm:flex-row gap-3">
                <div className="flex-1 space-y-1">
                    <Label htmlFor="role">Role</Label>
                    <Select value={role} onValueChange={(v) => setRole(v as CreateCollectionInviteRequest["role"])}>
                        <SelectTrigger>
                            <SelectValue/>
                        </SelectTrigger>
                        <SelectContent>
                            {ROLES.map((r) => (
                                <SelectItem key={r} value={r}>
                                    {r}
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                </div>

                <div className="flex-1 space-y-1">
                    <Label htmlFor="days">Expires in days</Label>
                    <Input
                        id="days"
                        type="number"
                        value={days}
                        onChange={(e) => setDays(e.target.value === "" ? "" : Number(e.target.value))}
                        min={1}
                        max={365}
                    />
                </div>

                <div className="flex items-end">
                    <Button type="submit" disabled={loading} className="whitespace-nowrap">
                        {loading ? "Creating..." : "Create invite"}
                    </Button>
                </div>
            </div>

            {error && (
                <Alert variant="destructive">
                    <AlertDescription>{error}</AlertDescription>
                </Alert>
            )}

            {link && (
                <div
                    className="border-2 border-border p-3 bg-card flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
                    <div className="space-y-1 flex-1 min-w-0">
                        <p className="text-sm text-muted-foreground">Share this link</p>
                        <p className="text-sm break-all">{fullUrl}</p>
                    </div>
                    <Button variant="outline" onClick={copyLink}>
                        <Copy className="w-4 h-4 mr-2"/>
                        Copy
                    </Button>
                </div>
            )}
        </form>
    )
}
