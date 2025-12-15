package org.rostislav.curiokeep.modules.contract;

public record Constraints(
        Double min,
        Double max,
        Integer minLength,
        Integer maxLength,
        String pattern,
        Boolean multi,
        Boolean uniqueWithinCollection
) {
}