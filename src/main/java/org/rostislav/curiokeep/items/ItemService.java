package org.rostislav.curiokeep.items;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.rostislav.curiokeep.collections.CollectionAccessService;
import org.rostislav.curiokeep.collections.api.dto.Role;
import org.rostislav.curiokeep.items.api.dto.*;
import org.rostislav.curiokeep.items.entities.ItemEntity;
import org.rostislav.curiokeep.items.entities.ItemIdentifierEntity;
import org.rostislav.curiokeep.modules.ModuleQueryService;
import org.rostislav.curiokeep.modules.contract.FieldContract;
import org.rostislav.curiokeep.modules.contract.ModuleContract;
import org.rostislav.curiokeep.modules.entities.ModuleDefinitionEntity;
import org.rostislav.curiokeep.user.CurrentUserService;
import org.rostislav.curiokeep.user.entities.AppUserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Service
public class ItemService {

    private static final Logger log = LoggerFactory.getLogger(ItemService.class);

    private final ItemRepository items;
    private final ItemIdentifierRepository identifiers;
    private final CurrentUserService currentUser;
    private final CollectionAccessService access;
    private final ModuleQueryService modules;
    private final ObjectMapper objectMapper;

    public ItemService(
            ItemRepository items,
            ItemIdentifierRepository identifiers,
            CurrentUserService currentUser,
            CollectionAccessService access,
            ModuleQueryService modules
    ) {
        this.items = items;
        this.identifiers = identifiers;
        this.currentUser = currentUser;
        this.access = access;
        this.modules = modules;

        objectMapper = JsonMapper.builder()
                .defaultPropertyInclusion(JsonInclude.Value.construct(NON_NULL, NON_NULL))
                .findAndAddModules()
                .build();
    }

    @Transactional(readOnly = true)
    public Page<ItemResponse> list(UUID collectionId, UUID moduleId, Pageable pageable) {
        checkUserRole(collectionId, Role.VIEWER);

        return items.findAllByCollectionIdAndModuleId(collectionId, moduleId, pageable)
                .map(e -> ItemResponse.from(e, objectMapper));
    }

    @Transactional
    public ItemResponse create(UUID collectionId, CreateItemRequest req) {
        AppUserEntity u = checkUserRole(collectionId, Role.EDITOR);

        ModuleDefinitionEntity def = modules.getEntityById(req.moduleId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "MODULE_NOT_FOUND"));

        ModuleContract contract = modules.getContract(def);

        JsonNode attrs = toJsonNode(req.attributes());
        validateState(contract, req.stateKey());
        validateAttributes(contract, attrs);

        ItemEntity e = new ItemEntity();
        e.setCollectionId(collectionId);
        e.setModuleId(req.moduleId());
        e.setStateKey(normalizeState(req.stateKey(), contract));
        e.setTitle(req.title());
        e.setAttributes(attrs);
        e.setCreatedBy(u.getId());

        items.save(e);

        upsertIdentifiers(e.getId(), req.identifiers());

        log.info("Item created: itemId={} collectionId={} moduleId={} byUserId={}",
                e.getId(), collectionId, e.getModuleId(), u.getId());

