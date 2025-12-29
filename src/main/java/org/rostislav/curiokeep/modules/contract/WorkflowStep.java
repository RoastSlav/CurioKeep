package org.rostislav.curiokeep.modules.contract;

import java.util.List;
import java.util.Map;

public record WorkflowStep(
        WorkflowStepType type,
        String field,
        List<String> fields,
        List<String> providers,
        String query,
        String label,
        Map<String, Object> extensions
) {
    public WorkflowStep {
        fields = fields == null ? List.of() : List.copyOf(fields);
        providers = providers == null ? List.of() : List.copyOf(providers);
        extensions = extensions == null ? Map.of() : Map.copyOf(extensions);
    }
}