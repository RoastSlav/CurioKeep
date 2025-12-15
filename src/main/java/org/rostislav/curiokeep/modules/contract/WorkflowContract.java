package org.rostislav.curiokeep.modules.contract;

import java.util.List;
import java.util.Map;

public record WorkflowContract(
        String key,
        String label,
        List<WorkflowStep> steps,
        Map<String, Object> extensions
) {
    public WorkflowContract {
        steps = steps == null ? List.of() : List.copyOf(steps);
        extensions = extensions == null ? Map.of() : Map.copyOf(extensions);
    }
}