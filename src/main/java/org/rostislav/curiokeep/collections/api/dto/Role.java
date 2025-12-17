package org.rostislav.curiokeep.collections.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Collection membership role. OWNER is highest.")
public enum Role {
    OWNER, ADMIN, EDITOR, VIEWER;

    public boolean allows(Role required) {
        return this.ordinal() <= required.ordinal();
    }
}
