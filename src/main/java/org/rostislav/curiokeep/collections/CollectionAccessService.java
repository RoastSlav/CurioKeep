package org.rostislav.curiokeep.collections;

import org.rostislav.curiokeep.collections.api.dto.Role;
import org.rostislav.curiokeep.collections.entities.CollectionMemberEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class CollectionAccessService {

    private final CollectionMemberRepository members;

    public CollectionAccessService(CollectionMemberRepository members) {
        this.members = members;
    }

    public CollectionMemberEntity requireRole(UUID collectionId, UUID userId, Role minRole) {
        CollectionMemberEntity memberEntity = members.findByIdCollectionIdAndIdUserId(collectionId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));

        if (!allows(memberEntity.getRole(), minRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return memberEntity;
    }

    private boolean allows(Role have, Role need) {
        return have.ordinal() <= need.ordinal();
    }
}
