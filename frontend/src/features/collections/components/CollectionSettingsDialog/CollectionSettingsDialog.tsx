import { Dialog, DialogContent, DialogTitle, Tab, Tabs } from "@mui/material";
import { useMemo, useState } from "react";
import type { CollectionMember, CollectionModule, CollectionInvite, ModuleSummary } from "../../../../api/types";
import ModulesSection from "./ModulesSection";
import MembersSection from "./MembersSection";
import InviteMemberForm from "./InviteMemberForm";
import PendingInvitesSection from "./PendingInvitesSection";

type TabKey = "modules" | "members";

type Props = {
    open: boolean;
    onClose: () => void;
    currentUserId?: string;
    availableModules: ModuleSummary[];
    enabledModules: CollectionModule[];
    members: CollectionMember[];
    invites: CollectionInvite[];
    loadingModules?: boolean;
    savingModules?: boolean;
    modulesError?: string | null;
    loadingMembers?: boolean;
    savingMembers?: boolean;
    membersError?: string | null;
    onRefreshModules: () => void;
    onRefreshMembers: () => void;
    onEnableModule: (moduleKey: string) => Promise<void>;
    onDisableModule: (moduleKey: string) => Promise<void>;
    onChangeRole: (userId: string, role: CollectionMember["role"]) => Promise<void>;
    onRemoveMember: (userId: string) => Promise<void>;
    onCreateInvite: (role: CollectionInvite["role"], expiresInDays?: number) => Promise<CollectionInvite>;
    onRevokeInvite?: (token: string) => void;
};

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
    const [tab, setTab] = useState<TabKey>("modules");

    const sortedMembers = useMemo(
        () => [...members].sort((a, b) => (a.role === "OWNER" ? -1 : b.role === "OWNER" ? 1 : 0)),
        [members]
    );

    const handleInvite = async (role: CollectionInvite["role"], expiresInDays?: number | null) => {
        return onCreateInvite(role, expiresInDays ?? undefined);
    };

    return (
        <Dialog open={open} onClose={onClose} fullWidth maxWidth="md">
            <DialogTitle>Collection Settings</DialogTitle>
            <DialogContent sx={{ pt: 0 }}>
                <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mb: 2 }}>
                    <Tab label="Modules" value="modules" />
                    <Tab label="Members" value="members" />
                </Tabs>

                {tab === "modules" ? (
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
                ) : (
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
                )}

                {tab === "members" && (
                    <>
                        <InviteMemberForm onCreate={(payload) => handleInvite(payload.role, payload.expiresInDays)} />
                        <PendingInvitesSection invites={invites} onRevoke={onRevokeInvite} />
                    </>
                )}
            </DialogContent>
        </Dialog>
    );
}
