"use client"

import type React from "react"

import { useCallback, useEffect, useState } from "react"
import { useNavigate, useParams } from "react-router-dom"
import { Loader2, CheckCircle, XCircle } from "lucide-react"
import { validateInvite, acceptInvite } from "../../../api/admin/invitesApi"
import { Button } from "../../../../components/ui/button"
import { Input } from "../../../../components/ui/input"
import { Label } from "../../../../components/ui/label"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "../../../../components/ui/card"
import { Alert, AlertDescription } from "../../../../components/ui/alert"

export default function AcceptInvitePage() {
  const { token } = useParams<{ token: string }>()
  const navigate = useNavigate()

  const [validating, setValidating] = useState(true)
  const [valid, setValid] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)

  const [displayName, setDisplayName] = useState("")
  const [password, setPassword] = useState("")
  const [passwordConfirm, setPasswordConfirm] = useState("")

  const validateToken = useCallback(async () => {
    if (!token) {
      setError("Invalid invite link")
      setValidating(false)
      return
    }
    setValidating(true)
    setError(null)
    try {
      const result = await validateInvite(token)
      setValid(result.valid)
      if (!result.valid) {
        setError("This invite is invalid or has expired")
      }
    } catch (err: any) {
      setError(err?.message || "Failed to validate invite")
      setValid(false)
    } finally {
      setValidating(false)
    }
  }, [token])

  useEffect(() => {
    void validateToken()
  }, [validateToken])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!token) return

    if (password !== passwordConfirm) {
      setError("Passwords do not match")
      return
    }

    if (password.length < 8) {
      setError("Password must be at least 8 characters")
      return
    }

    if (!displayName.trim()) {
      setError("Display name is required")
      return
    }

    setSubmitting(true)
    setError(null)
    try {
      await acceptInvite({ token, password, displayName: displayName.trim() })
      setSuccess(true)
      setTimeout(() => {
        navigate("/login")
      }, 2000)
    } catch (err: any) {
      setError(err?.message || "Failed to accept invite")
    } finally {
      setSubmitting(false)
    }
  }

  if (validating) {
    return (
      <div className="min-h-screen flex items-center justify-center p-4 bg-background">
        <Card className="w-full max-w-md brutal-border brutal-shadow-lg">
          <CardContent className="pt-6">
            <div className="flex items-center justify-center gap-3 text-muted-foreground">
              <Loader2 className="w-6 h-6 animate-spin" />
              <span className="font-semibold">VALIDATING INVITE...</span>
            </div>
          </CardContent>
        </Card>
      </div>
    )
  }

  if (!valid) {
    return (
      <div className="min-h-screen flex items-center justify-center p-4 bg-background">
        <Card className="w-full max-w-md brutal-border brutal-shadow-lg">
          <CardHeader>
            <CardTitle className="uppercase flex items-center gap-2 text-destructive">
              <XCircle className="w-6 h-6" />
              INVALID INVITE
            </CardTitle>
            <CardDescription>This invite link is not valid or has expired.</CardDescription>
          </CardHeader>
          <CardContent>
            {error && (
              <Alert variant="destructive" className="brutal-border">
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}
          </CardContent>
          <CardFooter>
            <Button onClick={() => navigate("/login")} className="w-full brutal-border">
              GO TO LOGIN
            </Button>
          </CardFooter>
        </Card>
      </div>
    )
  }

  if (success) {
    return (
      <div className="min-h-screen flex items-center justify-center p-4 bg-background">
        <Card className="w-full max-w-md brutal-border brutal-shadow-lg">
          <CardHeader>
            <CardTitle className="uppercase flex items-center gap-2 text-primary">
              <CheckCircle className="w-6 h-6" />
              ACCOUNT CREATED
            </CardTitle>
            <CardDescription>Your account has been created successfully. Redirecting to login...</CardDescription>
          </CardHeader>
        </Card>
      </div>
    )
  }

  return (
    <div className="min-h-screen flex items-center justify-center p-4 bg-background">
      <Card className="w-full max-w-md brutal-border brutal-shadow-lg">
        <CardHeader>
          <CardTitle className="uppercase">ACCEPT INVITE</CardTitle>
          <CardDescription>Create your account to get started with CurioKeep</CardDescription>
        </CardHeader>
        <form onSubmit={handleSubmit}>
          <CardContent className="space-y-4">
            {error && (
              <Alert variant="destructive" className="brutal-border">
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}

            <div className="space-y-2">
              <Label htmlFor="displayName" className="uppercase font-bold">
                DISPLAY NAME
              </Label>
              <Input
                id="displayName"
                type="text"
                placeholder="Enter your name"
                value={displayName}
                onChange={(e) => setDisplayName(e.target.value)}
                className="brutal-border"
                disabled={submitting}
                required
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="password" className="uppercase font-bold">
                PASSWORD
              </Label>
              <Input
                id="password"
                type="password"
                placeholder="Minimum 8 characters"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="brutal-border"
                disabled={submitting}
                required
                minLength={8}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="passwordConfirm" className="uppercase font-bold">
                CONFIRM PASSWORD
              </Label>
              <Input
                id="passwordConfirm"
                type="password"
                placeholder="Re-enter your password"
                value={passwordConfirm}
                onChange={(e) => setPasswordConfirm(e.target.value)}
                className="brutal-border"
                disabled={submitting}
                required
              />
            </div>
          </CardContent>
          <CardFooter>
            <Button type="submit" className="w-full brutal-border brutal-shadow-sm" disabled={submitting}>
              {submitting ? (
                <>
                  <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                  CREATING ACCOUNT...
                </>
              ) : (
                "CREATE ACCOUNT"
              )}
            </Button>
          </CardFooter>
        </form>
      </Card>
    </div>
  )
}
