package org.rostislav.curiokeep.items;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
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

@Service
public class ItemService {

    private static final Logger log = LoggerFactory.getLogger(ItemService.class);

    private final ItemRepository items;
    private final ItemIdentifierRepository identifiers;
    private final CurrentUserService currentUser;
    private final CollectionAccessService access;
    private final ModuleQueryService modules;
    private final ObjectMapper objectMapper;
    private final ItemImageService imageService;

    public ItemService(
            ItemRepository items,
            ItemIdentifierRepository identifiers,
            CurrentUserService currentUser,
            CollectionAccessService access,
            ModuleQueryService modules,
            ObjectMapper objectMapper,
            ItemImageService imageService
    ) {
        this.items = items;
        this.identifiers = identifiers;
        this.currentUser = currentUser;
        this.access = access;
        this.modules = modules;
        this.objectMapper = objectMapper;
        this.imageService = imageService;
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

        Map<String, Object> attrsMap = new java.util.LinkedHashMap<>(req.attributes() == null ? Map.of() : req.attributes());
        ImageProcessResult imageResult = handleImage(attrsMap);
        JsonNode attrs = toJsonNode(attrsMap);
        validateState(contract, req.stateKey());
        validateAttributes(contract, attrs);

        ItemEntity e = new ItemEntity();
        e.setCollectionId(collectionId);
        e.setModuleId(req.moduleId());
        e.setStateKey(normalizeState(req.stateKey(), contract));
        e.setTitle(req.title());
        e.setAttributes(writeJson(attrs));
        if (imageResult.fileName() != null) {
            e.setImageName(imageResult.fileName());
        }
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

        JsonNode attrs = null;
        ImageProcessResult imageResult = new ImageProcessResult(null, false);

        if (req.attributes() != null) {
            Map<String, Object> attrsMap = new java.util.LinkedHashMap<>(req.attributes());
            imageResult = handleImage(attrsMap);
            attrs = toJsonNode(attrsMap);
        }

        if (req.stateKey() != null) {
            validateState(contract, req.stateKey());
            e.setStateKey(normalizeState(req.stateKey(), contract));
        }
        if (req.title() != null) e.setTitle(req.title());
        if (req.attributes() != null) {
            validateAttributes(contract, attrs);
            e.setAttributes(writeJson(attrs));
        }

        if (imageResult.cleared()) {
            e.setImageName(null);
        } else if (imageResult.fileName() != null) {
            e.setImageName(imageResult.fileName());
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

    private String writeJson(JsonNode value) {
        if (value == null || value.isNull()) return "{}";
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "ATTRIBUTES_SERIALIZATION_FAILED", e);
        }
    }

    private ImageProcessResult handleImage(Map<String, Object> attrs) {
        Object urlObj = attrs.get("providerImageUrl");
        if (!(urlObj instanceof String urlRaw)) {
            return new ImageProcessResult(null, false);
        }

        String url = urlRaw.trim();
        if (url.isBlank()) {
            attrs.remove("providerImageUrl");
            return new ImageProcessResult(null, true);
        }

        // Already cached locally
        if (url.startsWith("/api/assets/")) {
            String fileName = url.substring("/api/assets/".length());
            return new ImageProcessResult(fileName, false);
        }

        String fileName = imageService.downloadToLocal(url);
        if (fileName != null) {
            attrs.put("providerImageUrl", "/api/assets/" + fileName);
            return new ImageProcessResult(fileName, false);
        }

        return new ImageProcessResult(null, false);
    }

    private record ImageProcessResult(String fileName, boolean cleared) {}
}