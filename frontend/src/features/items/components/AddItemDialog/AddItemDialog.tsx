import { useEffect, useMemo, useState } from "react";
import {
    Dialog,
    DialogContent,
    DialogTitle,
    Divider,
    List,
    ListItem,
    ListItemButton,
    ListItemText,
    Stack,
    Typography,
} from "@mui/material";
import type { Item, ModuleDefinition, WorkflowDef } from "../../../../api/types";
import WorkflowRunner from "./WorkflowRunner";

function buildFallbackWorkflow(moduleDefinition: ModuleDefinition | null | undefined): WorkflowDef {
    const fields = moduleDefinition?.fields ?? [];
    const fieldKeys = fields.map((f) => f.key);
    return {
        key: "manual_add_fallback",
        label: "Manual add",
        steps: [
            { type: "PROMPT_ANY", fields: fieldKeys },
            { type: "SAVE_ITEM" },
        ],
    };
}

export default function AddItemDialog({
    open,
    onClose,
    moduleDefinition,
    moduleId,
    collectionId,
    defaultState,
    onCreated,
}: {
    open: boolean;
    onClose: () => void;
    moduleDefinition: ModuleDefinition | null | undefined;
    moduleId: string;
    collectionId: string;
    defaultState?: string;
    onCreated?: (item: Item) => void;
}) {
    const workflows = useMemo(() => {
        const base = moduleDefinition?.workflows && moduleDefinition.workflows.length ? moduleDefinition.workflows : [];
        if (base.length) return base;
        return [buildFallbackWorkflow(moduleDefinition)];
    }, [moduleDefinition]);

    const [selectedKey, setSelectedKey] = useState(workflows[0]?.key);

    useEffect(() => {
        setSelectedKey(workflows[0]?.key);
    }, [workflows]);
    const selectedWorkflow = workflows.find((w) => w.key === selectedKey) || workflows[0];

    const handleCreated = (item: Item) => {
        onCreated?.(item);
        onClose();
    };

    return (
        <Dialog open={open} onClose={onClose} fullWidth maxWidth="md">
            <DialogTitle>Add item</DialogTitle>
            <Divider />
            <DialogContent>
                <Stack spacing={3}>
                    <Stack spacing={1}>
                        <Typography variant="subtitle2" fontWeight={700}>
                            Choose workflow
                        </Typography>
                        <List dense>
                            {workflows.map((wf) => (
                                <ListItem key={wf.key} disablePadding>
                                    <ListItemButton selected={wf.key === selectedKey} onClick={() => setSelectedKey(wf.key)}>
                                        <ListItemText primary={wf.label || wf.key} />
                                    </ListItemButton>
                                </ListItem>
                            ))}
                        </List>
                    </Stack>

                    {selectedWorkflow ? (
                        <WorkflowRunner
                            workflow={selectedWorkflow}
                            moduleDefinition={moduleDefinition}
                            moduleId={moduleId}
                            collectionId={collectionId}
                            defaultState={defaultState}
                            onComplete={handleCreated}
                            onCancel={onClose}
                        />
                    ) : (
                        <Typography>No workflow available.</Typography>
                    )}
                </Stack>
            </DialogContent>
        </Dialog>
    );
}
