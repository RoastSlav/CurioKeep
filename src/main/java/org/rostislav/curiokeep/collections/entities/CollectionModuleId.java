package org.rostislav.curiokeep.collections.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class CollectionModuleId implements Serializable {

    @Column(name = "collection_id", columnDefinition = "uuid")
    private UUID collectionId;

    @Column(name = "module_id", columnDefinition = "uuid")
    private UUID moduleId;

    public CollectionModuleId() {
    }

    public CollectionModuleId(UUID collectionId, UUID moduleId) {
        this.collectionId = collectionId;
        this.moduleId = moduleId;
    }

    public UUID getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(UUID collectionId) {
        this.collectionId = collectionId;
    }

    public UUID getModuleId() {
        return moduleId;
    }

    public void setModuleId(UUID moduleId) {
        this.moduleId = moduleId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CollectionModuleId that)) return false;
        return Objects.equals(collectionId, that.collectionId) && Objects.equals(moduleId, that.moduleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(collectionId, moduleId);
    }
}