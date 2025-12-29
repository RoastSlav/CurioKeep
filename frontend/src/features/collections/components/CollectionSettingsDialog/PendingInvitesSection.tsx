"use client"

import {Copy, X} from "lucide-react"
import type {CollectionInvite} from "../../../../api/types"
import {Button} from "../../../../../components/ui/button"
import {Badge} from "../../../../../components/ui/badge"
import {Alert, AlertDescription} from "../../../../../components/ui/alert"
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from "../../../../../components/ui/table"
import {Tooltip, TooltipContent, TooltipProvider, TooltipTrigger} from "../../../../../components/ui/tooltip"

function buildLink(token: string) {
    if (typeof window === "undefined") return `/invites/collection/${token}`
    return `${window.location.origin}/invites/collection/${token}`
}

type Props = {
    invites: CollectionInvite[]
    onCopy?: (token: string) => void
    onRevoke?: (token: string) => void | Promise<void>
}

export default function PendingInvitesSection({ invites, onCopy, onRevoke }: Props) {
    if (!invites.length) return null

    return (
        <div className="space-y-3">
            <h3 className="text-lg font-bold uppercase">Pending Invites</h3>
            <div className="border-2 border-border overflow-x-auto">
                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHead className="font-bold uppercase">Token</TableHead>
                            <TableHead className="font-bold uppercase">Role</TableHead>
                            <TableHead className="font-bold uppercase">Expires</TableHead>
                            <TableHead className="font-bold uppercase text-right">Actions</TableHead>
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {invites.map((invite) => (
                            <TableRow key={invite.token}>
                                <TableCell className="max-w-[200px]">
                                    <TooltipProvider>
                                        <Tooltip>
                                            <TooltipTrigger asChild>
                                                <p className="text-sm truncate cursor-help">{invite.token}</p>
                                            </TooltipTrigger>
                                            <TooltipContent>
                                                <p className="break-all max-w-xs">{invite.token}</p>
                                            </TooltipContent>
                                        </Tooltip>
                                    </TooltipProvider>
                                </TableCell>
                                <TableCell>
                                    <Badge variant="secondary">{invite.role}</Badge>
                                </TableCell>
                                <TableCell className="text-sm">
                                    {invite.expiresAt ? new Date(invite.expiresAt).toLocaleString() : "Never"}
                                </TableCell>
                                <TableCell className="text-right">
                                    <div className="flex items-center justify-end gap-1">
                                        <Button
                                            size="icon-sm"
                                            variant="ghost"
                                            onClick={() => {
                                                void navigator.clipboard.writeText(buildLink(invite.token))
                                                onCopy?.(invite.token)
                                            }}
                                        >
                                            <Copy className="w-4 h-4"/>
                                        </Button>
                                        {onRevoke && (
                                            <Button
                                                size="icon-sm"
                                                variant="ghost"
                                                className="text-destructive hover:text-destructive"
                                                onClick={() => void onRevoke(invite.token)}
                                            >
                                                <X className="w-4 h-4"/>
                                            </Button>
                                        )}
                                    </div>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </div>
            {!onRevoke && (
                <Alert>
                    <AlertDescription>Revoking invites not yet supported in API.</AlertDescription>
                </Alert>
            )}
        </div>
    )
}
