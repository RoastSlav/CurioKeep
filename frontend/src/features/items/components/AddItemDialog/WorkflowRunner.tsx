import { useEffect, useMemo, useState } from "react";
import { Alert, Box, LinearProgress, Stack, Typography } from "@mui/material";
import type { Item, ModuleDefinition, ProviderLookupResponse, WorkflowDef } from "../../../../api/types";
import PromptStep from "./steps/PromptStep";
import PromptAnyStep from "./steps/PromptAnyStep";
import LookupMetadataStep from "./steps/LookupMetadataStep";
import ApplyMetadataStep from "./steps/ApplyMetadataStep";
import SaveItemStep from "./steps/SaveItemStep";

export default function WorkflowRunner({
    workflow,
    moduleDefinition,
    moduleId,
    collectionId,
    onComplete,
    onCancel,
    defaultState,
}: {
    workflow: WorkflowDef;
    moduleDefinition: ModuleDefinition | null | undefined;
    moduleId: string;
    collectionId: string;
    defaultState?: string;
    onComplete: (item: Item) => void;
    onCancel?: () => void;
}) {
    const [stepIndex, setStepIndex] = useState(0);
    const [attributes, setAttributes] = useState<Record<string, any>>({});
    const [lookupResult, setLookupResult] = useState<ProviderLookupResponse | null>(null);

    useEffect(() => {
        setStepIndex(0);
        setAttributes({});
        setLookupResult(null);
    }, [workflow]);

    const steps = workflow.steps || [];
    const current = steps[stepIndex];

    const resolveField = (key?: string) => (moduleDefinition?.fields || []).find((f) => f.key === key);

    const goNext = () => setStepIndex((idx) => Math.min(idx + 1, steps.length - 1));
    const goPrev = () => setStepIndex((idx) => Math.max(idx - 1, 0));

    const progress = steps.length ? ((stepIndex + 1) / steps.length) * 100 : 0;

    const handlePromptSubmit = async (vals: Record<string, any>) => {
        setAttributes((prev) => ({ ...prev, ...vals }));
        goNext();
    };

    const handleLookupComplete = (result: ProviderLookupResponse) => {
        setLookupResult(result);
        goNext();
    };

    const handleApply = (attrs: Record<string, any>) => {
        setAttributes(attrs);
        goNext();
    };

    const handleSave = (item: Item) => {
        onComplete(item);
    };

    const content = useMemo(() => {
        if (!current) return <Alert severity="info">No steps defined for this workflow.</Alert>;
        switch (current.type) {
            case "PROMPT": {
                const field = resolveField(current.field);
                if (!field) return <Alert severity="warning">Workflow references missing field {current.field}</Alert>;
                return (
                    <PromptStep field={field} values={attributes} onSubmit={handlePromptSubmit} onCancel={onCancel} />
                );
            }
            case "PROMPT_ANY": {
                const fields = (current.fields || [])
                    .map((key) => resolveField(key))
                    .filter(Boolean) as NonNullable<ModuleDefinition["fields"]>;
                return (
                    <PromptAnyStep
                        fields={fields.length ? fields : moduleDefinition?.fields || []}
                        values={attributes}
                        label={current.label}
                        onSubmit={handlePromptSubmit}
                        onCancel={onCancel}
                    />
                );
            }
            case "LOOKUP_METADATA":
                return (
                    <LookupMetadataStep
                        moduleDefinition={moduleDefinition}
                        moduleId={moduleId}
                        attributes={attributes}
                        providers={current.providers}
                        onComplete={handleLookupComplete}
                        onBack={stepIndex > 0 ? goPrev : undefined}
                        onAttributesChange={(next) => setAttributes(next)}
                    />
                );
            case "APPLY_METADATA":
                return (
                    <ApplyMetadataStep
                        moduleDefinition={moduleDefinition}
                        lookup={lookupResult}
                        attributes={attributes}
                        onApply={handleApply}
                        onSkip={goNext}
                    />
                );
            case "SAVE_ITEM":
                return (
                    <SaveItemStep
                        collectionId={collectionId}
                        moduleId={moduleId}
                        attributes={attributes}
                        defaultState={defaultState}
                        moduleDefinition={moduleDefinition}
                        onSaved={handleSave}
                        onBack={stepIndex > 0 ? goPrev : undefined}
                    />
                );
            default:
                return <Alert severity="warning">Unsupported step {current.type}</Alert>;
        }
    }, [current, attributes, moduleDefinition, moduleId, collectionId, lookupResult, stepIndex]);

    return (
        <Stack spacing={2}>
            <Box>
                <Typography variant="subtitle1" fontWeight={700}>
                    {workflow.label || workflow.key}
                </Typography>
                <LinearProgress variant="determinate" value={progress} sx={{ mt: 1 }} />
            </Box>
            {content}
        </Stack>
    );
}
