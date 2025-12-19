package org.rostislav.curiokeep.modules.importing;

import org.rostislav.curiokeep.modules.ModuleProps;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ModuleImportStorage {
    private static final String DEFAULT_IMPORT_DIR = "./data/modules-imported";

    private final Path importDir;

    public ModuleImportStorage(ModuleProps props) {
        String candidate = props.importDir();
        if (candidate == null || candidate.isBlank()) {
            candidate = DEFAULT_IMPORT_DIR;
        }
        this.importDir = Paths.get(candidate).toAbsolutePath().normalize();
    }

    public Path getImportDir() {
        return importDir;
    }

    public void ensureDir() {
        try {
            Files.createDirectories(importDir);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to ensure module import directory exists: " + importDir, e);
        }
    }

    public Path saveXml(String fileName, byte[] bytes) {
        ensureDir();
        Path target = importDir.resolve(sanitizeFileName(fileName));
        try {
            Path temp = Files.createTempFile(importDir, "module-import-", ".tmp");
            Files.write(temp, bytes);
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            return target;
        } catch (FileAlreadyExistsException fae) {
            // Should not happen because REPLACE_EXISTING is used, but fall back just in case.
            return target;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save imported module XML: " + target, e);
        }
    }

    public List<Path> listXmlFiles() {
        ensureDir();
        try (Stream<Path> stream = Files.list(importDir)) {
            return stream
                    .filter((path) -> Files.isRegularFile(path) && path.getFileName().toString().toLowerCase().endsWith(".xml"))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to list imported module files in " + importDir, e);
        }
    }

    public void deleteXml(Path file) {
        Path normalized = file.toAbsolutePath().normalize();
        if (!normalized.startsWith(importDir)) {
            throw new IllegalArgumentException("Cannot delete file outside of import directory: " + file);
        }
        try {
            Files.deleteIfExists(normalized);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to delete imported module file: " + normalized, e);
        }
    }

    private Path sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("Module file name cannot be empty");
        }
        Path name;
        try {
            name = Paths.get(fileName).getFileName();
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("Invalid module file name: " + fileName, e);
        }
        if (name == null) {
            throw new IllegalArgumentException("Module file name cannot be resolved: " + fileName);
        }
        String normalized = name.toString().replaceAll("[^a-zA-Z0-9._-]", "_");
        if (!normalized.toLowerCase().endsWith(".xml")) {
            normalized = normalized + ".xml";
        }
        return Paths.get(normalized);
    }
}