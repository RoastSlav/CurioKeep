"use client"

import { useCallback, useEffect, useState } from "react"
import { Loader2, RefreshCw, Trash2, Plus, UserCheck, UserX, Copy, Check } from "lucide-react"
import { useAuth } from "../../../auth/useAuth"
import { listUsers, updateUserStatus, updateUserAdmin, deleteUser, type AdminUser } from "../../../api/admin/usersApi"
import { listInvites, createInvite, revokeInvite, type AdminInvite } from "../../../api/admin/invitesApi"
import { Button } from "../../../../components/ui/button"
import { Badge } from "../../../../components/ui/badge"
import { Alert, AlertDescription } from "../../../../components/ui/alert"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "../../../../components/ui/table"
import { Input } from "../../../../components/ui/input"
import { Label } from "../../../../components/ui/label"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "../../../../components/ui/dialog"
import { useToast } from "../../../components/Toasts"

export default function UsersPage() {
  const { user: currentUser } = useAuth()
  const { showToast } = useToast()

  const [users, setUsers] = useState<AdminUser[]>([])
  const [invites, setInvites] = useState<AdminInvite[]>([])
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const [deleteUserId, setDeleteUserId] = useState<string | null>(null)
  const [createDialogOpen, setCreateDialogOpen] = useState(false)
  const [newInviteEmail, setNewInviteEmail] = useState("")
  const [inviteToken, setInviteToken] = useState<string | null>(null)
  const [copiedToken, setCopiedToken] = useState(false)

  const loadData = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const [usersData, invitesData] = await Promise.all([listUsers(), listInvites()])
      setUsers(usersData)
      setInvites(invitesData)
    } catch (err: any) {
      setError(err?.message || "Failed to load data")
      showToast(err?.message || "Failed to load data", "error")
    } finally {
      setLoading(false)
    }
  }, [showToast])

  useEffect(() => {
    void loadData()
  }, [loadData])

  const handleToggleStatus = async (userId: string, currentStatus: string) => {
    const newStatus = currentStatus === "ACTIVE" ? "DISABLED" : "ACTIVE"
    setSaving(true)
    const snapshot = users
    setUsers((prev) => prev.map((u) => (u.id === userId ? { ...u, status: newStatus } : u)))
    try {
      await updateUserStatus(userId, { status: newStatus })
      showToast(`User ${newStatus.toLowerCase()}`, "success")
    } catch (err: any) {
      setUsers(snapshot)
      showToast(err?.message || "Failed to update status", "error")
    } finally {
      setSaving(false)
    }
  }

  const handleToggleAdmin = async (userId: string, currentAdmin: boolean) => {
    const newAdmin = !currentAdmin
    setSaving(true)
    const snapshot = users
    setUsers((prev) => prev.map((u) => (u.id === userId ? { ...u, admin: newAdmin } : u)))
    try {
      await updateUserAdmin(userId, { admin: newAdmin })
      showToast(newAdmin ? "Admin granted" : "Admin revoked", "success")
    } catch (err: any) {
      setUsers(snapshot)
      showToast(err?.message || "Failed to update admin", "error")
    } finally {
      setSaving(false)
    }
  }

  const handleDeleteUser = async () => {
    if (!deleteUserId) return
    const userId = deleteUserId
    setDeleteUserId(null)
    setSaving(true)
    const snapshot = users
    setUsers((prev) => prev.filter((u) => u.id !== userId))
    try {
      await deleteUser(userId)
      showToast("User deleted", "success")
    } catch (err: any) {
      setUsers(snapshot)
      showToast(err?.message || "Failed to delete user", "error")
    } finally {
      setSaving(false)
    }
  }

  const handleCreateInvite = async () => {
    if (!newInviteEmail.trim()) {
      showToast("Email is required", "error")
      return
    }
    setSaving(true)
    try {
      const result = await createInvite({ email: newInviteEmail.trim() })
      setInviteToken(result.token)
      setNewInviteEmail("")
      await loadData()
      showToast("Invite created", "success")
    } catch (err: any) {
      showToast(err?.message || "Failed to create invite", "error")
    } finally {
      setSaving(false)
    }
  }

  const handleRevokeInvite = async (token: string) => {
    setSaving(true)
    const snapshot = invites
    setInvites((prev) => prev.filter((i) => i.token !== token))
    try {
      await revokeInvite(token)
      showToast("Invite revoked", "success")
    } catch (err: any) {
      setInvites(snapshot)
      showToast(err?.message || "Failed to revoke invite", "error")
    } finally {
      setSaving(false)
    }
  }

  const copyInviteLink = () => {
    if (!inviteToken) return
    const inviteUrl = `${window.location.origin}/invites/accept/${inviteToken}`
    navigator.clipboard.writeText(inviteUrl)
    setCopiedToken(true)
    setTimeout(() => setCopiedToken(false), 2000)
    showToast("Invite link copied to clipboard", "success")
  }

  const closeTokenDialog = () => {
    setInviteToken(null)
    setCopiedToken(false)
  }

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-4xl font-bold uppercase tracking-tight mb-2">USERS</h1>
        <p className="text-muted-foreground">Manage user accounts and invitations</p>
      </div>

      {error && (
        <Alert variant="destructive" className="brutal-border brutal-shadow-sm">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      {/* Users Section */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-2xl font-bold uppercase">USER ACCOUNTS</h2>
          <div className="flex items-center gap-2">
            {saving && <Loader2 className="w-4 h-4 animate-spin" />}
            <Button
              size="sm"
              variant="outline"
              onClick={loadData}
              disabled={loading}
              className="brutal-border bg-transparent"
            >
              <RefreshCw className="w-4 h-4" />
            </Button>
          </div>
        </div>

        {loading ? (
          <div className="flex items-center gap-2 text-muted-foreground p-8 justify-center brutal-border brutal-shadow-sm bg-card">
            <Loader2 className="w-5 h-5 animate-spin" />
            <span>Loading users...</span>
          </div>
        ) : users.length ? (
          <div className="brutal-border brutal-shadow-sm overflow-x-auto bg-card">
            <Table>
              <TableHeader>
                <TableRow className="border-b-2 border-border">
                  <TableHead className="font-bold uppercase">USER</TableHead>
                  <TableHead className="font-bold uppercase">PROVIDER</TableHead>
                  <TableHead className="font-bold uppercase">STATUS</TableHead>
                  <TableHead className="font-bold uppercase">ROLE</TableHead>
                  <TableHead className="font-bold uppercase">LAST LOGIN</TableHead>
                  <TableHead className="font-bold uppercase text-right">ACTIONS</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {users.map((u) => {
                  const isSelf = u.id === currentUser?.id
                  const lastLogin = u.lastLoginAt ? new Date(u.lastLoginAt).toLocaleDateString() : "Never"
                  return (
                    <TableRow key={u.id} className="border-b border-border">
                      <TableCell>
                        <div className="space-y-0.5">
                          <p className="font-bold">{u.displayName || u.email}</p>
                          <p className="text-sm text-muted-foreground">{u.email}</p>
                        </div>
                      </TableCell>
                      <TableCell>
                        <Badge variant="outline" className="uppercase">
                          {u.authProvider}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <Badge
                          variant={u.status === "ACTIVE" ? "default" : "secondary"}
                          className="uppercase bg-secondary"
                        >
                          {u.status}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <Badge variant={u.admin ? "default" : "secondary"} className="uppercase">
                          {u.admin ? "ADMIN" : "USER"}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-sm text-muted-foreground">{lastLogin}</TableCell>
                      <TableCell className="text-right">
                        <div className="flex items-center justify-end gap-2">
                          {!isSelf && (
                            <>
                              <Button
                                size="sm"
                                variant={u.status === "ACTIVE" ? "destructive" : "default"}
                                disabled={saving}
                                onClick={() => handleToggleStatus(u.id, u.status)}
                                className="brutal-border"
                              >
                                {u.status === "ACTIVE" ? (
                                  <UserX className="w-4 h-4" />
                                ) : (
                                  <UserCheck className="w-4 h-4" />
                                )}
                              </Button>
                              <Button
                                size="sm"
                                variant={u.admin ? "secondary" : "default"}
                                disabled={saving}
                                onClick={() => handleToggleAdmin(u.id, u.admin)}
                                className="brutal-border"
                              >
                                {u.admin ? "REVOKE ADMIN" : "GRANT ADMIN"}
                              </Button>
                              <Button
                                size="sm"
                                variant="destructive"
                                disabled={saving}
                                onClick={() => setDeleteUserId(u.id)}
                                className="brutal-border"
                              >
                                <Trash2 className="w-4 h-4" />
                              </Button>
                            </>
                          )}
                          {isSelf && <span className="text-sm text-muted-foreground italic">You</span>}
                        </div>
                      </TableCell>
                    </TableRow>
                  )
                })}
              </TableBody>
            </Table>
          </div>
        ) : (
          <Alert className="brutal-border brutal-shadow-sm">
            <AlertDescription>No users found.</AlertDescription>
          </Alert>
        )}
      </div>

      {/* Invites Section */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-2xl font-bold uppercase">PENDING INVITES</h2>
          <Button
            onClick={() => setCreateDialogOpen(true)}
            disabled={saving}
            className="brutal-border brutal-shadow-sm"
          >
            <Plus className="w-4 h-4 mr-2" />
            CREATE INVITE
          </Button>
        </div>

        {invites.length ? (
          <div className="brutal-border brutal-shadow-sm overflow-x-auto bg-card">
            <Table>
              <TableHeader>
                <TableRow className="border-b-2 border-border">
                  <TableHead className="font-bold uppercase">EMAIL</TableHead>
                  <TableHead className="font-bold uppercase">INVITED BY</TableHead>
                  <TableHead className="font-bold uppercase">CREATED</TableHead>
                  <TableHead className="font-bold uppercase text-right">ACTIONS</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {invites.map((inv) => (
                  <TableRow key={inv.token} className="border-b border-border">
                    <TableCell className="font-bold">{inv.email}</TableCell>
                    <TableCell className="text-sm text-muted-foreground">
                      {inv.invitedBy.displayName || inv.invitedBy.email}
                    </TableCell>
                    <TableCell className="text-sm text-muted-foreground">
                      {new Date(inv.createdAt).toLocaleDateString()}
                    </TableCell>
                    <TableCell className="text-right">
                      <Button
                        size="sm"
                        variant="destructive"
                        disabled={saving}
                        onClick={() => handleRevokeInvite(inv.token)}
                        className="brutal-border"
                      >
                        REVOKE
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        ) : (
          <Alert className="brutal-border brutal-shadow-sm">
            <AlertDescription>No pending invites.</AlertDescription>
          </Alert>
        )}
      </div>

      {/* Delete User Confirmation Dialog */}
      <Dialog open={Boolean(deleteUserId)} onOpenChange={() => setDeleteUserId(null)}>
        <DialogContent className="brutal-border brutal-shadow-lg">
          <DialogHeader>
            <DialogTitle className="uppercase">DELETE USER</DialogTitle>
            <DialogDescription>
              Are you sure you want to delete this user? This action cannot be undone.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteUserId(null)} className="brutal-border">
              CANCEL
            </Button>
            <Button variant="destructive" onClick={handleDeleteUser} className="brutal-border">
              DELETE
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Create Invite Dialog */}
      <Dialog open={createDialogOpen} onOpenChange={setCreateDialogOpen}>
        <DialogContent className="brutal-border brutal-shadow-lg">
          <DialogHeader>
            <DialogTitle className="uppercase">CREATE INVITE</DialogTitle>
            <DialogDescription>Enter the email address of the person you want to invite.</DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="invite-email" className="uppercase font-bold">
                EMAIL ADDRESS
              </Label>
              <Input
                id="invite-email"
                type="email"
                placeholder="user@example.com"
                value={newInviteEmail}
                onChange={(e) => setNewInviteEmail(e.target.value)}
                className="brutal-border"
                disabled={saving}
              />
            </div>
          </div>
          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setCreateDialogOpen(false)}
              disabled={saving}
              className="brutal-border"
            >
              CANCEL
            </Button>
            <Button onClick={handleCreateInvite} disabled={saving || !newInviteEmail.trim()} className="brutal-border">
              {saving ? <Loader2 className="w-4 h-4 animate-spin" /> : "CREATE"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Invite Token Display Dialog */}
      <Dialog open={Boolean(inviteToken)} onOpenChange={closeTokenDialog}>
        <DialogContent className="brutal-border brutal-shadow-lg">
          <DialogHeader>
            <DialogTitle className="uppercase">INVITE CREATED</DialogTitle>
            <DialogDescription>Share this link with the invited user. It will only be shown once.</DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label className="uppercase font-bold">INVITE LINK</Label>
              <div className="flex gap-2">
                <Input
                  readOnly
                  value={`${window.location.origin}/invites/accept/${inviteToken}`}
                  className="brutal-border font-mono text-sm"
                  onClick={(e) => (e.target as HTMLInputElement).select()}
                />
                <Button onClick={copyInviteLink} className="brutal-border brutal-shadow-sm">
                  {copiedToken ? <Check className="w-4 h-4" /> : <Copy className="w-4 h-4" />}
                </Button>
              </div>
            </div>
            <Alert className="brutal-border">
              <AlertDescription>Make sure to copy this link now. It won't be shown again.</AlertDescription>
            </Alert>
          </div>
          <DialogFooter>
            <Button onClick={closeTokenDialog} className="brutal-border">
              CLOSE
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
