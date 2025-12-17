package org.rostislav.curiokeep.collections;

import org.rostislav.curiokeep.collections.api.dto.CollectionModuleResponse;
import org.rostislav.curiokeep.collections.api.dto.EnableModuleResponse;
import org.rostislav.curiokeep.collections.api.dto.Role;
import org.rostislav.curiokeep.collections.entities.CollectionModuleEntity;
import org.rostislav.curiokeep.collections.entities.CollectionModuleId;
import org.rostislav.curiokeep.modules.ModuleDefinitionRepository;
import org.rostislav.curiokeep.modules.entities.ModuleDefinitionEntity;
import org.rostislav.curiokeep.user.CurrentUserService;
import org.rostislav.curiokeep.user.entities.AppUserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CollectionModuleService {

    private static final Logger log = LoggerFactory.getLogger(CollectionModuleService.class);

    private final CollectionModuleRepository collectionModules;
    private final ModuleDefinitionRepository moduleDefinitions;
    private final CurrentUserService currentUser;
    private final CollectionAccessService access;

    public CollectionModuleService(
            CollectionModuleRepository collectionModules,
            ModuleDefinitionRepository moduleDefinitions,
            CurrentUserService currentUser,
            CollectionAccessService access
    ) {
        this.collectionModules = collectionModules;
        this.moduleDefinitions = moduleDefinitions;
        this.currentUser = currentUser;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public List<CollectionModuleResponse> listEnabled(UUID collectionId) {
        AppUserEntity u = currentUser.requireCurrentUser();

        access.requireRole(collectionId, u.getId(), Role.VIEWER);

        List<CollectionModuleEntity> enabled = collectionModules.findAllByIdCollectionId(collectionId);
        if (enabled.isEmpty()) return List.of();

        Set<UUID> moduleIds = enabled.stream()
                .map(CollectionModuleEntity::getModuleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, ModuleDefinitionEntity> defsById = moduleDefinitions.findAllById(moduleIds).stream()
                .collect(Collectors.toMap(ModuleDefinitionEntity::getId, x -> x));

        return enabled.stream()
                .map(cm -> {
                    ModuleDefinitionEntity def = defsById.get(cm.getModuleId());
                    if (def == null) return null;
                    return new CollectionModuleResponse(
                            def.getModuleKey(),
                            def.getName(),
                            def.getVersion(),
                            def.getSource(),
                            cm.getEnabledAt(),
                            def.getId()
                    );
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Transactional
    public EnableModuleResponse enable(UUID collectionId, String moduleKeyRaw) {
        AppUserEntity u = currentUser.requireCurrentUser();

        access.requireRole(collectionId, u.getId(), Role.ADMIN);

        String moduleKey = normalizeModuleKey(moduleKeyRaw);

        ModuleDefinitionEntity def = moduleDefinitions.findByModuleKey(moduleKey)
                .orElseThrow(() -> {
                    log.info("Module not found for enabling: collectionId={} moduleKey={} byUserId={}",
                            collectionId, moduleKey, u.getId());
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "MODULE_NOT_FOUND");
                });

        if (collectionModules.existsByIdCollectionIdAndIdModuleId(collectionId, def.getId())) {
            return new EnableModuleResponse(true);
        }

        CollectionModuleEntity cm = new CollectionModuleEntity();
        cm.setId(new CollectionModuleId(collectionId, def.getId()));
        collectionModules.save(cm);

        log.info("Module enabled for collection: collectionId={} moduleKey={} byUserId={}",
                collectionId, moduleKey, u.getId());

        return new EnableModuleResponse(true);
    }

    @Transactional
    public EnableModuleResponse disable(UUID collectionId, String moduleKeyRaw) {
        AppUserEntity u = currentUser.requireCurrentUser();

        access.requireRole(collectionId, u.getId(), Role.ADMIN);

        String moduleKey = normalizeModuleKey(moduleKeyRaw);

        ModuleDefinitionEntity def = moduleDefinitions.findByModuleKey(moduleKey)
                .orElseThrow(() -> {
                    log.info("Module not found for disabling: collectionId={} moduleKey={} byUserId={}",
                            collectionId, moduleKey, u.getId());
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "MODULE_NOT_FOUND");
                });

        collectionModules.deleteByIdCollectionIdAndIdModuleId(collectionId, def.getId());

        log.info("Module disabled for collection: collectionId={} moduleKey={} byUserId={}",
                collectionId, moduleKey, u.getId());

        return new EnableModuleResponse(false);
    }

    private String normalizeModuleKey(String raw) {
        if (raw == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "moduleKey is required");
        String k = raw.trim().toLowerCase();
        if (k.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "moduleKey is required");
        if (!k.matches("[a-z0-9][a-z0-9_-]{1,63}")) {
            log.info("Invalid moduleKey format: {}", raw);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid moduleKey");
        }
        return k;
    }
}