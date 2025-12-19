"use client";

import { useMemo, useState } from "react";
import type {
  ItemIdentifier,
  ModuleDefinition,
  ProviderLookupResponse,
} from "../../../../../api/types";
import { lookupProviders } from "../../../../providers/api";
import BarcodeScanner from "../BarcodeScanner";
import { Button } from "../../../../../../components/ui/button";
import { Alert, AlertDescription } from "../../../../../../components/ui/alert";
import { Separator } from "../../../../../../components/ui/separator";

function collectIdentifiers(
  moduleDefinition: ModuleDefinition | null | undefined,
  attributes: Record<string, any>
): ItemIdentifier[] {
  if (!moduleDefinition) return [];
  const list: ItemIdentifier[] = [];
  const fields = moduleDefinition.fields ?? [];
  fields.forEach((field) => {
    if (!field.identifiers || !field.identifiers.length) return;
    const val = attributes[field.key];
    if (!val) return;
    field.identifiers.forEach((type) => {
      list.push({ type: type as ItemIdentifier["type"], value: String(val) });
    });
  });
  return list;
}

export default function LookupMetadataStep({
  moduleDefinition,
  moduleId,
  attributes,
  providers,
  onComplete,
  onBack,
  onAttributesChange,
}: {
  moduleDefinition: ModuleDefinition | null | undefined;
  moduleId: string;
  attributes: Record<string, any>;
  providers?: string[];
  onComplete: (response: ProviderLookupResponse) => void;
  onBack?: () => void;
  onAttributesChange: (next: Record<string, any>) => void;
}) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<ProviderLookupResponse | null>(null);
  const identifiers = useMemo(
    () => collectIdentifiers(moduleDefinition, attributes),
    [moduleDefinition, attributes]
  );
  const canLookup = identifiers.length > 0;

  const runLookup = async () => {
    if (!identifiers.length) {
      setError("No identifiers provided yet (e.g., ISBN). Add one to lookup.");
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const response = await lookupProviders({
        moduleId,
        identifiers,
        providers,
      });
      setResult(response);
      onComplete(response);
    } catch (err: any) {
      setError(err?.message || "Lookup failed");
    } finally {
      setLoading(false);
    }
  };

  const handleBarcode = (code: string) => {
    if (!moduleDefinition) return;
    const trimmed = code.trim();
    const idFields = (moduleDefinition.fields || []).filter(
      (f) => f.identifiers?.length
    );
    let target = idFields.find((f) =>
      f.identifiers?.includes(trimmed.length === 13 ? "ISBN13" : "ISBN10")
    );
    if (!target) target = idFields[0];
    if (!target) return;
    onAttributesChange({ ...attributes, [target.key]: trimmed });
  };

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-bold uppercase">Lookup metadata</h3>
      </div>

      {error && (
        <Alert variant="destructive">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      {result && (
        <div className="space-y-2">
          <Alert variant="default">
            <AlertDescription>
              Metadata found. You can apply it next.
            </AlertDescription>
          </Alert>
          {result.providerResults?.length ? (
            <div className="border-2 border-border brutal-shadow-sm rounded-md divide-y divide-border">
              {result.providerResults.map((r) => (
                <div
                  key={r.providerKey}
                  className="flex items-center justify-between px-3 py-2 text-sm"
                >
                  <div className="font-bold uppercase">{r.providerKey}</div>
                  <div
                    className={
                      r.error ? "text-destructive" : "text-muted-foreground"
                    }
                  >
                    {r.error ? `Error: ${r.error}` : "OK"}
                  </div>
                </div>
              ))}
            </div>
          ) : null}
        </div>
      )}

      <Separator className="pt-1" />

      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="flex flex-wrap items-start gap-3">
          <Button
            variant="outline"
            className="min-w-[140px]"
            onClick={runLookup}
            disabled={loading || !canLookup}
          >
            {loading ? "Looking up..." : "Run lookup"}
          </Button>
          <BarcodeScanner
            onDetected={handleBarcode}
            className="space-y-1"
            buttonProps={{
              variant: "outline",
              size: "default",
              className: "min-w-[140px]",
            }}
          />
        </div>

        <div className="flex items-center gap-2">
          {onBack && (
            <Button
              variant="outline"
              size="default"
              className="min-w-[110px]"
              onClick={onBack}
              disabled={loading}
            >
              Back
            </Button>
          )}
          <Button
            variant="default"
            size="default"
            className="min-w-[140px]"
            onClick={runLookup}
            disabled={loading || !canLookup}
          >
            Next
          </Button>
        </div>
      </div>
    </div>
  );
}
