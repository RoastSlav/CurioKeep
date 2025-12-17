package org.rostislav.curiokeep.collections;

import org.rostislav.curiokeep.collections.api.dto.CollectionResponse;
import org.rostislav.curiokeep.collections.api.dto.CreateCollectionRequest;
import org.rostislav.curiokeep.collections.api.dto.Role;
import org.rostislav.curiokeep.collections.api.dto.UpdateCollectionRequest;
import org.rostislav.curiokeep.collections.entities.CollectionEntity;
import org.rostislav.curiokeep.collections.entities.CollectionMemberEntity;
import org.rostislav.curiokeep.collections.entities.CollectionMemberId;
import org.rostislav.curiokeep.user.CurrentUserService;
import org.rostislav.curiokeep.user.entities.AppUserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CollectionService {

    private static final Logger log = LoggerFactory.getLogger(CollectionService.class);

    private final CollectionRepository collections;
    private final CollectionMemberRepository members;
    private final CurrentUserService currentUser;
    private final CollectionAccessService access;

    public CollectionService(CollectionRepository collections,
                             CollectionMemberRepository members,
                             CurrentUserService currentUser,
                             CollectionAccessService access) {
        this.collections = collections;
        this.members = members;
        this.currentUser = currentUser;
        this.access = access;
    }

    @Transactional
    public CollectionResponse create(CreateCollectionRequest req) {
        AppUserEntity user = currentUser.requireCurrentUser();

        if (req.name() == null || req.name().isBlank()) {
            log.info("Failed to create collection: missing name by userId={}", user.getId());
            throw new IllegalArgumentException("name is required");
        }

        CollectionEntity collection = new CollectionEntity();
        collection.setName(req.name().trim());
        collection.setDescription(req.description());
        collection.setOwnerUserId(user.getId());

        collection = collections.save(collection);

        CollectionMemberEntity m = new CollectionMemberEntity();
        m.setId(new CollectionMemberId(collection.getId(), user.getId()));
        m.setRole(Role.OWNER);
        members.save(m);

        log.info("Collection created: collectionId={} ownerId={}", collection.getId(), user.getId());

        return CollectionResponse.from(collection, m.getRole());
    }

    @Transactional(readOnly = true)
    public CollectionResponse get(UUID id) {
        AppUserEntity user = currentUser.requireCurrentUser();

        Role role = access.requireRole(id, user.getId(), Role.VIEWER).getRole();

        CollectionEntity collection = collections.findById(id)
                .orElseThrow(() -> {
                    log.info("Collection not found: collectionId={} requestedByUserId={}", id, user.getId());
                    return new ResponseStatusException(HttpStatus.NOT_FOUND);
                });

        return CollectionResponse.from(collection, role);
    }

    @Transactional
    public CollectionResponse update(UUID id, UpdateCollectionRequest req) {
        AppUserEntity user = currentUser.requireCurrentUser();

        Role role = access.requireRole(id, user.getId(), Role.ADMIN).getRole();

        CollectionEntity collection = collections.findById(id)
                .orElseThrow(() -> {
                    log.info("Collection not found for update: collectionId={} requestedByUserId={}", id, user.getId());
                    return new ResponseStatusException(HttpStatus.NOT_FOUND);
                });

        if (req.name() != null) {
            String name = req.name().trim();
            if (name.isBlank()) {
                log.info("Failed to update collection: blank name collectionId={} byUserId={}", id, user.getId());
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name cannot be blank");
            }
            collection.setName(name);
        }
        if (req.description() != null) {
            collection.setDescription(req.description());
        }

        collections.save(collection);

        log.info("Collection updated: collectionId={} byUserId={} role={}", id, user.getId(), role);

        return CollectionResponse.from(collection, role);
    }

    @Transactional
    public void delete(UUID id) {
        AppUserEntity user = currentUser.requireCurrentUser();

        access.requireRole(id, user.getId(), Role.OWNER);

        CollectionEntity collection = collections.findById(id)
                .orElseThrow(() -> {
                    log.info("Collection not found: collectionId={} requestedByUserId={}", id, user.getId());
                    return new ResponseStatusException(HttpStatus.NOT_FOUND);
                });

        members.deleteAllByIdCollectionId(id);
        collections.delete(collection);

        log.info("Collection deleted: collectionId={} byUserId={}", id, user.getId());
    }

    @Transactional(readOnly = true)
    public List<CollectionResponse> listForCurrentUser() {
        AppUserEntity user = currentUser.requireCurrentUser();

        var memberships = members.findAllByIdUserId(user.getId());
        if (memberships.isEmpty()) return List.of();

        var collectionIds = memberships.stream()
                .map(CollectionMemberEntity::getCollectionId)
                .distinct()
                .toList();

        var collectionsById = collections.findAllByIdIn(collectionIds).stream()
                .collect(Collectors.toMap(CollectionEntity::getId, x -> x));

        return memberships.stream()
                .map(m -> {
                    CollectionEntity collection = collectionsById.get(m.getCollectionId());
                    if (collection == null) return null;
                    return CollectionResponse.from(collection, m.getRole());
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }
}
