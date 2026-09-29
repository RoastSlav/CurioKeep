package org.rostislav.curiokeep.items;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import org.rostislav.curiokeep.collections.CollectionAccessService;
import org.rostislav.curiokeep.collections.api.dto.Role;
import org.rostislav.curiokeep.items.api.dto.*;
import org.rostislav.curiokeep.items.entities.ItemEntity;
import org.rostislav.curiokeep.items.entities.ItemIdentifierEntity;
import org.rostislav.curiokeep.modules.ModuleQueryService;
import org.rostislav.curiokeep.modules.contract.ModuleContract;
import org.rostislav.curiokeep.modules.entities.ModuleDefinitionEntity;
import org.rostislav.curiokeep.user.CurrentUserService;
import org.rostislav.curiokeep.user.entities.AppUserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class ItemService {

    private static final Logger log = LoggerFactory.getLogger(ItemService.class);
    private static final String ASSET_PATH = "/api/assets/";

    private final ItemRepository items;
    private final ItemIdentifierRepository identifiers;
    private final CurrentUserService currentUser;
    private final CollectionAccessService access;
    private final ModuleQueryService modules;
    private final ObjectMapper objectMapper;
    private final ItemImageService imageService;
    private final ItemSearchRepository search;
    private final TransactionTemplate tx;

    public ItemService(
            ItemRepository items,
            ItemIdentifierRepository identifiers,
            CurrentUserService currentUser,
            CollectionAccessService access,
            ModuleQueryService modules,
            ObjectMapper objectMapper,
            ItemImageService imageService,
            ItemSearchRepository search,
            TransactionTemplate tx
    ) {
        this.items = items;
        this.identifiers = identifiers;
        this.currentUser = currentUser;
        this.access = access;
        this.modules = modules;
        this.objectMapper = objectMapper;
        this.imageService = imageService;
        this.search = search;
        this.tx = tx;
    }

    @Transactional(readOnly = true)
    public Page<ItemResponse> list(UUID collectionId, ItemListRequest request) {
        checkUserRole(collectionId, Role.VIEWER);

        ModuleDefinitionEntity def = modules.getEntityById(request.moduleId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "MODULE_NOT_FOUND"));
        ItemQuery query = ItemQueryParser.parse(modules.getContract(def), new ItemQueryParser.Params(
                collectionId, request.moduleId(), request.search(), request.state(), request.sort(),
                request.page(), request.size(), request.otherParams()));

        return search.search(query).map(e -> ItemResponse.from(e, objectMapper));
    }

    public ItemResponse create(UUID collectionId, CreateItemRequest req) {
        AppUserEntity u = checkUserRole(collectionId, Role.EDITOR);

        ModuleDefinitionEntity def = modules.getEntityById(req.moduleId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "MODULE_NOT_FOUND"));
        ModuleContract contract = modules.getContract(def);
        validateState(contract, req.stateKey());

        Map<String, Object> attrsMap = new java.util.LinkedHashMap<>(req.attributes() == null ? Map.of() : req.attributes());
        ItemAttributeValidator.validate(contract, toJsonNode(attrsMap));
        // The cover is downloaded before the transaction starts so a slow host cannot hold a database connection.
        ImageProcessResult imageResult = handleImage(attrsMap);
        JsonNode attrs = toJsonNode(attrsMap);

        ItemEntity saved;
        try {
            saved = tx.execute(status -> {
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
                return e;
            });
        } catch (RuntimeException ex) {
            if (imageResult.downloaded()) imageService.delete(imageResult.fileName());
            throw ex;
        }

        log.info("Item created: itemId={} collectionId={} moduleId={} byUserId={}",
                saved.getId(), collectionId, saved.getModuleId(), u.getId());

        return ItemResponse.from(saved, objectMapper);
    }

    @Transactional(readOnly = true)
    public ItemResponse get(UUID collectionId, UUID itemId) {
        checkUserRole(collectionId, Role.VIEWER);

        ItemEntity e = requireItem(collectionId, itemId);

        return ItemResponse.from(e, objectMapper);
    }

    public ItemResponse update(UUID collectionId, UUID itemId, UpdateItemRequest req) {
        AppUserEntity u = checkUserRole(collectionId, Role.EDITOR);

        ItemEntity current = requireItem(collectionId, itemId);
        ModuleDefinitionEntity def = modules.getEntityById(current.getModuleId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "MODULE_NOT_FOUND"));
        ModuleContract contract = modules.getContract(def);

        if (req.stateKey() != null) {
            validateState(contract, req.stateKey());
        }

        JsonNode attrs = null;
        ImageProcessResult imageResult = ImageProcessResult.NONE;
        if (req.attributes() != null) {
            Map<String, Object> attrsMap = new java.util.LinkedHashMap<>(req.attributes());
            ItemAttributeValidator.validate(contract, toJsonNode(attrsMap));
            imageResult = handleImage(attrsMap);
            attrs = toJsonNode(attrsMap);
        }

        JsonNode newAttrs = attrs;
        ImageProcessResult image = imageResult;
        SavedItem result;
        try {
            result = tx.execute(status -> {
                ItemEntity e = requireItem(collectionId, itemId);
                String before = e.getImageName();
                if (req.stateKey() != null) e.setStateKey(normalizeState(req.stateKey(), contract));
                if (req.title() != null) e.setTitle(req.title());
                if (newAttrs != null) e.setAttributes(writeJson(newAttrs));
                if (image.cleared()) {
                    e.setImageName(null);
                } else if (image.fileName() != null) {
                    e.setImageName(image.fileName());
                }
                items.save(e);
                if (req.identifiers() != null) {
                    replaceIdentifiers(e.getId(), req.identifiers());
                }
                return new SavedItem(e, before);
            });
        } catch (RuntimeException ex) {
            if (image.downloaded()) imageService.delete(image.fileName());
            throw ex;
        }
        if (!Objects.equals(result.previousImage(), result.entity().getImageName())) releaseImage(result.previousImage());

        log.info("Item updated: itemId={} collectionId={} byUserId={}", itemId, collectionId, u.getId());

        return ItemResponse.from(result.entity(), objectMapper);
    }

    public ItemResponse setImageFromUrl(UUID collectionId, UUID itemId, String url) {
        AppUserEntity u = checkUserRole(collectionId, Role.EDITOR);
        requireItem(collectionId, itemId);

        if (url == null || url.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IMAGE_URL_REQUIRED");
        }

        String trimmed = url.trim();
        Optional<String> fileName = imageService.downloadToLocal(trimmed);
        if (fileName.isEmpty()) {
            log.warn("Image download failed, storing external url only: itemId={} collectionId={}", itemId, collectionId);
        }

        SavedItem result;
        try {
            result = tx.execute(status -> {
                ItemEntity e = requireItem(collectionId, itemId);
                String before = e.getImageName();
                if (fileName.isPresent()) {
                    applyStoredImage(e, fileName.get());
                } else {
                    clearStoredImage(e);
                    replaceProviderImageAttribute(e, trimmed);
                }
                items.save(e);
                return new SavedItem(e, before);
            });
        } catch (RuntimeException ex) {
            fileName.ifPresent(imageService::delete);
            throw ex;
        }
        releaseImage(result.previousImage());

        log.info("Item image set from url: itemId={} collectionId={} byUserId={}", itemId, collectionId, u.getId());

        return ItemResponse.from(result.entity(), objectMapper);
    }

    @Transactional(readOnly = true)
    public ItemCountsResponse counts(UUID collectionId) {
        checkUserRole(collectionId, Role.VIEWER);

        Map<UUID, Map<String, Long>> byModule = new java.util.LinkedHashMap<>();
        for (ItemRepository.StateCount row : items.countByModuleAndState(collectionId)) {
            byModule.computeIfAbsent(row.getModuleId(), id -> new java.util.LinkedHashMap<>()).put(row.getStateKey(), row.getCount());
        }
        Map<UUID, ItemCountsResponse.ModuleCounts> modulesCounts = new java.util.LinkedHashMap<>();
        byModule.forEach((moduleId, byState) -> modulesCounts.put(moduleId,
                new ItemCountsResponse.ModuleCounts(byState.values().stream().mapToLong(Long::longValue).sum(), byState)));
        return new ItemCountsResponse(modulesCounts);
    }

    @Transactional
    public ItemResponse setImageFromUpload(UUID collectionId, UUID itemId, MultipartFile file) {
        AppUserEntity u = checkUserRole(collectionId, Role.EDITOR);
        ItemEntity e = requireItem(collectionId, itemId);

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IMAGE_FILE_REQUIRED");
        }

        Optional<String> fileName;
        try {
            fileName = imageService.storeUploaded(file.getBytes());
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IMAGE_READ_FAILED", ex);
        }

        if (fileName.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_IMAGE");
        }

        String previousImage = e.getImageName();
        applyStoredImage(e, fileName.get());
        items.save(e);
        releaseImage(previousImage);

        log.info("Item image uploaded: itemId={} collectionId={} byUserId={}", e.getId(), collectionId, u.getId());

        return ItemResponse.from(e, objectMapper);
    }

    @Transactional
    public ItemResponse clearImage(UUID collectionId, UUID itemId) {
        AppUserEntity u = checkUserRole(collectionId, Role.EDITOR);
        ItemEntity e = requireItem(collectionId, itemId);

        String previousImage = e.getImageName();
        clearStoredImage(e);
        items.save(e);
        releaseImage(previousImage);

        log.info("Item image cleared: itemId={} collectionId={} byUserId={}", e.getId(), collectionId, u.getId());

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
        releaseImage(e.getImageName());

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
        Map<ItemIdentifierEntity.IdType, ItemIdentifierDto> onePerType = new java.util.LinkedHashMap<>();
        ids.forEach(dto -> onePerType.put(dto.idType(), dto));
        for (ItemIdentifierDto dto : onePerType.values()) {
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

    private ItemEntity requireItem(UUID collectionId, UUID itemId) {
        return items.findById(itemId)
                .filter(it -> it.getCollectionId().equals(collectionId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ITEM_NOT_FOUND"));
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
            return ImageProcessResult.NONE;
        }

        String url = urlRaw.trim();
        if (url.isBlank()) {
            attrs.remove("providerImageUrl");
            return ImageProcessResult.CLEARED;
        }

        // Already cached locally
        if (url.startsWith(ASSET_PATH)) {
            String fileName = url.substring(ASSET_PATH.length());
            if (ItemImageService.isStoredName(fileName)) {
                return new ImageProcessResult(fileName, false, false);
            }
            attrs.remove("providerImageUrl");
            return ImageProcessResult.NONE;
        }

        Optional<String> fileName = imageService.downloadToLocal(url);
        if (fileName.isPresent()) {
            attrs.put("providerImageUrl", ASSET_PATH + fileName.get());
            return new ImageProcessResult(fileName.get(), false, true);
        }

        return ImageProcessResult.NONE;
    }

    /** {@code downloaded} marks a file this request created, so it can be removed again if saving fails. */
    private record ImageProcessResult(String fileName, boolean cleared, boolean downloaded) {
        static final ImageProcessResult NONE = new ImageProcessResult(null, false, false);
        static final ImageProcessResult CLEARED = new ImageProcessResult(null, true, false);
    }

    private record SavedItem(ItemEntity entity, String previousImage) {
    }

    /** Removes a cover file once no item refers to it any more. */
    private void releaseImage(String fileName) {
        if (fileName == null || items.existsByImageName(fileName)) return;
        imageService.delete(fileName);
    }

    private void applyStoredImage(ItemEntity e, String fileName) {
        e.setImageName(fileName);
        replaceProviderImageAttribute(e, ASSET_PATH + fileName);
    }

    private void clearStoredImage(ItemEntity e) {
        e.setImageName(null);
        replaceProviderImageAttribute(e, null);
    }

    private void replaceProviderImageAttribute(ItemEntity e, String assetPath) {
        try {
            ObjectNode attrs = objectMapper.readValue(
                    e.getAttributes() == null ? "{}" : e.getAttributes(),
                    ObjectNode.class
            );

            if (assetPath == null) {
                attrs.remove("providerImageUrl");
            } else {
                attrs.put("providerImageUrl", assetPath);
            }

            e.setAttributes(writeJson(attrs));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "ATTRIBUTES_DESERIALIZATION_FAILED", ex);
        }
    }
}