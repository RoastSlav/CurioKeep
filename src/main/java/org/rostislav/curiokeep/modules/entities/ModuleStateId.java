package org.rostislav.curiokeep.modules.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ModuleStateId implements Serializable {

    @Column(name = "module_id", columnDefinition = "uuid")
    private UUID moduleId;

    @Column(name = "state_key")
    private String stateKey;

    public ModuleStateId() {}
    public ModuleStateId(UUID moduleId, String stateKey) {
        this.moduleId = moduleId;
        this.stateKey = stateKey;
    }

    public UUID getModuleId() { return moduleId; }
    public void setModuleId(UUID moduleId) { this.moduleId = moduleId; }
    public String getStateKey() { return stateKey; }
    public void setStateKey(String stateKey) { this.stateKey = stateKey; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ModuleStateId that)) return false;
        return Objects.equals(moduleId, that.moduleId) && Objects.equals(stateKey, that.stateKey);
    }
    @Override public int hashCode() { return Objects.hash(moduleId, stateKey); }
}