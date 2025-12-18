import type { ModuleDefinition } from "../../../api/types";
import DynamicForm from "../../forms/DynamicForm";

export default function ItemForm({
    moduleDefinition,
    initialAttributes,
    onSubmit,
    onCancel,
    disabled,
    submitLabel = "Save",
}: {
    moduleDefinition: ModuleDefinition | null | undefined;
    initialAttributes?: Record<string, any>;
    onSubmit: (attributes: Record<string, any>) => void | Promise<void>;
    onCancel?: () => void;
    disabled?: boolean;
    submitLabel?: string;
}) {
    return (
        <DynamicForm
            moduleDefinition={moduleDefinition || undefined}
            initialValues={initialAttributes}
            disabled={disabled}
            submitLabel={submitLabel}
            onSubmit={onSubmit}
            onCancel={onCancel}
        />
    );
}
