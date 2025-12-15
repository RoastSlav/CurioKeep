package org.rostislav.curiokeep.collections.entities;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "collection_module")
public class CollectionModuleEntity {

    @EmbeddedId
    private CollectionModuleId id;

    @Column(name = "enabled_at", nullable = false)
    private OffsetDateTime enabledAt;

    @PrePersist
    void prePersist() {
        if (enabledAt == null) enabledAt = OffsetDateTime.now();
    }

    public java.util.UUID getCollectionId() {
        return id != null ? id.getCollectionId() : null;
    }

    public java.util.UUID getModuleId() {
        return id != null ? id.getModuleId() : null;
    }

    // getters/setters
    public CollectionModuleId getId() {
        return id;
    }

    public void setId(CollectionModuleId id) {
        this.id = id;
    }

    public OffsetDateTime getEnabledAt() {
        return enabledAt;
    }

    public void setEnabledAt(OffsetDateTime enabledAt) {
        this.enabledAt = enabledAt;
    }
}