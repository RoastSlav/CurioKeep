package org.rostislav.curiokeep.modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.rostislav.curiokeep.collections.CollectionModuleRepository;
import org.rostislav.curiokeep.items.ItemRepository;
import org.rostislav.curiokeep.modules.api.dto.ModuleSummaryResponse;
import org.rostislav.curiokeep.modules.api.dto.ScanModulesResponse;
import org.rostislav.curiokeep.modules.contract.ModuleSource;
import org.rostislav.curiokeep.modules.entities.ModuleDefinitionEntity;
import org.rostislav.curiokeep.modules.importing.ModuleImportStorage;
import org.rostislav.curiokeep.modules.xml.ModuleXml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class ModuleImportService {
    private static final Logger log = LoggerFactory.getLogger(ModuleImportService.class);

    private final ModuleLoadTx moduleLoadTx;
    private final ModuleDefinitionRepository moduleDefinitionRepository;
    private final ModuleImportStorage moduleImportStorage;
    private final ModuleXmlParser moduleXmlParser;
    private final CollectionModuleRepository collectionModuleRepository;
    private final ItemRepository itemRepository;

    public ModuleImportService(ModuleLoadTx moduleLoadTx,
                               ModuleDefinitionRepository moduleDefinitionRepository,
                               ModuleImportStorage moduleImportStorage,
                               ModuleXmlParser moduleXmlParser,
                               CollectionModuleRepository collectionModuleRepository,
                               ItemRepository itemRepository) {
        this.moduleLoadTx = moduleLoadTx;
        this.moduleDefinitionRepository = moduleDefinitionRepository;
        this.moduleImportStorage = moduleImportStorage;
        this.moduleXmlParser = moduleXmlParser;
        this.collectionModuleRepository = collectionModuleRepository;
        this.itemRepository = itemRepository;
    }

    public ModuleSummaryResponse importFromBytes(byte[] xmlBytes, String sourceName, boolean persistFile) {
        if (xmlBytes == null || xmlBytes.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Module XML is required");
        }

        String xml = new String(xmlBytes, StandardCharsets.UTF_8);
        ModuleXml moduleXml;
        try {
            moduleXml = moduleXmlParser.parse(xml);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid module XML: " + sourceName, e);
        }

        String moduleKey = Optional.ofNullable(moduleXml.key())
                .map(String::trim)
                .orElse("");
        if (moduleKey.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Module key is required");
        }

        if (moduleDefinitionRepository.existsByModuleKeyIgnoreCase(moduleKey)) {
            throw new ModuleAlreadyExistsException(moduleKey);
        }

        String resolvedSourceName = (sourceName == null || sourceName.isBlank())
                ? moduleKey + ".xml"
                : sourceName;

        Path savedPath = null;
        if (persistFile) {
            String fileName = buildFileName(moduleXml, resolvedSourceName);
            savedPath = moduleImportStorage.saveXml(fileName, xmlBytes);
        }

        try {
            moduleLoadTx.loadOneModule(new ByteArrayResource(xmlBytes), resolvedSourceName, ModuleSource.IMPORTED);
        } catch (ResponseStatusException ex) {
            cleanupSavedFile(savedPath);
            throw ex;
        } catch (IllegalStateException ex) {
            cleanupSavedFile(savedPath);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (Exception ex) {
            cleanupSavedFile(savedPath);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to import module: " + resolvedSourceName, ex);
        }

        return loadSummary(moduleKey);
    }

    public ModuleSummaryResponse importFromFile(Path file) {
        try {
            byte[] bytes = Files.readAllBytes(file);
            return importFromBytes(bytes, file.getFileName().toString(), false);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot read imported module file: " + file.getFileName(), e);
        }
    }

    public ScanModulesResponse scanImportDir() {
        List<ModuleSummaryResponse> imported = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        List<ScanModulesResponse.ScanFailure> failed = new ArrayList<>();

        for (Path file : moduleImportStorage.listXmlFiles()) {
            try {
                imported.add(importFromFile(file));
            } catch (ModuleAlreadyExistsException ex) {
                skipped.add(ex.moduleKey());
            } catch (ResponseStatusException ex) {
                String reason = ex.getReason() != null ? ex.getReason() : ex.getMessage();
                failed.add(new ScanModulesResponse.ScanFailure(file.getFileName().toString(), reason));
            } catch (Exception ex) {
                String reason = ex.getMessage() != null ? ex.getMessage() : "Unknown error";
                failed.add(new ScanModulesResponse.ScanFailure(file.getFileName().toString(), reason));
            }
        }

        return new ScanModulesResponse(imported, skipped, failed);
    }

    @Transactional
    public void deleteImportedModule(String moduleKeyRaw) {
        String normalizedKey = normalizeKey(moduleKeyRaw);
        ModuleDefinitionEntity module = moduleDefinitionRepository.findByModuleKeyIgnoreCase(normalizedKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Module not found"));

        if (module.getSource() != ModuleSource.IMPORTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only imported modules can be deleted");
        }

        if (collectionModuleRepository.existsByIdModuleId(module.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Module is enabled in one or more collections");
        }

        if (itemRepository.existsByModuleId(module.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Module has associated items");
        }

        findFileForModule(module.getModuleKey()).ifPresent(moduleImportStorage::deleteXml);
        moduleDefinitionRepository.delete(module);
    }

    private void cleanupSavedFile(Path savedPath) {
        if (savedPath == null) return;
        try {
            moduleImportStorage.deleteXml(savedPath);
        } catch (Exception e) {
            log.warn("Failed to clean up imported module file {}", savedPath, e);
        }
    }

    private ModuleSummaryResponse loadSummary(String moduleKey) {
        ModuleDefinitionEntity entity = moduleDefinitionRepository.findByModuleKeyIgnoreCase(moduleKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Module could not be loaded after import"));
        return ModuleSummaryResponse.from(entity);
    }

    private Optional<Path> findFileForModule(String moduleKey) {
        for (Path p : moduleImportStorage.listXmlFiles()) {
            try {
                ModuleXml xml = moduleXmlParser.parse(Files.readString(p, StandardCharsets.UTF_8));
                if (xml.key() != null && xml.key().equalsIgnoreCase(moduleKey)) {
                    return Optional.of(p);
                }
            } catch (Exception e) {
                log.debug("Failed to parse imported module file {}: {}", p, e.getMessage());
            }
        }
        return Optional.empty();
    }

    private String buildFileName(ModuleXml xml, String fallback) {
        StringBuilder builder = new StringBuilder();
        if (xml.key() != null && !xml.key().isBlank()) {
            builder.append(xml.key().trim());
        }
        if (xml.version() != null && !xml.version().isBlank()) {
            if (builder.length() > 0) builder.append("-");
            builder.append(xml.version().trim());
        }
        String candidate = builder.length() > 0 ? builder.toString() : fallback;
        if (candidate == null || candidate.isBlank()) {
            candidate = "imported-module-" + System.currentTimeMillis();
        }
        if (!candidate.toLowerCase(Locale.ROOT).endsWith(".xml")) {
            candidate = candidate + ".xml";
        }
        return candidate;
    }

    private String normalizeKey(String raw) {
        if (raw == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "moduleKey is required");
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "moduleKey is required");
        }
        return trimmed;
    }
}
