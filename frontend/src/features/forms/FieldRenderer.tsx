import type { FieldDef } from "../../api/types";
import BooleanFieldField from "./fields/BooleanField";
import DateFieldField from "./fields/DateField";
import EnumFieldField from "./fields/EnumField";
import JsonFieldField from "./fields/JsonField";
import LinkFieldField from "./fields/LinkField";
import NumberFieldField from "./fields/NumberField";
import TagsFieldField from "./fields/TagsField";
import TextFieldField from "./fields/TextField";

export type FieldRendererProps = {
    field: FieldDef;
    value: any;
    error?: string;
    disabled?: boolean;
    onChange: (value: any) => void;
    onBlur?: () => void;
};

export default function FieldRenderer({ field, ...rest }: FieldRendererProps) {
    if (field.ui?.hidden) return null;

    const commonProps = { field, ...rest };

    switch (field.type) {
        case "NUMBER":
            return <NumberFieldField {...commonProps} />;
        case "DATE":
            return <DateFieldField {...commonProps} />;
        case "BOOLEAN":
            return <BooleanFieldField {...commonProps} />;
        case "ENUM":
            return <EnumFieldField {...commonProps} />;
        case "TAGS":
            return <TagsFieldField {...commonProps} />;
        case "LINK":
            return <LinkFieldField {...commonProps} />;
        case "JSON":
            return <JsonFieldField {...commonProps} />;
        case "TEXT":
        default:
            return <TextFieldField {...commonProps} />;
    }
}
