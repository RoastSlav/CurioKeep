package org.rostislav.curiokeep.modules.contract;

import java.util.List;

public record ModuleMeta(
        List<Author> authors,
        String license,
        String homepage,
        String repository,
        String icon,
        List<String> tags,
        String minAppVersion
) {
    public ModuleMeta {
        authors = authors == null ? List.of() : List.copyOf(authors);
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}