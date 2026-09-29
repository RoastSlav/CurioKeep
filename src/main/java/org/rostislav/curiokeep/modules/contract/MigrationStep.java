package org.rostislav.curiokeep.modules.contract;

import java.util.List;

/**
 * One step of a migration. Which properties are set depends on {@code op}: MOVE and COPY use {@code from}, {@code to} and an
 * optional {@code transform}; MAP uses {@code field} and {@code mappings}; DEFAULT uses {@code field} and {@code value}; DROP uses
 * {@code field}.
 */
public record MigrationStep(
        MigrationOp op,
        String from,
        String to,
        String field,
        String value,
        String transform,
        List<ValueMapping> mappings
) {
    public MigrationStep {
        mappings = mappings == null ? List.of() : List.copyOf(mappings);
    }

    public record ValueMapping(String from, String to) {
    }
}
