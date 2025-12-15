package org.rostislav.curiokeep.collections.entities;

import jakarta.persistence.*;
import org.rostislav.curiokeep.collections.api.dto.Role;

import java.time.OffsetDateTime;

@Entity
@Table(name = "collection_member")
public class CollectionMemberEntity {

    @EmbeddedId
    private CollectionMemberId id;
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role = Role.OWNER;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }

    public java.util.UUID getCollectionId() {
        return id != null ? id.getCollectionId() : null;
    }

    public java.util.UUID getUserId() {
        return id != null ? id.getUserId() : null;
    }

    // getters/setters
    public CollectionMemberId getId() {
        return id;
    }

    public void setId(CollectionMemberId id) {
        this.id = id;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}