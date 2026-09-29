package org.rostislav.curiokeep.items;

import org.rostislav.curiokeep.collections.CollectionAccessService;
import org.rostislav.curiokeep.collections.api.dto.Role;
import org.rostislav.curiokeep.items.api.dto.MigrationPreviewResponse;
import org.rostislav.curiokeep.items.api.dto.MigrationResultResponse;
import org.rostislav.curiokeep.items.entities.ItemEntity;
import org.rostislav.curiokeep.modules.MigrationEngine;
import org.rostislav.curiokeep.modules.ModuleQueryService;
import org.rostislav.curiokeep.modules.ModuleVersion;
import org.rostislav.curiokeep.modules.contract.MigrationStep;
import org.rostislav.curiokeep.modules.contract.ModuleContract;
import org.rostislav.curiokeep.modules.entities.ModuleDefinitionEntity;
import org.rostislav.curiokeep.user.CurrentUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Brings the items of a collection up to the current version of their module by running the migration steps the module declares.
 * <p>
 * A collection admin first asks for a preview, which reads every item that is behind but changes nothing, and then accepts it. The
 * work is bounded by the collection: items are read and written in batches of {@value #BATCH_SIZE}, in creation order, and one
 * batch is one transaction. Accepting is not undoable and is not recorded anywhere except the log.
 */
@Service
public class ItemMigrationService {

    static final int BATCH_SIZE = 200;
    private static final int MAX_SAMPLES = 5;
    private static final Logger log = LoggerFactory.getLogger(ItemMigrationService.class);

    private final ItemRepository items;
    private final ModuleQueryService modules;
    private final CollectionAccessService access;
    private final CurrentUserService currentUser;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate tx;

    public ItemMigrationService(ItemRepository items, ModuleQueryService modules, CollectionAccessService access,
                                CurrentUserService currentUser, ObjectMapper objectMapper, TransactionTemplate tx) {
        this.items = items;
        this.modules = modules;
        this.access = access;
        this.currentUser = currentUser;
        this.objectMapper = objectMapper;
        this.tx = tx;
    }

    /** Working state of one run: the module, and the steps each earlier version needs, worked out once per version. */
    private static final class Run {
        final UUID collectionId;
        final UUID moduleId;
        final ModuleContract contract;
        private final Map<String, List<MigrationStep>> stepsByVersion = new HashMap<>();

        Run(UUID collectionId, UUID moduleId, ModuleContract contract) {
            this.collectionId = collectionId;
            this.moduleId = moduleId;
            this.contract = contract;
        }

        String target() {
            return contract.version();
        }

        List<MigrationStep> stepsFor(String fromVersion) {
            return stepsByVersion.computeIfAbsent(fromVersion, v -> MigrationEngine.stepsSince(contract, v));
        }

        /** An item saved under a newer version than the module now has is left alone. */
        boolean isBehind(ItemEntity item) {
            return ModuleVersion.compare(item.getModuleVersion(), target()) < 0;
        }
    }

    public MigrationPreviewResponse preview(UUID collectionId, UUID moduleId) {
        Run run = start(collectionId, moduleId);
        long[] counts = new long[2];
        Map<String, Long> versions = new TreeMap<>(ModuleVersion::compare);
        List<MigrationPreviewResponse.Sample> samples = new ArrayList<>();

        scan(run, false, batch -> {
            for (ItemEntity item : batch) {
                if (!run.isBehind(item)) continue;
                ObjectNode before = readAttributes(item);
                if (before == null) continue;
                counts[0]++;
                versions.merge(item.getModuleVersion(), 1L, Long::sum);
                ObjectNode after = MigrationEngine.apply(run.contract, run.stepsFor(item.getModuleVersion()), before);
                if (after.equals(before)) continue;
                counts[1]++;
                if (samples.size() < MAX_SAMPLES) {
                    samples.add(new MigrationPreviewResponse.Sample(item.getId(), item.getTitle(), diff(before, after)));
                }
            }
        });

        List<MigrationPreviewResponse.VersionCount> byVersion = versions.entrySet().stream()
                .map(e -> new MigrationPreviewResponse.VersionCount(e.getKey(), e.getValue())).toList();
        return new MigrationPreviewResponse(run.target(), counts[0], counts[1], byVersion, List.copyOf(samples));
    }

    public MigrationResultResponse apply(UUID collectionId, UUID moduleId) {
        Run run = start(collectionId, moduleId);
        long[] counts = new long[3];

        scan(run, true, batch -> {
            for (ItemEntity item : batch) {
                if (!run.isBehind(item)) continue;
                ObjectNode before = readAttributes(item);
                if (before == null) {
                    counts[2]++;
                    continue;
                }
                ObjectNode after = MigrationEngine.apply(run.contract, run.stepsFor(item.getModuleVersion()), before);
                if (!after.equals(before)) {
                    item.setAttributes(after.toString());
                    counts[1]++;
                }
                item.setModuleVersion(run.target());
                counts[0]++;
            }
        });

        log.info("Items migrated: collectionId={} moduleId={} version={} migrated={} changed={} skipped={} byUserId={}",
                collectionId, moduleId, run.target(), counts[0], counts[1], counts[2], currentUser.requireCurrentUser().getId());
        return new MigrationResultResponse(counts[0], counts[1], counts[2]);
    }

    private Run start(UUID collectionId, UUID moduleId) {
        access.requireRole(collectionId, currentUser.requireCurrentUser().getId(), Role.ADMIN);
        ModuleDefinitionEntity module = modules.getEntityById(moduleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "MODULE_NOT_FOUND"));
        return new Run(collectionId, moduleId, modules.getContract(module));
    }

    /**
     * Hands every item of the module that is not on the current version to {@code handler}, a batch at a time. Batches are keyed
     * on creation order rather than on offset, so items that leave the set while it runs do not shift the rest. When
     * {@code write} is set each batch is read and handled inside one transaction, and changes made to its items are saved.
     */
    private void scan(Run run, boolean write, Consumer<List<ItemEntity>> handler) {
        ItemEntity last = null;
        while (true) {
            ItemEntity after = last;
            List<ItemEntity> batch = write
                    ? tx.execute(status -> {
                        List<ItemEntity> rows = nextBatch(run, after);
                        handler.accept(rows);
                        return rows;
                    })
                    : nextBatch(run, after);
            if (batch == null || batch.isEmpty()) return;
            if (!write) handler.accept(batch);
            last = batch.getLast();
            if (batch.size() < BATCH_SIZE) return;
        }
    }

    private List<ItemEntity> nextBatch(Run run, ItemEntity after) {
        PageRequest page = PageRequest.of(0, BATCH_SIZE);
        return after == null
                ? items.findBehindFirstBatch(run.collectionId, run.moduleId, run.target(), page)
                : items.findBehindBatchAfter(run.collectionId, run.moduleId, run.target(), after.getCreatedAt(), after.getId(), page);
    }

    /** Null when the stored attributes are not a JSON object, which the migration cannot work on. */
    private ObjectNode readAttributes(ItemEntity item) {
        try {
            return objectMapper.readTree(item.getAttributes()) instanceof ObjectNode object ? object : null;
        } catch (JacksonException e) {
            log.warn("Item attributes unreadable, skipped by migration: itemId={}", item.getId());
            return null;
        }
    }

    private static List<MigrationPreviewResponse.FieldChange> diff(ObjectNode before, ObjectNode after) {
        TreeSet<String> keys = new TreeSet<>(before.propertyNames());
        keys.addAll(after.propertyNames());
        List<MigrationPreviewResponse.FieldChange> changes = new ArrayList<>();
        for (String key : keys) {
            JsonNode was = before.get(key);
            JsonNode now = after.get(key);
            if (!java.util.Objects.equals(was, now)) changes.add(new MigrationPreviewResponse.FieldChange(key, was, now));
        }
        return changes;
    }
}
