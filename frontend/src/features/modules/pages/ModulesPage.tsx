"use client"
import {useCallback, useEffect, useMemo, useState} from "react"
import ErrorState from "../../../components/ErrorState"
import type {ModuleDetails, ModuleSummary} from "../api/modulesApi"
import {getModuleDetails, getModuleRawXml, listModules} from "../api/modulesApi"
import ModuleContractDialog from "../components/ModuleContractDialog"
import ModuleDetailsPanel from "../components/ModuleDetailsPanel"
import ModuleList from "../components/ModuleList"
import ModuleRawXmlDialog from "../components/ModuleRawXmlDialog"
import ImportModuleDialog from "../components/ImportModuleDialog"
import ScanModulesDialog from "../components/ScanModulesDialog"
import {useAuth} from "../../../auth/useAuth"
import {Skeleton} from "../../../../components/ui/skeleton"
import {Button} from "../../../../components/ui/button"
import {FileUp, FolderSearch} from "lucide-react"

type RawDialogState = {
    open: boolean
    moduleKey?: string
    xml?: string
    loading: boolean
}

export default function ModulesPage() {
    const [modules, setModules] = useState<ModuleSummary[]>([])
    const [listError, setListError] = useState<string | null>(null)
    const [listLoading, setListLoading] = useState(true)
    const [selectedKey, setSelectedKey] = useState<string | null>(null)
    const [detailsMap, setDetailsMap] = useState<Record<string, ModuleDetails>>({})
    const [detailsLoading, setDetailsLoading] = useState<Record<string, boolean>>({})
    const [detailsError, setDetailsError] = useState<string | null>(null)

    const [rawDialog, setRawDialog] = useState<RawDialogState>({open: false, loading: false})
    const [contractDialogOpen, setContractDialogOpen] = useState(false)

    const {user} = useAuth()
    const [importDialogOpen, setImportDialogOpen] = useState(false)
    const [scanDialogOpen, setScanDialogOpen] = useState(false)
    const isAdmin = Boolean(user?.isAdmin)

    useEffect(() => {
        let canceled = false
        setListLoading(true)
        listModules()
            .then((data) => {
                if (canceled) return
                console.log("[v0] Modules list loaded:", data)
                setModules(data)
                if (!data.length) return
                setSelectedKey((prev) => prev ?? data[0].moduleKey)
            })
            .catch((error) => {
                console.error(error)
                setListError(error?.message || "Failed to load modules")
            })
            .finally(() => {
                if (canceled) return
                setListLoading(false)
            })
        return () => {
            canceled = true
        }
    }, [])

    useEffect(() => {
        const missingKeys = modules
            .map((module) => module.moduleKey)
            .filter((key) => !detailsMap[key] && !detailsLoading[key])

        if (!missingKeys.length) return

        console.log("[v0] Loading details for modules:", missingKeys)
        console.log("[v0] Current detailsMap keys:", Object.keys(detailsMap))
        console.log("[v0] Current detailsLoading:", detailsLoading)

        setDetailsLoading((prev) => {
            const next = {...prev}
            missingKeys.forEach((key) => {
                next[key] = true
            })
            console.log("[v0] Updated detailsLoading:", next)
            return next
        })

        const loadDetails = async () => {
            try {
                const results = await Promise.allSettled(missingKeys.map((key) => getModuleDetails(key)))

                console.log("[v0] Promise resolved, processing results:", results)

                const newDetails: Record<string, ModuleDetails> = {}
                results.forEach((result, idx) => {
                    const key = missingKeys[idx]
                    if (result.status === "fulfilled") {
                        newDetails[key] = result.value
                        console.log(`[v0] Successfully loaded ${key}:`, result.value)
                    } else {
                        console.error(`[v0] Failed to load ${key}:`, result.reason)
                        setDetailsError("Failed to load some module details")
                    }
                })

                console.log("[v0] About to update detailsMap with:", newDetails)

                setDetailsMap((prev) => {
                    const next = {...prev, ...newDetails}
                    console.log("[v0] detailsMap updated, keys:", Object.keys(next))
                    return next
                })

                setDetailsLoading((prev) => {
                    const next = {...prev}
                    missingKeys.forEach((key) => {
                        next[key] = false
                    })
                    return next
                })
            } catch (error) {
                console.error("[v0] Error in loadDetails:", error)
                setDetailsError("Failed to load module details")
                setDetailsLoading((prev) => {
                    const next = {...prev}
                    missingKeys.forEach((key) => {
                        next[key] = false
                    })
                    return next
                })
            }
        }

        loadDetails()
    }, [modules, detailsMap, detailsLoading])

    const selectedModule = useMemo(() => {
        const module = selectedKey ? detailsMap[selectedKey] : undefined
        console.log("[v0] selectedModule useMemo:", {
            selectedKey,
            hasModule: !!module,
            detailsMapKeys: Object.keys(detailsMap),
            moduleData: module,
        })
        return module
    }, [selectedKey, detailsMap])

    const selectedSummary = useMemo(
        () => modules.find((module) => module.moduleKey === selectedKey),
        [modules, selectedKey],
    )

    const selectedLoading = useMemo(() => {
        const loading = selectedKey ? Boolean(detailsLoading[selectedKey]) : false
        console.log("[v0] selectedLoading useMemo:", {
            selectedKey,
            loading,
            detailsLoadingState: detailsLoading[selectedKey ?? ""],
        })
        return loading
    }, [selectedKey, detailsLoading])

    const handleSelect = useCallback((moduleKey: string) => {
        console.log("[v0] Module selected:", moduleKey)
        setSelectedKey(moduleKey)
    }, [])

    const handleRaw = useCallback(async (moduleKey: string) => {
        setRawDialog({open: true, moduleKey, loading: true})
        try {
            const payload = await getModuleRawXml(moduleKey)
            setRawDialog({open: true, moduleKey, xml: payload.xmlRaw, loading: false})
        } catch (error) {
            console.error(error)
            setRawDialog({open: true, moduleKey, xml: "Failed to load XML", loading: false})
        }
    }, [])

    const handleCloseRaw = useCallback(() => {
        setRawDialog({open: false, loading: false})
    }, [])

    const handleOpenContract = useCallback(() => {
        setContractDialogOpen(true)
    }, [])

    const handleCloseContract = useCallback(() => {
        setContractDialogOpen(false)
    }, [])

    const handleModuleDeleted = useCallback((moduleKey: string) => {
        setModules((prev) => {
            const nextModules = prev.filter((module) => module.moduleKey !== moduleKey)
            setSelectedKey((current) => (current === moduleKey ? (nextModules[0]?.moduleKey ?? null) : current))
            return nextModules
        })
        setDetailsMap((prev) => {
            const next = {...prev}
            delete next[moduleKey]
            return next
        })
        setContractDialogOpen(false)
    }, [])

    const handleRefreshModules = useCallback(async () => {
        setListLoading(true)
        try {
            const data = await listModules()
            setModules(data)
            if (!data.length) return
            setSelectedKey((prev) => prev ?? data[0].moduleKey)
        } catch (error) {
            console.error(error)
            setListError("Failed to refresh modules")
        } finally {
            setListLoading(false)
        }
    }, [])

    const handleModuleImported = useCallback(() => {
        handleRefreshModules()
        setImportDialogOpen(false)
    }, [handleRefreshModules])

    const handleModulesScanned = useCallback(() => {
        handleRefreshModules()
    }, [handleRefreshModules])

    const modulesEmpty = !listLoading && !modules.length && !listError

    return (
        <div className="space-y-6">
            <div className="space-y-2">
                <div className="flex flex-wrap items-center justify-between gap-4">
                    <div>
                        <h1 className="text-3xl md:text-4xl font-bold tracking-tight uppercase">MODULE CATALOG</h1>
                        <p className="text-base text-muted-foreground">
                            {modules.length} module{modules.length === 1 ? "" : "s"} ·{" "}
                            {listLoading ? "Loading catalog…" : "Freshly synced"}
                        </p>
                    </div>
                    {isAdmin && (
                        <div className="flex flex-wrap gap-2">
                            <Button
                                onClick={() => setImportDialogOpen(true)}
                                className="brutal-border brutal-shadow-sm bg-primary text-primary-foreground hover:bg-primary/90 uppercase"
                            >
                                <FileUp className="h-4 w-4 mr-2"/>
                                Import XML
                            </Button>
                            <Button
                                onClick={() => setScanDialogOpen(true)}
                                className="brutal-border brutal-shadow-sm bg-secondary text-secondary-foreground hover:bg-secondary/90 uppercase"
                            >
                                <FolderSearch className="h-4 w-4 mr-2"/>
                                Scan Folder
                            </Button>
                        </div>
                    )}
                </div>
            </div>

            {listError && (
                <div className="mb-6">
                    <ErrorState message={listError}/>
                </div>
            )}

            <div className="grid grid-cols-1 lg:grid-cols-[minmax(0,400px)_minmax(0,1fr)] gap-6">
                <div className="brutal-border brutal-shadow-sm bg-card">
                    <div className="p-6 border-b-4 border-border">
                        <h2 className="text-xl font-bold uppercase">Installed modules</h2>
                        <p className="text-sm text-muted-foreground mt-1">Search, filter, and peek at existing
                            contracts.</p>
                    </div>
                    <div className="p-6">
                        {listLoading && !modules.length ? (
                            <div className="space-y-2">
                                <Skeleton className="h-10 w-full"/>
                                <Skeleton className="h-10 w-4/5"/>
                                <Skeleton className="h-10 w-3/5"/>
                            </div>
                        ) : (
                            <ModuleList
                                modules={modules}
                                details={detailsMap}
                                selectedKey={selectedKey ?? undefined}
                                onSelect={handleSelect}
                                onViewXml={handleRaw}
                            />
                        )}
                    </div>
                </div>

                <div>
                    {modulesEmpty ? (
                        <div className="brutal-border brutal-shadow-sm bg-card p-6">
                            <div className="space-y-3">
                                <h3 className="text-xl font-bold uppercase">No modules found</h3>
                                <p className="text-muted-foreground">
                                    This workspace has no modules yet. Head to the admin import flow to add curated
                                    contract packages.
                                </p>
                            </div>
                        </div>
                    ) : (
                        <ModuleDetailsPanel
                            module={selectedModule}
                            summary={selectedSummary}
                            loading={selectedLoading}
                            error={detailsError ?? undefined}
                            onViewRaw={() => selectedModule && handleRaw(selectedModule.moduleKey)}
                            onViewContract={selectedModule ? handleOpenContract : undefined}
                            onModuleDeleted={handleModuleDeleted}
                        />
                    )}
                </div>
            </div>

            <ModuleRawXmlDialog
                open={rawDialog.open}
                moduleKey={rawDialog.moduleKey}
                xml={rawDialog.xml}
                loading={rawDialog.loading}
                onClose={handleCloseRaw}
            />
            <ModuleContractDialog
                open={contractDialogOpen}
                moduleKey={selectedModule?.moduleKey}
                contract={selectedModule?.contract}
                onClose={handleCloseContract}
            />
            {isAdmin && (
                <>
                    <ImportModuleDialog
                        open={importDialogOpen}
                        onClose={() => setImportDialogOpen(false)}
                        onModuleImported={handleModuleImported}
                    />
                    <ScanModulesDialog
                        open={scanDialogOpen}
                        onClose={() => setScanDialogOpen(false)}
                        onModulesScanned={handleModulesScanned}
                    />
                </>
            )}
        </div>
    )
}
