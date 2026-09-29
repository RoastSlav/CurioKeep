package org.rostislav.curiokeep.modules.contract;

import java.util.List;

/** The steps that bring an item from any earlier module version up to version {@code to}. */
public record MigrationContract(String to, List<MigrationStep> steps) {
    public MigrationContract {
        steps = steps == null ? List.of() : List.copyOf(steps);
    }
}
