package org.rostislav.curiokeep.collections;

import org.rostislav.curiokeep.collections.api.dto.CollectionMemberResponse;
import org.rostislav.curiokeep.collections.api.dto.Role;
import org.rostislav.curiokeep.collections.api.dto.UpdateMemberRoleRequest;
import org.rostislav.curiokeep.collections.entities.CollectionEntity;
import org.rostislav.curiokeep.collections.entities.CollectionMemberEntity;
import org.rostislav.curiokeep.collections.entities.CollectionMemberId;
import org.rostislav.curiokeep.user.AppUserRepository;
import org.rostislav.curiokeep.user.CurrentUserService;
import org.rostislav.curiokeep.user.entities.AppUserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CollectionMemberManagementService {

    private static final Logger log = LoggerFactory.getLogger(CollectionMemberManagementService.class);

    private final CollectionMemberRepository members;
    private final CollectionRepository collections;
    private final AppUserRepository users;
    private final CurrentUserService currentUser;
    private final CollectionAccessService access;

    public CollectionMemberManagementService(CollectionMemberRepository members,
                                             CollectionRepository collections,
                                             AppUserRepository users,
                                             CurrentUserService currentUser,
                                             CollectionAccessService access) {
        this.members = members;
        this.collections = collections;
        this.users = users;
        this.currentUser = currentUser;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public List<CollectionMemberResponse> list(UUID collectionId) {
        var acting = currentUser.requireCurrentUser();
        access.requireRole(collectionId, acting.getId(), Role.ADMIN);

        List<CollectionMemberEntity> memberEntities = members.findAllByIdCollectionId(collectionId);
        Map<UUID, AppUserEntity> userMap = users.findAllById(
                memberEntities.stream().map(CollectionMemberEntity::getUserId).toList()
        ).stream().collect(Collectors.toMap(AppUserEntity::getId, Function.identity()));

        return memberEntities.stream()
                .map(m -> toResponse(m, userMap.get(m.getUserId())))
                .toList();
    }

    @Transactional
    public CollectionMemberResponse updateRole(UUID collectionId, UUID targetUserId, UpdateMemberRoleRequest req) {
        if (req == null || req.role() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "role is required");
        }
        if (req.role() == Role.OWNER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ownership transfer is not supported");
        }

        var acting = currentUser.requireCurrentUser();
        CollectionMemberEntity actingMember = access.requireRole(collectionId, acting.getId(), Role.ADMIN);

        CollectionEntity collection = collections.findById(collectionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        CollectionMemberEntity target = members.findByIdCollectionIdAndIdUserId(collectionId, targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (isOwner(collection, target)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot change owner role");
        }
        if (actingMember.getRole() == Role.ADMIN && target.getRole() == Role.OWNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admins cannot change owners");
        }

        target.setRole(req.role());
        members.save(target);

        AppUserEntity user = users.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        log.info("Member role updated: collectionId={} targetUserId={} newRole={} byUserId={}", collectionId, targetUserId, req.role(), acting.getId());
        return toResponse(target, user);
    }

    @Transactional
    public void remove(UUID collectionId, UUID targetUserId) {
        var acting = currentUser.requireCurrentUser();
        CollectionMemberEntity actingMember = access.requireRole(collectionId, acting.getId(), Role.ADMIN);

        CollectionEntity collection = collections.findById(collectionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        CollectionMemberEntity target = members.findByIdCollectionIdAndIdUserId(collectionId, targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (isOwner(collection, target)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot remove the owner");
        }
        if (actingMember.getRole() == Role.ADMIN && target.getRole() == Role.OWNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admins cannot remove owners");
        }

        members.delete(target);
        log.info("Member removed: collectionId={} targetUserId={} byUserId={}", collectionId, targetUserId, acting.getId());
    }

    public CollectionMemberResponse toResponse(CollectionMemberEntity member, AppUserEntity user) {
        String email = user != null ? user.getEmail() : null;
        String name = user != null ? user.getDisplayName() : null;
        return new CollectionMemberResponse(member.getUserId(), email, name, member.getRole(), member.getCreatedAt());
    }

    boolean isOwner(CollectionEntity collection, CollectionMemberEntity member) {
        return member.getRole() == Role.OWNER || (collection.getOwnerUserId() != null && collection.getOwnerUserId().equals(member.getUserId()));
    }

    public CollectionMemberEntity createMembership(UUID collectionId, UUID userId, Role role) {
        CollectionMemberEntity entity = new CollectionMemberEntity();
        entity.setId(new CollectionMemberId(collectionId, userId));
        entity.setRole(role);
        return entity;
    }
}
