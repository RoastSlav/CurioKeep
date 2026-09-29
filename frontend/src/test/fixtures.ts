import type { FieldContract } from "@/features/modules/moduleTypes"

export function field(overrides: Partial<FieldContract> & Pick<FieldContract, "key">): FieldContract {
    return {
        label: overrides.key,
        type: "TEXT",
        required: false,
        searchable: false,
        filterable: false,
        sortable: false,
        order: 0,
        active: true,
        deprecated: false,
        identifiers: [],
        enumValues: [],
        providerMappings: [],
        ...overrides,
    }
}
