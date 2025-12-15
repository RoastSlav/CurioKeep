package org.rostislav.curiokeep.modules.contract;

import java.util.List;
import java.util.Map;

public record ModuleContract(
        String key,
        String version,
        String name,
        String description,
        ModuleMeta meta,
        List<StateContract> states,
        List<ProviderContract> providers,
        List<FieldContract> fields,
        List<WorkflowContract> workflows,
        Map<String, Object> extensions
) {
    public ModuleContract {
        states = states == null ? List.of() : List.copyOf(states);
        providers = providers == null ? List.of() : List.copyOf(providers);
        fields = fields == null ? List.of() : List.copyOf(fields);
        workflows = workflows == null ? List.of() : List.copyOf(workflows);
        extensions = extensions == null ? Map.of() : Map.copyOf(extensions);
    }
}
