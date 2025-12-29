import { useState } from "react";
import { Button } from "../../../../../../components/ui/button";
import { Input } from "../../../../../../components/ui/input";
import { Alert, AlertDescription } from "../../../../../../components/ui/alert";

export default function QueryPromptStep({
  label,
  placeholder,
  initialValue,
  onSubmit,
  onCancel,
}: {
  label?: string;
  placeholder?: string;
  initialValue?: string;
  onSubmit: (value: string) => void;
  onCancel?: () => void;
}) {
  const [value, setValue] = useState(initialValue ?? "");
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = () => {
    const v = value.trim();
    if (!v) {
      setError("Please enter a search query.");
      return;
    }
    setError(null);
    onSubmit(v);
  };

  return (
    <div className="space-y-3">
      <h3 className="text-sm font-bold uppercase">{label || "Search"}</h3>
      <Input
        value={value}
        placeholder={placeholder || "Enter search text"}
        onChange={(e) => setValue(e.target.value)}
      />
      {error && (
        <Alert variant="destructive">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}
      <div className="flex items-center gap-2">
        {onCancel && (
          <Button variant="outline" onClick={onCancel}>
            Back
          </Button>
        )}
        <Button onClick={handleSubmit}>Next</Button>
      </div>
    </div>
  );
}
