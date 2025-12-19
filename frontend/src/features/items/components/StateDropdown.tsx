"use client"

import type {ModuleStateDef} from "../../../api/types"
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from "../../../../components/ui/select"

export default function StateDropdown({
                                          states,
                                          value,
                                          disabled,
                                          onChange,
}: {
    states: ModuleStateDef[]
    value: string
    disabled?: boolean
    onChange: (stateKey: string) => void
}) {
    return (
        <Select value={value} onValueChange={onChange} disabled={disabled}>
            <SelectTrigger className="w-32" onClick={(e) => e.stopPropagation()}>
                <SelectValue/>
            </SelectTrigger>
            <SelectContent>
                {states.map((state) => (
                    <SelectItem key={state.key} value={state.key} disabled={state.deprecated === true}>
                        {state.label || state.key}
                    </SelectItem>
                ))}
            </SelectContent>
        </Select>
    )
}
