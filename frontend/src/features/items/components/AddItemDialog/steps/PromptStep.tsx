import type { FieldDef } from "../../../../../api/types";
import DynamicForm from "../../../../forms/DynamicForm";

export default function PromptStep({
  field,
  values,
  onSubmit,
  onCancel,
}: {
  field: FieldDef;
  values: Record<string, any>;
  onSubmit: (values: Record<string, any>) => void | Promise<void>;
  onCancel?: () => void;
}) {
  return (
    <>
      <h3 className="text-sm font-bold uppercase mb-2">
        {field.label || field.key}
      </h3>
      <DynamicForm
        fields={[field]}
        initialValues={values}
        onSubmit={(attrs: Record<string, any>) => onSubmit(attrs)}
        onCancel={onCancel}
        cancelLabel="Back"
        submitLabel="Next"
      />
    </>
  );
}
