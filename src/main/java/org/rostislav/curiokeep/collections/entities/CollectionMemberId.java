package org.rostislav.curiokeep.collections.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class CollectionMemberId implements Serializable {

    @Column(name = "collection_id", columnDefinition = "uuid")
    private UUID collectionId;

    @Column(name = "user_id", columnDefinition = "uuid")
    private UUID userId;

    public CollectionMemberId() {}
    public CollectionMemberId(UUID collectionId, UUID userId) {
        this.collectionId = collectionId;
        this.userId = userId;
    }

    public UUID getCollectionId() { return collectionId; }
    public UUID getUserId() { return userId; }
    public void setCollectionId(UUID collectionId) { this.collectionId = collectionId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CollectionMemberId that)) return false;
        return Objects.equals(collectionId, that.collectionId) && Objects.equals(userId, that.userId);
    }
    @Override public int hashCode() { return Objects.hash(collectionId, userId); }
}