"use client"

import {useEffect, useMemo, useState} from "react"
import {useNavigate, useParams} from "react-router-dom"
import {Loader2} from "lucide-react"
import {useAuth} from "../../../auth/useAuth"
import LoadingState from "../../../components/LoadingState"
import ErrorState from "../../../components/ErrorState"
import {acceptCollectionInvite, validateCollectionInvite} from "../api/collectionInvitesApi"
import {Card, CardContent, CardFooter, CardHeader, CardTitle} from "../../../../components/ui/card"
import {Button} from "../../../../components/ui/button"
import {Alert, AlertDescription} from "../../../../components/ui/alert"

export default function AcceptCollectionInvitePage() {
    const {token} = useParams<{ token: string }>()
    const navigate = useNavigate()
    const {user, loading: authLoading} = useAuth()

    const [status, setStatus] = useState<"idle" | "loading" | "ready" | "accepting" | "accepted">("idle")
    const [error, setError] = useState<string | null>(null)
    const [role, setRole] = useState<string | null>(null)
    const [collectionId, setCollectionId] = useState<string | null>(null)
    const [reason, setReason] = useState<string | null>(null)

    const inviteValid = useMemo(() => !error && !reason && role && collectionId, [error, reason, role, collectionId])

    useEffect(() => {
        if (!token) return
        const load = async () => {
            setStatus("loading")
            setError(null)
            try {
                const res = await validateCollectionInvite(token)
                if (!res.valid || !res.collectionId) {
                    setReason(res.reason || "Invite is not valid")
                    setStatus("ready")
                    return
                }
                setRole(res.role || null)
                setCollectionId(res.collectionId)
                setStatus("ready")
            } catch (err: any) {
                setError(err?.message || "Failed to validate invite")
                setStatus("ready")
            }
        }
        void load()
    }, [token])

    useEffect(() => {
        if (authLoading) return
        if (!user) {
            const params = new URLSearchParams()
            params.set("returnTo", `/invites/collection/${token}`)
            navigate(`/login?${params.toString()}`, {replace: true})
        }
    }, [authLoading, navigate, token, user])

    const handleAccept = async () => {
        if (!token || !collectionId) return
        setStatus("accepting")
        setError(null)
        try {
            await acceptCollectionInvite(token)
            setStatus("accepted")
            navigate(`/collections/${collectionId}`, {replace: true})
        } catch (err: any) {
            setError(err?.message || "Failed to accept invite")
            setStatus("ready")
        }
    }

    if (!token) return <ErrorState title="Invalid invite" message="Missing invite token"/>
    if (authLoading) return <LoadingState message="Checking session..."/>

    return (
        <div className="flex items-center justify-center mt-12">
            <Card className="max-w-lg w-full">
                <CardHeader>
                    <CardTitle className="text-2xl font-bold uppercase">Accept Collection Invite</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                    {status === "loading" && (
                        <div className="flex items-center gap-2 text-muted-foreground">
                            <Loader2 className="w-5 h-5 animate-spin"/>
                            <span>Validating invite...</span>
                        </div>
                    )}

                    {error && (
                        <Alert variant="destructive">
                            <AlertDescription>{error}</AlertDescription>
                        </Alert>
                    )}

                    {reason && (
                        <Alert>
                            <AlertDescription>{reason}</AlertDescription>
                        </Alert>
                    )}

                    {status === "ready" && inviteValid && (
                        <div className="space-y-2">
                            <p>You have been invited to a collection.</p>
                            <p>
                                Role: <strong className="uppercase">{role}</strong>
                            </p>
                        </div>
                    )}

                    {status === "accepted" && (
                        <Alert className="bg-green-50 border-green-200 text-green-800">
                            <AlertDescription>Invite accepted! Redirecting...</AlertDescription>
                        </Alert>
                    )}
                </CardContent>
                <CardFooter className="flex justify-end gap-2">
                    <Button variant="outline" onClick={() => navigate("/")}>
                        Cancel
                    </Button>
                    <Button disabled={!inviteValid || status === "accepting"} onClick={() => void handleAccept()}>
                        {status === "accepting" ? (
                            <>
                                <Loader2 className="w-4 h-4 mr-2 animate-spin"/>
                                Accepting...
                            </>
                        ) : (
                            "Accept invite"
                        )}
                    </Button>
                </CardFooter>
            </Card>
        </div>
    )
}
