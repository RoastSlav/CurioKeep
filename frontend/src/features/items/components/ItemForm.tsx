import type { Attributes } from "@/features/items/itemTypes";
import type { ModuleContract } from "@/features/modules/moduleTypes";
import DynamicForm from "../../forms/DynamicForm";

export default function ItemForm({
  moduleDefinition,
  initialAttributes,
  onSubmit,
  onCancel,
  disabled,
  cancelLabel,
  submitLabel = "Save",
}: {
  moduleDefinition: ModuleContract | null | undefined;
  initialAttributes?: Attributes;
  onSubmit: (attributes: Attributes) => void | Promise<void>;
  onCancel?: () => void;
  disabled?: boolean;
  cancelLabel?: string;
  submitLabel?: string;
}) {
  return (
    <DynamicForm
      moduleDefinition={moduleDefinition || undefined}
      initialValues={initialAttributes}
      disabled={disabled}
      submitLabel={submitLabel}
      cancelLabel={cancelLabel}
      onSubmit={onSubmit}
      onCancel={onCancel}
    />
  );
}
