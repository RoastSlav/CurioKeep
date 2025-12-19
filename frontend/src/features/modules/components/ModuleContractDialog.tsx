import { Box, Button, Dialog, DialogActions, DialogContent, DialogTitle, Stack, Typography } from "@mui/material";
import { useMemo, useState } from "react";
import type { ModuleContract } from "../api/modulesApi";

export default function ModuleContractDialog({
    open,
    moduleKey,
    contract,
    onClose,
}: {
    open: boolean;
    moduleKey?: string;
    contract?: ModuleContract;
    onClose: () => void;
}) {
    const [copied, setCopied] = useState(false);
    const rawContract = useMemo(() => {
        if (!contract) return "";
        // prefer rawContract if provider included it, otherwise stringify
        // @ts-ignore
        return (contract as any).rawContract ?? JSON.stringify(contract, null, 2);
    }, [contract]);
    const canCopy = !!rawContract;

    const handleCopy = async () => {
        if (!rawContract) return;
        try {
            await navigator.clipboard.writeText(rawContract);
            setCopied(true);
            setTimeout(() => setCopied(false), 1500);
        } catch (error) {
            console.error("Copy failed", error);
        }
    };

    return (
        <Dialog open={open} onClose={onClose} maxWidth="lg" fullWidth>
            <DialogTitle>{moduleKey ? `${moduleKey} Contract` : "Contract"}</DialogTitle>
            <DialogContent>
                <Stack spacing={2}>
                    <Typography variant="body2" color="text.secondary">
                        The raw contract is generated directly from the XML and may contain private values.
                    </Typography>
                    <Box component="pre" sx={{ whiteSpace: "pre-wrap" }}>
                        {rawContract || "<no contract available>"}
                    </Box>
                </Stack>
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose}>Close</Button>
                <Button disabled={!canCopy} onClick={handleCopy}>
                    {copied ? "Copied" : "Copy"}
                </Button>
            </DialogActions>
        </Dialog>
    );
}
