import type { Attributes } from "@/features/items/itemTypes";
import type { FieldContract } from "@/features/modules/moduleTypes";
import DynamicForm from "../../../../forms/DynamicForm";

export default function PromptStep({
  field,
  values,
  onSubmit,
  onCancel,
}: {
  field: FieldContract;
  values: Attributes;
  onSubmit: (values: Attributes) => void | Promise<void>;
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
        onSubmit={(attrs: Attributes) => onSubmit(attrs)}
        onCancel={onCancel}
        cancelLabel="Back"
        submitLabel="Next"
      />
    </>
  );
}
