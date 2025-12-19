"use client"

import {useState} from "react"
import {Loader2, RefreshCw, Trash2} from "lucide-react"
import type {CollectionMember} from "../../../../api/types"
import ConfirmRemoveMemberDialog from "./ConfirmRemoveMemberDialog"
import {Button} from "../../../../../components/ui/button"
import {Badge} from "../../../../../components/ui/badge"
import {Alert, AlertDescription} from "../../../../../components/ui/alert"
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from "../../../../../components/ui/select"
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from "../../../../../components/ui/table"

const ROLE_OPTIONS: Array<CollectionMember["role"]> = ["ADMIN", "EDITOR", "VIEWER"]

type Props = {
    currentUserId?: string
    members: CollectionMember[]
    loading?: boolean
    saving?: boolean
    error?: string | null
    onChangeRole: (userId: string, role: CollectionMember["role"]) => Promise<void>
    onRemove: (userId: string) => Promise<void>
    onRefresh?: () => void
}

export default function MembersSection({
                                           currentUserId,
                                           members,
                                           loading,
                                           saving,
                                           error,
                                           onChangeRole,
                                           onRemove,
                                           onRefresh,
}: Props) {
    const [removeUserId, setRemoveUserId] = useState<string | null>(null)

    const handleRoleChange = async (userId: string, role: CollectionMember["role"]) => {
        await onChangeRole(userId, role)
    }

    const removeMember = async () => {
        if (!removeUserId) return
        const id = removeUserId
        setRemoveUserId(null)
        await onRemove(id)
    }

    return (
        <div className="space-y-4">
            <div className="flex items-center justify-between">
                <h3 className="text-lg font-bold uppercase">Members</h3>
                <div className="flex items-center gap-2">
                    {saving && <Loader2 className="w-4 h-4 animate-spin"/>}
                    {onRefresh && (
                        <Button size="sm" variant="outline" onClick={onRefresh} disabled={loading}>
                            <RefreshCw className="w-4 h-4"/>
                        </Button>
                    )}
                </div>
            </div>

            {error && (
                <Alert variant="destructive">
                    <AlertDescription>{error}</AlertDescription>
                </Alert>
            )}

            {loading ? (
                <div className="flex items-center gap-2 text-muted-foreground">
                    <Loader2 className="w-4 h-4 animate-spin"/>
                    <span>Loading members...</span>
                </div>
            ) : members.length ? (
                <div className="border-2 border-border overflow-x-auto">
                    <Table>
                        <TableHeader>
                            <TableRow>
                                <TableHead className="font-bold uppercase">User</TableHead>
                                <TableHead className="font-bold uppercase">Role</TableHead>
                                <TableHead className="font-bold uppercase text-right">Actions</TableHead>
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            {members.map((m) => {
                                const isSelf = m.userId === currentUserId
                                const isOwner = m.role === "OWNER"
                                const disableActions = saving || isSelf
                                const showActions = !isOwner
                                return (
                                    <TableRow key={m.userId}>
                                        <TableCell>
                                            <div className="space-y-0.5">
                                                <p className="font-bold">{m.displayName || m.email}</p>
                                                <p className="text-sm text-muted-foreground">{m.email}</p>
                                            </div>
                                        </TableCell>
                                        <TableCell>
                                            <Badge variant={m.role === "OWNER" ? "default" : "secondary"}
                                                   className="bg-secondary">
                                                {m.role}
                                            </Badge>
                                        </TableCell>
                                        <TableCell className="text-right">
                                            <div className="flex items-center justify-end gap-2">
                                                {showActions && (
                                                    <Select
                                                        value={m.role}
                                                        onValueChange={(value) =>
                                                            void handleRoleChange(m.userId, value as CollectionMember["role"])
                                                        }
                                                        disabled={disableActions}
                                                    >
                                                        <SelectTrigger className="w-28">
                                                            <SelectValue/>
                                                        </SelectTrigger>
                                                        <SelectContent>
                                                            {ROLE_OPTIONS.map((r) => (
                                                                <SelectItem key={r} value={r}>
                                                                    {r}
                                                                </SelectItem>
                                                            ))}
                                                        </SelectContent>
                                                    </Select>
                                                )}
                                                {showActions && (
                                                    <Button
                                                        size="icon-sm"
                                                        variant="destructive"
                                                        disabled={disableActions}
                                                        onClick={() => setRemoveUserId(m.userId)}
                                                    >
                                                        <Trash2 className="w-4 h-4"/>
                                                    </Button>
                                                )}
                                            </div>
                                        </TableCell>
                                    </TableRow>
                                )
                            })}
                        </TableBody>
                    </Table>
                </div>
            ) : (
                <Alert>
                    <AlertDescription>No members found.</AlertDescription>
                </Alert>
            )}

            <ConfirmRemoveMemberDialog
                open={Boolean(removeUserId)}
                memberName={members.find((m) => m.userId === removeUserId)?.displayName || undefined}
                onCancel={() => setRemoveUserId(null)}
                onConfirm={() => void removeMember()}
            />
        </div>
    )
}
