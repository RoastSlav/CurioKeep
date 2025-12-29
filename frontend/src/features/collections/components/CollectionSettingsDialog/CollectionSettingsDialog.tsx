"use client"

import {useMemo, useState} from "react"
import type {CollectionMember, CollectionModule, CollectionInvite, ModuleSummary} from "../../../../api/types"
import ModulesSection from "./ModulesSection"
import MembersSection from "./MembersSection"
import InviteMemberForm from "./InviteMemberForm"
import PendingInvitesSection from "./PendingInvitesSection"
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
} from "../../../../../components/ui/dialog"
import {Tabs, TabsContent, TabsList, TabsTrigger} from "../../../../../components/ui/tabs"

type TabKey = "modules" | "members"

type Props = {
    open: boolean
    onClose: () => void
    currentUserId?: string
    availableModules: ModuleSummary[]
    enabledModules: CollectionModule[]
    members: CollectionMember[]
    invites: CollectionInvite[]
    loadingModules?: boolean
    savingModules?: boolean
    modulesError?: string | null
    loadingMembers?: boolean
    savingMembers?: boolean
    membersError?: string | null
    onRefreshModules: () => void
    onRefreshMembers: () => void
    onEnableModule: (moduleKey: string) => Promise<void>
    onDisableModule: (moduleKey: string) => Promise<void>
    onChangeRole: (userId: string, role: CollectionMember["role"]) => Promise<void>
    onRemoveMember: (userId: string) => Promise<void>
    onCreateInvite: (role: CollectionInvite["role"], expiresInDays?: number) => Promise<CollectionInvite>
    onRevokeInvite?: (token: string) => void
}

export default function CollectionSettingsDialog({
                                                     open,
                                                     onClose,
                                                     currentUserId,
                                                     availableModules,
                                                     enabledModules,
                                                     members,
                                                     invites,
                                                     loadingModules,
                                                     savingModules,
                                                     modulesError,
                                                     loadingMembers,
                                                     savingMembers,
                                                     membersError,
                                                     onRefreshModules,
                                                     onRefreshMembers,
                                                     onEnableModule,
                                                     onDisableModule,
                                                     onChangeRole,
                                                     onRemoveMember,
                                                     onCreateInvite,
                                                     onRevokeInvite,
}: Props) {
    const [tab, setTab] = useState<TabKey>("modules")

    const sortedMembers = useMemo(
        () => [...members].sort((a, b) => (a.role === "OWNER" ? -1 : b.role === "OWNER" ? 1 : 0)),
        [members],
    )

    const handleInvite = async (role: CollectionInvite["role"], expiresInDays?: number | null) => {
        return onCreateInvite(role, expiresInDays ?? undefined)
    }

    return (
        <Dialog open={open} onOpenChange={(isOpen) => !isOpen && onClose()}>
            <DialogContent className="max-w-3xl max-h-[90vh] overflow-y-auto">
                <DialogHeader>
                    <DialogTitle className="text-2xl font-bold uppercase">Collection Settings</DialogTitle>
                    <DialogDescription>Configure modules and manage members for this collection</DialogDescription>
                </DialogHeader>

                <Tabs value={tab} onValueChange={(v) => setTab(v as TabKey)} className="mt-4">
                    <TabsList className="w-full justify-start">
                        <TabsTrigger value="modules" className="font-bold uppercase">
                            Modules
                        </TabsTrigger>
                        <TabsTrigger value="members" className="font-bold uppercase">
                            Members
                        </TabsTrigger>
                    </TabsList>

                    <TabsContent value="modules" className="mt-4">
                        <ModulesSection
                            availableModules={availableModules}
                            enabledModules={enabledModules}
                            loading={loadingModules}
                            saving={savingModules}
                            error={modulesError}
                            onEnable={onEnableModule}
                            onDisable={onDisableModule}
                            onRefresh={onRefreshModules}
                        />
                    </TabsContent>

                    <TabsContent value="members" className="mt-4 space-y-6">
                        <MembersSection
                            currentUserId={currentUserId}
                            members={sortedMembers}
                            loading={loadingMembers}
                            saving={savingMembers}
                            error={membersError}
                            onChangeRole={(userId, role) => onChangeRole(userId, role)}
                            onRemove={(userId) => onRemoveMember(userId)}
                            onRefresh={onRefreshMembers}
                        />
                        <InviteMemberForm onCreate={(payload) => handleInvite(payload.role, payload.expiresInDays)}/>
                        <PendingInvitesSection invites={invites} onRevoke={onRevokeInvite}/>
                    </TabsContent>
                </Tabs>
            </DialogContent>
        </Dialog>
    )
}
