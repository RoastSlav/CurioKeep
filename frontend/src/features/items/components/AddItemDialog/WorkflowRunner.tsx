import type { Attributes } from "@/features/items/itemTypes";
import { useEffect, useMemo, useState } from "react";
import type { Item, ProviderLookupResponse } from "../../../../api/types";
import type { ModuleContract, WorkflowContract } from "@/features/modules/moduleTypes";
import PromptStep from "./steps/PromptStep";
import PromptAnyStep from "./steps/PromptAnyStep";
import LookupMetadataStep from "./steps/LookupMetadataStep";
import ApplyMetadataStep from "./steps/ApplyMetadataStep";
import SaveItemStep from "./steps/SaveItemStep";
import SelectItemImageStep from "../forms/SelectItemImageStep";
import type { SelectedImage } from "../forms/SelectItemImageStep";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Progress } from "@/components/ui/progress";
import QueryPromptStep from "./steps/QueryPromptStep";

export default function WorkflowRunner({
  workflow,
  moduleDefinition,
  moduleId,
  collectionId,
  onComplete,
  onCancel,
  defaultState,
  onStepChange,
}: {
  workflow: WorkflowContract;
  moduleDefinition: ModuleContract | null | undefined;
  moduleId: string;
  collectionId: string;
  defaultState?: string;
  onComplete: (item: Item) => void;
  onCancel?: () => void;
  onStepChange?: (
    stepType: WorkflowContract["steps"][number]["type"] | undefined
  ) => void;
}) {
  const [stepIndex, setStepIndex] = useState(0);
  const [attributes, setAttributes] = useState<Attributes>({});
  const [lookupResult, setLookupResult] =
    useState<ProviderLookupResponse | null>(null);
  const [selectedImage, setSelectedImage] = useState<SelectedImage>(null);

  const steps = useMemo(() => {
    const base = workflow.steps || [];
    const hasSelectImage = base.some((s) => s.type === "SELECT_IMAGE");
    if (hasSelectImage) return base;

    const insert = { type: "SELECT_IMAGE" as const };
    const saveIdx = base.findIndex((s) => s.type === "SAVE_ITEM");
    if (saveIdx >= 0) {
      const clone = [...base];
      clone.splice(saveIdx, 0, insert);
      return clone;
    }
    return [...base, insert];
  }, [workflow]);
  const current = steps[stepIndex];

  const resolveField = (key?: string) =>
    (moduleDefinition?.fields || []).find((f) => f.key === key);

  useEffect(() => {
    onStepChange?.(current?.type);
  }, [current?.type, onStepChange]);

  const goNext = () =>
    setStepIndex((idx) => Math.min(idx + 1, steps.length - 1));
  const goPrev = () => setStepIndex((idx) => Math.max(idx - 1, 0));

  const progress = steps.length ? ((stepIndex + 1) / steps.length) * 100 : 0;

  const handlePromptSubmit = async (vals: Attributes) => {
    setAttributes((prev) => ({ ...prev, ...vals }));
    goNext();
  };

  const handleLookupComplete = (result: ProviderLookupResponse) => {
    setLookupResult(result);
    goNext();
  };

  const handleApply = (attrs: Attributes) => {
    setAttributes(attrs);
    goNext();
  };

  const handleSave = (item: Item) => {
    onComplete(item);
  };

  const lookupAssets = useMemo(() => {
    if (!lookupResult) return [];
    if (lookupResult.assets?.length) return lookupResult.assets;
    if (lookupResult.best?.assets?.length) return lookupResult.best.assets;
    return lookupResult.results?.flatMap((result) => result.assets || []) || [];
  }, [lookupResult]);

  const renderStep = () => {
    if (!current)
      return (
        <Alert>
          <AlertDescription>
            No steps defined for this workflow.
          </AlertDescription>
        </Alert>
      );
    const stepType: string = current.type;
    switch (current.type) {
      case "PROMPT": {
        const field = resolveField(current.field);
        if (field) {
          return (
            <PromptStep
              field={field}
              values={attributes}
              onSubmit={handlePromptSubmit}
              onCancel={onCancel}
            />
          );
        }
        if (current.query) {
          return (
            <QueryPromptStep
              label={current.label || "Search"}
              placeholder={current.query}
              initialValue={typeof attributes.query === "string" ? attributes.query : undefined}
              onSubmit={(val) =>
                handlePromptSubmit({ ...attributes, query: val })
              }
              onCancel={onCancel}
            />
          );
        }
        return (
          <Alert variant="destructive">
            <AlertDescription>
              Workflow references missing field {current.field}
            </AlertDescription>
          </Alert>
        );
      }
      case "PROMPT_ANY": {
        const fields = (current.fields ?? []).flatMap((key) => {
          const field = resolveField(key);
          return field ? [field] : [];
        });
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
            onBack={stepIndex > 0 ? goPrev : undefined}
          />
        );
      case "SELECT_IMAGE":
        return (
          <SelectItemImageStep
            assets={lookupAssets}
            selected={selectedImage}
            onSelect={(val) => setSelectedImage(val)}
            onNext={goNext}
            onBack={stepIndex > 0 ? goPrev : undefined}
          />
        );
      case "SAVE_ITEM":
        return (
          <SaveItemStep
            collectionId={collectionId}
            moduleId={moduleId}
            attributes={attributes}
            selectedImage={selectedImage}
            defaultState={defaultState}
            moduleDefinition={moduleDefinition}
            onSaved={handleSave}
            onBack={stepIndex > 0 ? goPrev : undefined}
          />
        );
      default:
        return (
          <Alert variant="destructive">
            <AlertDescription>
              Unsupported step {stepType}
            </AlertDescription>
          </Alert>
        );
    }
  };

  return (
    <div className="space-y-4">
      <div>
        <h4 className="font-bold">{workflow.label || workflow.key}</h4>
        <Progress value={progress} className="mt-2 h-2" />
      </div>
      {renderStep()}
    </div>
  );
}
