"use client"

import {AlertCircle} from "lucide-react"
import {useMemo, useState} from "react"
import type {FormEvent} from "react"
import {Navigate, useNavigate} from "react-router-dom"
import {apiFetch} from "../api/client"
import {type ApiError, isApiError} from "../api/errors"
import {useSetupStatus} from "../components/AppGate"
import {useToast} from "../components/Toasts"
import LoadingState from "../components/LoadingState"
import ErrorState from "../components/ErrorState"
import {Card, CardContent, CardDescription, CardHeader, CardTitle} from "../../components/ui/card"
import {Input} from "../../components/ui/input"
import {Label} from "../../components/ui/label"
import {Button} from "../../components/ui/button"
import {Alert, AlertDescription} from "../../components/ui/alert"

export default function SetupPage() {
    const navigate = useNavigate()
    const {setupRequired, loading, error, reload, setSetupRequired} = useSetupStatus()
    const {showToast} = useToast()

    const [email, setEmail] = useState("")
    const [displayName, setDisplayName] = useState("")
    const [password, setPassword] = useState("")
    const [confirmPassword, setConfirmPassword] = useState("")
    const [submitting, setSubmitting] = useState(false)
    const [submitError, setSubmitError] = useState<string | null>(null)

    const emailValid = useMemo(() => /.+@.+/.test(email), [email])

    if (loading) {
        return <LoadingState message="Checking setup..."/>
    }

    if (error) {
        return <ErrorState title="Setup check failed" message={error} onRetry={reload}/>
    }

    if (!setupRequired) {
        return <Navigate to="/login" replace/>
    }

    const onSubmit = async (evt: FormEvent) => {
        evt.preventDefault()
        setSubmitError(null)

        if (!emailValid) {
            setSubmitError("Please enter a valid email")
            return
        }
        if (!password || !confirmPassword) {
            setSubmitError("Password is required")
            return
        }
        if (password !== confirmPassword) {
            setSubmitError("Passwords do not match")
            return
        }

        setSubmitting(true)
        try {
            await apiFetch("/setup/admin", {method: "POST", body: {email, password, displayName}})
            setSetupRequired(false)
            showToast("Admin account created", "success")
            navigate("/login", {replace: true})
        } catch (err) {
            const apiErr = err as ApiError
            setSubmitError(isApiError(apiErr) ? apiErr.message : "Setup failed")
        } finally {
            setSubmitting(false)
        }
    }

    return (
        <div className="max-w-xl mx-auto mt-12 p-4">
            <Card>
                <CardHeader>
                    <CardTitle className="text-2xl">First-time setup</CardTitle>
                    <CardDescription>Create the initial administrator account to finish setup.</CardDescription>
                </CardHeader>
                <CardContent>
                    <form onSubmit={onSubmit} className="space-y-4">
                        {submitError && (
                            <Alert variant="destructive">
                                <AlertCircle className="h-4 w-4"/>
                                <AlertDescription>{submitError}</AlertDescription>
                            </Alert>
                        )}
                        <div className="space-y-2">
                            <Label htmlFor="email">Email *</Label>
                            <Input
                                id="email"
                                type="email"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                placeholder="admin@example.com"
                                required
                                autoFocus
                            />
                            {email && !emailValid && <p className="text-sm text-destructive">Enter a valid email</p>}
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="displayName">Display name *</Label>
                            <Input
                                id="displayName"
                                value={displayName}
                                onChange={(e) => setDisplayName(e.target.value)}
                                placeholder="Your name"
                                required
                            />
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="password">Password *</Label>
                            <Input
                                id="password"
                                type="password"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                placeholder="••••••••"
                                required
                            />
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="confirmPassword">Confirm password *</Label>
                            <Input
                                id="confirmPassword"
                                type="password"
                                value={confirmPassword}
                                onChange={(e) => setConfirmPassword(e.target.value)}
                                placeholder="••••••••"
                                required
                            />
                            {confirmPassword && password !== confirmPassword && (
                                <p className="text-sm text-destructive">Passwords must match</p>
                            )}
                        </div>
                        <Button type="submit" className="w-full bg-secondary hover:bg-secondary-dark"
                                disabled={submitting}>
                            {submitting ? "Creating..." : "Create admin"}
                        </Button>
                    </form>
                </CardContent>
            </Card>
        </div>
    )
}
