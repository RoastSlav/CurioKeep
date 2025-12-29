import { useMemo, useState } from "react";
import type {
  ModuleDefinition,
  ProviderLookupResponse,
} from "../../../../../api/types";
import { Button } from "../../../../../../components/ui/button";
import { Alert, AlertDescription } from "../../../../../../components/ui/alert";
import { Separator } from "../../../../../../components/ui/separator";

export default function ApplyMetadataStep({
  moduleDefinition,
  lookup,
  attributes,
  onApply,
  onSkip,
  onBack,
}: {
  moduleDefinition: ModuleDefinition | null | undefined;
  lookup: ProviderLookupResponse | null;
  attributes: Record<string, any>;
  onApply: (attrs: Record<string, any>) => void;
  onSkip: () => void;
  onBack?: () => void;
}) {
  if (!lookup) {
    return (
      <div className="space-y-3">
        <Alert variant="default">
          <AlertDescription>Run metadata lookup first.</AlertDescription>
        </Alert>
        <div className="flex justify-end">
          <Button variant="outline" onClick={onSkip}>
            Skip
          </Button>
        </div>
      </div>
    );
  }

  const suggestions = lookup.mergedAttributes || {};
  const fields = moduleDefinition?.fields ?? [];

  const [selectedKeys, setSelectedKeys] = useState<Set<string>>(new Set());

  const diffEntries = useMemo(
    () =>
      Object.entries<Record<string, unknown>>(suggestions).filter(
        ([key, val]) => {
          const current = attributes[key];
          return current !== val;
        }
      ),
    [suggestions, attributes]
  );

  const toggleSelection = (key: string) => {
    setSelectedKeys((prev) => {
      const next = new Set(prev);
      if (next.has(key)) next.delete(key);
      else next.add(key);
      return next;
    });
  };

  const handleApplyAll = () => {
    const merged = { ...attributes, ...suggestions };
    onApply(merged);
  };

  const handleApplySelected = () => {
    if (!selectedKeys.size) return;
    const selectedEntries = diffEntries.filter(([key]) =>
      selectedKeys.has(key)
    );
    const partial = Object.fromEntries(selectedEntries);
    const merged = { ...attributes, ...partial };
    onApply(merged);
  };

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between gap-2">
        <h3 className="text-sm font-bold uppercase">Apply provider data</h3>
      </div>

      {!diffEntries.length ? (
        <Alert variant="default">
          <AlertDescription>No new data to apply.</AlertDescription>
        </Alert>
      ) : (
        <div className="space-y-4">
          {diffEntries.map(([key, val], idx) => {
            const field = fields.find((f) => f.key === key);
            const selected = selectedKeys.has(key);
            return (
              <div key={key} className="space-y-2">
                <div className="flex items-start justify-between gap-4">
                  <div className="space-y-1 text-sm">
                    <div className="font-bold uppercase">
                      {field?.label || key}
                    </div>
                    <div className="text-muted-foreground">
                      Current: {String(attributes[key] ?? "-")}
                    </div>
                    <div>Suggested: {String(val)}</div>
                  </div>
                  <Button
                    variant={selected ? "default" : "outline"}
                    size="sm"
                    onClick={() => toggleSelection(key)}
                  >
                    {selected ? "Selected" : "Apply"}
                  </Button>
                </div>
                {idx < diffEntries.length - 1 && <Separator />}
              </div>
            );
          })}
        </div>
      )}

      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-wrap gap-2">
          <Button variant="outline" onClick={onSkip}>
            Skip
          </Button>
          <Button
            variant="outline"
            onClick={handleApplyAll}
            disabled={!diffEntries.length}
          >
            Apply all
          </Button>
        </div>

        <div className="flex items-center gap-2">
          {onBack && (
            <Button variant="outline" onClick={onBack}>
              Back
            </Button>
          )}
          <Button
            variant="default"
            onClick={handleApplySelected}
            disabled={!selectedKeys.size}
          >
            Next
          </Button>
        </div>
      </div>
    </div>
  );
}