        return ItemResponse.from(e, objectMapper);
    }

    @Transactional(readOnly = true)
    public ItemResponse get(UUID collectionId, UUID itemId) {
        checkUserRole(collectionId, Role.VIEWER);

        ItemEntity e = items.findById(itemId)
                .filter(it -> it.getCollectionId().equals(collectionId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ITEM_NOT_FOUND"));

        return ItemResponse.from(e, objectMapper);
    }

    @Transactional
    public ItemResponse update(UUID collectionId, UUID itemId, UpdateItemRequest req) {
        AppUserEntity u = checkUserRole(collectionId, Role.EDITOR);

        ItemEntity e = items.findById(itemId)
                .filter(it -> it.getCollectionId().equals(collectionId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ITEM_NOT_FOUND"));

        ModuleDefinitionEntity def = modules.getEntityById(e.getModuleId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "MODULE_NOT_FOUND"));

        ModuleContract contract = modules.getContract(def);

        JsonNode attrs = toJsonNode(req.attributes());

        if (req.stateKey() != null) {
            validateState(contract, req.stateKey());
            e.setStateKey(normalizeState(req.stateKey(), contract));
        }
        if (req.title() != null) e.setTitle(req.title());
        if (req.attributes() != null) {
            validateAttributes(contract, attrs);
            e.setAttributes(attrs);
        }

        items.save(e);

        if (req.identifiers() != null) {
            replaceIdentifiers(e.getId(), req.identifiers());
        }

        log.info("Item updated: itemId={} collectionId={} byUserId={}", e.getId(), collectionId, u.getId());

        return ItemResponse.from(e, objectMapper);
    }

    @Transactional
    public void delete(UUID collectionId, UUID itemId) {
        AppUserEntity u = checkUserRole(collectionId, Role.ADMIN);

        ItemEntity e = items.findById(itemId)
                .filter(it -> it.getCollectionId().equals(collectionId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ITEM_NOT_FOUND"));

        identifiers.deleteAll(identifiers.findAllByItemId(e.getId()));
        items.delete(e);

        log.info("Item deleted: itemId={} collectionId={} byUserId={}", e.getId(), collectionId, u.getId());
    }

    @Transactional
    public ItemResponse changeState(UUID collectionId, UUID itemId, ChangeStateRequest req) {
        AppUserEntity u = checkUserRole(collectionId, Role.EDITOR);

        ItemEntity e = items.findById(itemId)
                .filter(it -> it.getCollectionId().equals(collectionId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ITEM_NOT_FOUND"));

        ModuleDefinitionEntity def = modules.getEntityById(e.getModuleId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "MODULE_NOT_FOUND"));

        ModuleContract contract = modules.getContract(def);

        validateState(contract, req.stateKey());
        e.setStateKey(normalizeState(req.stateKey(), contract));
        items.save(e);

        log.info("Item state changed: itemId={} collectionId={} state={} byUserId={}",
                e.getId(), collectionId, e.getStateKey(), u.getId());

        return ItemResponse.from(e, objectMapper);
    }

    private void upsertIdentifiers(UUID itemId, List<ItemIdentifierDto> ids) {
        if (ids == null || ids.isEmpty()) return;
        for (ItemIdentifierDto dto : ids) {
            ItemIdentifierEntity e = new ItemIdentifierEntity();
            e.setItemId(itemId);
            e.setIdType(dto.idType());
            e.setIdValue(dto.idValue().trim());
            identifiers.save(e);
        }
    }

    private AppUserEntity checkUserRole(UUID collectionId, Role minimumRole) {
        AppUserEntity u = currentUser.requireCurrentUser();
        access.requireRole(collectionId, u.getId(), minimumRole);
        return u;
    }

    private void replaceIdentifiers(UUID itemId, List<ItemIdentifierDto> ids) {
        identifiers.deleteAll(identifiers.findAllByItemId(itemId));
        upsertIdentifiers(itemId, ids);
    }

    private String normalizeState(String stateKey, ModuleContract contract) {
        if (stateKey == null || stateKey.isBlank()) {
            return contract.states().isEmpty() ? "OWNED" : contract.states().getFirst().key();
        }
        return stateKey.trim().toUpperCase(Locale.ROOT);
    }

    private void validateState(ModuleContract contract, String stateKeyRaw) {
        if (stateKeyRaw == null || stateKeyRaw.isBlank()) return;
        String key = stateKeyRaw.trim().toUpperCase(Locale.ROOT);

        boolean ok = contract.states().stream().anyMatch(s -> s.key().equalsIgnoreCase(key));
        if (!ok) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_STATE");
    }

    private void validateAttributes(ModuleContract contract, JsonNode attributes) {
        if (attributes == null || !attributes.isObject()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ATTRIBUTES_MUST_BE_OBJECT");
        }

        contract.fields().stream()
                .filter(FieldContract::required)
                .forEach(f -> {
                    JsonNode v = attributes.get(f.key());
                    if (v == null || v.isNull() || (v.isTextual() && v.asText().isBlank())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MISSING_REQUIRED_FIELD_" + f.key());
                    }
                });
    }

    private JsonNode toJsonNode(Map<String, Object> attributes) {
        return objectMapper.valueToTree(attributes);
    }
}