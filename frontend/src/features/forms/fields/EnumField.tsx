"use client";

import { Label } from "../../../../components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../../../../components/ui/select";
import { Badge } from "../../../../components/ui/badge";
import type { FieldDef } from "../../../api/types";

export default function EnumFieldField({
  field,
  value,
  error,
  disabled,
  onChange,
  onBlur,
}: {
  field: FieldDef;
  value: any;
  error?: string;
  disabled?: boolean;
  onChange: (value: any) => void;
  onBlur?: () => void;
}) {
  const multi = Boolean(field.constraints?.multi);
  const current = multi ? (Array.isArray(value) ? value : []) : value ?? "";
  const id = `field-${field.key}`;

  // For multi-select, we use a simple toggle approach
  const handleMultiToggle = (optKey: string) => {
    const currentArray = Array.isArray(value) ? value : [];
    if (currentArray.includes(optKey)) {
      onChange(currentArray.filter((v: string) => v !== optKey));
    } else {
      onChange([...currentArray, optKey]);
    }
  };

  if (multi) {
    return (
      <div className="flex flex-col gap-2">
        <Label className="text-sm font-semibold text-foreground">
          {field.label || field.key}
          {field.flags?.required && (
            <span className="text-destructive ml-1">*</span>
          )}
        </Label>
        <div className="flex flex-wrap gap-2 p-3 brutal-border rounded-md bg-card min-h-[44px]">
          {(field.enumValues || []).map((opt) => {
            const isSelected = current.includes(opt.key);
            return (
              <Badge
                key={opt.key}
                variant={isSelected ? "default" : "outline"}
                className={`cursor-pointer transition-all brutal-border ${
                  isSelected
                    ? "bg-primary text-primary-foreground"
                    : "bg-card text-card-foreground hover:bg-muted"
                } ${disabled ? "opacity-50 cursor-not-allowed" : ""}`}
                onClick={() => !disabled && handleMultiToggle(opt.key)}
              >
                {opt.label || opt.key}
              </Badge>
            );
          })}
        </div>
        {(error || field.ui?.helpText) && (
          <p
            className={`text-xs ${
              error ? "text-destructive" : "text-muted-foreground"
            }`}
          >
            {error || field.ui?.helpText}
          </p>
        )}
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-2">
      <Label htmlFor={id} className="text-sm font-semibold text-foreground">
        {field.label || field.key}
        {field.flags?.required && (
          <span className="text-destructive ml-1">*</span>
        )}
      </Label>
      <Select value={current} onValueChange={onChange} disabled={disabled}>
        <SelectTrigger
          id={id}
          className={`w-full brutal-border ${
            error ? "border-destructive" : ""
          } bg-card text-card-foreground`}
          onBlur={onBlur}
        >
          <SelectValue placeholder={field.ui?.placeholder || "Select..."} />
        </SelectTrigger>
        <SelectContent className="brutal-border bg-card">
          {(field.enumValues || []).map((opt) => (
            <SelectItem
              key={opt.key}
              value={opt.key}
              className="text-card-foreground hover:bg-muted"
            >
              {opt.label || opt.key}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
      {(error || field.ui?.helpText) && (
        <p
          className={`text-xs ${
            error ? "text-destructive" : "text-muted-foreground"
          }`}
        >
          {error || field.ui?.helpText}
        </p>
      )}
    </div>
  );
}
