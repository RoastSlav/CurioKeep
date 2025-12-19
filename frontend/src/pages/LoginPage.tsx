"use client"

import type {FormEvent} from "react"
import {useState} from "react"
import {Navigate, useLocation, useNavigate, useSearchParams} from "react-router-dom"
import {type ApiError, isApiError} from "../api/errors"
import {useAuth} from "../auth/useAuth"
import {useSetupStatus} from "../components/AppGate"
import LoadingState from "../components/LoadingState"
import ErrorState from "../components/ErrorState"
import {useToast} from "../components/Toasts"
import {Card, CardContent, CardDescription, CardHeader, CardTitle} from "../../components/ui/card"
import {Input} from "../../components/ui/input"
import {Label} from "../../components/ui/label"
import {Button} from "../../components/ui/button"
import {Alert, AlertDescription} from "../../components/ui/alert"
import {AlertCircle} from "lucide-react"

export default function LoginPage() {
    const navigate = useNavigate()
    const location = useLocation()
    const [params] = useSearchParams()
    const {user, loading: authLoading, error: authError, login} = useAuth()
    const {setupRequired, loading: setupLoading, error: setupError, reload: reloadSetup} = useSetupStatus()
    const {showToast} = useToast()

    const [email, setEmail] = useState("")
    const [password, setPassword] = useState("")
    const [submitting, setSubmitting] = useState(false)
    const [submitError, setSubmitError] = useState<string | null>(null)

    if (setupLoading) return <LoadingState message="Checking setup..."/>
    if (setupError) return <ErrorState title="Setup check failed" message={setupError} onRetry={reloadSetup}/>
    if (setupRequired) return <Navigate to="/setup" replace/>

    if (authLoading) return <LoadingState message="Checking session..."/>
    if (user) return <Navigate to="/" replace/>

    const onSubmit = async (evt: FormEvent) => {
        evt.preventDefault()
        setSubmitError(null)
        setSubmitting(true)
        try {
            await login(email, password)
            showToast("Signed in", "success")
            const returnTo = params.get("returnTo") || "/"
            navigate(returnTo, {replace: true, state: {from: location}})
        } catch (err) {
            const apiErr = err as ApiError
            setSubmitError(isApiError(apiErr) ? apiErr.message : "Login failed")
        } finally {
            setSubmitting(false)
        }
    }

    return (
        <div className="max-w-md mx-auto mt-12">
            <Card>
                <CardHeader>
                    <CardTitle className="text-2xl">Sign in</CardTitle>
                    <CardDescription>Enter your credentials to access your account</CardDescription>
                </CardHeader>
                <CardContent>
                    <form onSubmit={onSubmit} className="space-y-4">
                        {authError && (
                            <Alert variant="destructive">
                                <AlertCircle className="h-4 w-4"/>
                                <AlertDescription>{authError}</AlertDescription>
                            </Alert>
                        )}
                        {submitError && (
                            <Alert variant="destructive">
                                <AlertCircle className="h-4 w-4"/>
                                <AlertDescription>{submitError}</AlertDescription>
                            </Alert>
                        )}
                        <div className="space-y-2">
                            <Label htmlFor="email">Email</Label>
                            <Input
                                id="email"
                                type="email"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                placeholder="you@example.com"
                                required
                                autoFocus
                            />
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="password">Password</Label>
                            <Input
                                id="password"
                                type="password"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                placeholder="••••••••"
                                required
                            />
                        </div>
                        <Button type="submit" className="w-full bg-secondary hover:bg-secondary-dark"
                                disabled={submitting}>
                            {submitting ? "Signing in..." : "Sign in"}
                        </Button>
                    </form>
                </CardContent>
            </Card>
        </div>
    )
}
