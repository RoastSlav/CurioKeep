package org.rostislav.curiokeep.modules.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "module_state")
public class ModuleStateEntity {

    @EmbeddedId
    private ModuleStateId id;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "deprecated", nullable = false)
    private boolean deprecated = false;

    // convenience
    public java.util.UUID getModuleId() { return id != null ? id.getModuleId() : null; }
    public String getStateKey() { return id != null ? id.getStateKey() : null; }

    // getters/setters
    public ModuleStateId getId() { return id; }
    public void setId(ModuleStateId id) { this.id = id; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isDeprecated() { return deprecated; }
    public void setDeprecated(boolean deprecated) { this.deprecated = deprecated; }
}