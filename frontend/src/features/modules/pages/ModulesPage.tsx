import { Box, Container, Drawer, Paper, Skeleton, Stack, Typography, useMediaQuery, useTheme } from "@mui/material";
import { useCallback, useEffect, useMemo, useState } from "react";
import ErrorState from "../../../components/ErrorState";
import type { ModuleDetails, ModuleSummary } from "../api/modulesApi";
import { getModuleDetails, getModuleRawXml, listModules } from "../api/modulesApi";
import ModuleContractDialog from "../components/ModuleContractDialog";
import ModuleDetailsPanel from "../components/ModuleDetailsPanel";
import ModuleList from "../components/ModuleList";
import ModuleRawXmlDialog from "../components/ModuleRawXmlDialog";

type RawDialogState = {
    open: boolean;
    moduleKey?: string;
    xml?: string;
    loading: boolean;
};

const listSkeleton = (
    <Stack spacing={1} sx={{ mt: 1 }}>
        <Skeleton height={36} />
        <Skeleton height={36} width="80%" />
        <Skeleton height={36} width="60%" />
    </Stack>
);

export default function ModulesPage() {
    const theme = useTheme();
    const isMobile = useMediaQuery(theme.breakpoints.down("md"));

    const [modules, setModules] = useState<ModuleSummary[]>([]);
    const [listError, setListError] = useState<string | null>(null);
    const [listLoading, setListLoading] = useState(true);
    const [selectedKey, setSelectedKey] = useState<string | null>(null);
    const [detailsMap, setDetailsMap] = useState<Record<string, ModuleDetails>>({});
    const [detailsLoading, setDetailsLoading] = useState<Record<string, boolean>>({});
    const [detailsError, setDetailsError] = useState<string | null>(null);

    const [rawDialog, setRawDialog] = useState<RawDialogState>({ open: false, loading: false });
    const [contractDialogOpen, setContractDialogOpen] = useState(false);
    const [mobilePanelOpen, setMobilePanelOpen] = useState(false);

    useEffect(() => {
        let canceled = false;
        setListLoading(true);
        listModules()
            .then((data) => {
                if (canceled) return;
                setModules(data);
                if (!data.length) return;
                setSelectedKey((prev) => prev ?? data[0].moduleKey);
            })
            .catch((error) => {
                console.error(error);
                setListError(error?.message || "Failed to load modules");
            })
            .finally(() => {
                if (canceled) return;
                setListLoading(false);
            });
        return () => {
            canceled = true;
        };
    }, []);

    useEffect(() => {
        const missingKeys = modules
            .map((module) => module.moduleKey)
            .filter((key) => !detailsMap[key] && !detailsLoading[key]);
        if (!missingKeys.length) return;

        setDetailsLoading((prev) => {
            const next = { ...prev };
            missingKeys.forEach((key) => {
                next[key] = true;
            });
            return next;
        });

        let canceled = false;
        Promise.allSettled(missingKeys.map((key) => getModuleDetails(key)))
            .then((results) => {
                if (canceled) return;
                setDetailsMap((prev) => {
                    const next = { ...prev };
                    results.forEach((result, idx) => {
                        if (result.status === "fulfilled") {
                            next[missingKeys[idx]] = result.value;
                        } else {
                            console.error("Failed to load module details", missingKeys[idx], result.reason);
                            setDetailsError("Failed to load some module details");
                        }
                    });
                    return next;
                });
            })
            .finally(() => {
                if (canceled) return;
                setDetailsLoading((prev) => {
                    const next = { ...prev };
                    missingKeys.forEach((key) => {
                        delete next[key];
                    });
                    return next;
                });
            });
        return () => {
            canceled = true;
        };
    }, [modules, detailsMap, detailsLoading]);

    const selectedModule = useMemo(() => (selectedKey ? detailsMap[selectedKey] : undefined), [selectedKey, detailsMap]);
    const selectedSummary = useMemo(
        () => modules.find((module) => module.moduleKey === selectedKey),
        [modules, selectedKey]
    );
    const selectedLoading = selectedKey ? Boolean(detailsLoading[selectedKey]) : false;

    const handleSelect = useCallback(
        (moduleKey: string) => {
            setSelectedKey(moduleKey);
            if (isMobile) {
                setMobilePanelOpen(true);
            }
        },
        [isMobile]
    );

    const handleRaw = useCallback(async (moduleKey: string) => {
        setRawDialog({ open: true, moduleKey, loading: true });
        try {
            const payload = await getModuleRawXml(moduleKey);
            setRawDialog({ open: true, moduleKey, xml: payload.xmlRaw, loading: false });
        } catch (error) {
            console.error(error);
            setRawDialog({ open: true, moduleKey, xml: "Failed to load XML", loading: false });
        }
    }, []);

    const handleCloseRaw = useCallback(() => {
        setRawDialog({ open: false, loading: false });
    }, []);

    const handleOpenContract = useCallback(() => {
        setContractDialogOpen(true);
    }, []);

    const handleCloseContract = useCallback(() => {
        setContractDialogOpen(false);
    }, []);

    const handleModuleDeleted = useCallback((moduleKey: string) => {
        setModules((prev) => {
            const nextModules = prev.filter((module) => module.moduleKey !== moduleKey);
            setSelectedKey((current) => (current === moduleKey ? nextModules[0]?.moduleKey ?? null : current));
            return nextModules;
        });
        setDetailsMap((prev) => {
            const next = { ...prev };
            delete next[moduleKey];
            return next;
        });
        setContractDialogOpen(false);
        setMobilePanelOpen(false);
    }, []);

    const modulesEmpty = !listLoading && !modules.length && !listError;

    return (
        <Container maxWidth="xl" sx={{ py: { xs: 2, md: 4 } }}>
            <Stack spacing={1} mb={3}>
                <Typography variant="h4" fontWeight={600} color="accent.main">
                    Module catalog
                </Typography>
                <Typography variant="body2" color="secondary.main">
                    {modules.length} module{modules.length === 1 ? "" : "s"} · {listLoading ? "Loading catalog…" : "Freshly synced"}
                </Typography>
            </Stack>

            {listError && (
                <Box mb={3}>
                    <ErrorState message={listError} />
                </Box>
            )}

            <Box
                sx={{
                    display: "grid",
                    gridTemplateColumns: { xs: "1fr", md: "minmax(0, 400px) minmax(0, 1fr)" },
                    gap: 3,
                }}
            >
                <Paper
                    elevation={0}
                    sx={{
                        p: 3,
                        borderRadius: 3,
                        minWidth: 0,
                        display: "flex",
                        flexDirection: "column",
                        gap: 2,
                    }}
                >
                    <Stack spacing={0.5}>
                        <Typography variant="subtitle1" fontWeight={600} color="primary.main">
                            Installed modules
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                            Search, filter, and peek at existing contracts.
                        </Typography>
                    </Stack>
                    {listLoading && !modules.length ? listSkeleton : null}
                    <Box sx={{ flex: 1, minWidth: 0, overflow: "hidden" }}>
                        <ModuleList
                            modules={modules}
                            details={detailsMap}
                            selectedKey={selectedKey ?? undefined}
                            onSelect={handleSelect}
                            onViewXml={handleRaw}
                        />
                    </Box>
                </Paper>

                <Box sx={{ minWidth: 0 }}>
                    {modulesEmpty ? (
                        <Paper
                            sx={{
                                p: 4,
                                borderRadius: 3,
                                bgcolor: "secondary.light",
                            }}
                        >
                            <Stack spacing={1}>
                                <Typography variant="h6" color="accent.main">
                                    No modules found
                                </Typography>
                                <Typography color="text.secondary">
                                    This workspace has no modules yet. Head to the admin import flow to add curated contract packages.
                                </Typography>
                            </Stack>
                        </Paper>
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
                </Box>
            </Box>

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
            <Drawer anchor="right" open={isMobile && mobilePanelOpen} onClose={() => setMobilePanelOpen(false)}>
                <Box sx={{ width: { xs: "100vw", sm: 500 }, p: 2 }}>{selectedSummary && (
                    <ModuleDetailsPanel
                        module={selectedModule}
                        summary={selectedSummary}
                        loading={selectedLoading}
                        error={detailsError ?? undefined}
                        onViewRaw={() => selectedModule && handleRaw(selectedModule.moduleKey)}
                        onViewContract={selectedModule ? handleOpenContract : undefined}
                        onModuleDeleted={handleModuleDeleted}
                    />
                )}</Box>
            </Drawer>
        </Container>
    );
}
