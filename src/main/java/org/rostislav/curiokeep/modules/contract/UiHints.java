package org.rostislav.curiokeep.modules.contract;

public record UiHints(
        String widget,
        String placeholder,
        String helpText,
        String group,
        Boolean hidden
) {
}