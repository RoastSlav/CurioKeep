"use client"

import { useState, useEffect } from "react"
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "../../../../components/ui/dialog"
import { Button } from "../../../../components/ui/button"
import { Input } from "../../../../components/ui/input"
import { Label } from "../../../../components/ui/label"
import { Eye, EyeOff } from "lucide-react"
import { getCredentialStatus, updateCredentials, deleteCredentials } from "../api"
import type { Provider } from "../providerTypes"

type Props = {
  provider: Provider
  open: boolean
  onClose: () => void
  onUpdated: (status: { credentialsConfigured: boolean }) => void
}

export function ProviderCredentialsModal({ provider, open, onClose, onUpdated }: Props) {
  const [loading, setLoading] = useState(false)
  const [isAdmin, setIsAdmin] = useState(true)
  const [values, setValues] = useState<Record<string, string>>({})
  const [showPassword, setShowPassword] = useState<Record<string, boolean>>({})
  const [error, setError] = useState<string | null>(null)
  const [confirmClear, setConfirmClear] = useState(false)

  useEffect(() => {
    if (open) {
      checkStatus()
    }
  }, [open, provider.key])

  async function checkStatus() {
    try {
      setLoading(true)
      setError(null)
      await getCredentialStatus(provider.key)
      setIsAdmin(true)
    } catch (err: any) {
      if (err.status === 403) {
        setIsAdmin(false)
        setError("Admin access required to manage credentials")
      }
    } finally {
      setLoading(false)
    }
  }

  const handleSave = async () => {
    const missingFields = provider.credentialFields
      .filter((f) => f.required && !values[f.name]?.trim())
      .map((f) => f.label)

    if (missingFields.length > 0) {
      setError(`Missing required fields: ${missingFields.join(", ")}`)
      return
    }

    try {
      setLoading(true)
      setError(null)
      const status = await updateCredentials(provider.key, { values })
      onUpdated({ credentialsConfigured: status.credentialsConfigured })
      onClose()
    } catch (err: any) {
      setError(err.message || "Failed to save credentials")
    } finally {
      setLoading(false)
    }
  }

  const handleClear = async () => {
    if (!confirmClear) {
      setConfirmClear(true)
      return
    }

    try {
      setLoading(true)
      setError(null)
      await deleteCredentials(provider.key)
      onUpdated({ credentialsConfigured: false })
      setValues({})
      setConfirmClear(false)
      onClose()
    } catch (err: any) {
      setError(err.message || "Failed to clear credentials")
    } finally {
      setLoading(false)
    }
  }

  const canSave = isAdmin && provider.credentialFields.every((f) => !f.required || values[f.name]?.trim())

  return (
    <Dialog open={open} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="sm:max-w-lg bg-card border-4 border-border brutal-shadow">
        <DialogHeader>
          <DialogTitle className="text-2xl font-bold uppercase">{provider.displayName}</DialogTitle>
          <DialogDescription>Configure API credentials for {provider.displayName}</DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-4">
          {!isAdmin && (
            <div className="p-4 bg-destructive/10 border-2 border-destructive/50 rounded">
              <p className="text-sm text-destructive font-semibold">Admin access required</p>
            </div>
          )}

          {error && (
            <div className="p-4 bg-destructive/10 border-2 border-destructive/50 rounded">
              <p className="text-sm text-destructive">{error}</p>
            </div>
          )}

          {provider.credentialFields.map((field) => (
            <div key={field.name} className="space-y-2">
              <Label htmlFor={field.name} className="font-bold uppercase text-sm">
                {field.label}
                {field.required && <span className="text-destructive ml-1">*</span>}
              </Label>
              {field.helpText && <p className="text-xs text-muted-foreground">{field.helpText}</p>}
              <div className="relative">
                <Input
                  id={field.name}
                  type={field.secret && !showPassword[field.name] ? "password" : "text"}
                  value={values[field.name] || ""}
                  onChange={(e) => setValues({ ...values, [field.name]: e.target.value })}
                  disabled={!isAdmin || loading}
                  className="pr-10"
                  placeholder={field.secret ? "Enter API key..." : ""}
                />
                {field.secret && (
                  <button
                    type="button"
                    onClick={() => setShowPassword({ ...showPassword, [field.name]: !showPassword[field.name] })}
                    className="absolute right-2 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                  >
                    {showPassword[field.name] ? <EyeOff size={16} /> : <Eye size={16} />}
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>

        <div className="flex gap-3 justify-end">
          {provider.credentialsConfigured && (
            <Button variant="destructive" onClick={handleClear} disabled={!isAdmin || loading}>
              {confirmClear ? "CONFIRM CLEAR?" : "CLEAR"}
            </Button>
          )}
          <Button variant="outline" onClick={onClose} disabled={loading}>
            CANCEL
          </Button>
          <Button onClick={handleSave} disabled={!canSave || loading}>
            {loading ? "SAVING..." : "SAVE"}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  )
}
