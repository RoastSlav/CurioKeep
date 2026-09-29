package org.rostislav.curiokeep.items;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Stores item cover images on disk under generated names; the type of every file is decided from its content. */
@Service
public class ItemImageService {

    private static final Logger log = LoggerFactory.getLogger(ItemImageService.class);
    private static final Pattern STORED_NAME = Pattern.compile("[0-9a-f]{32}\\.(png|jpg|gif|webp|bmp)");

    public record StoredImage(Resource resource, MediaType mediaType) {
    }

    private final RemoteImageFetcher fetcher;
    private final Path baseDir;

    public ItemImageService(RemoteImageFetcher fetcher, @Value("${curiokeep.assets.dir:./data/assets}") String baseDir) {
        this.fetcher = fetcher;
        this.baseDir = Path.of(baseDir);
        try {
            Files.createDirectories(this.baseDir);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create assets directory: " + this.baseDir, e);
        }
    }

    /** True when the name has the shape this service generates, which rules out path segments and unexpected types. */
    public static boolean isStoredName(String fileName) {
        return fileName != null && STORED_NAME.matcher(fileName).matches();
    }

    public Optional<String> downloadToLocal(String url) {
        if (url == null || url.isBlank()) return Optional.empty();
        return fetcher.fetch(url).flatMap(this::store);
    }

    public Optional<String> storeUploaded(byte[] bytes) {
        return store(bytes);
    }

    public Optional<StoredImage> load(String fileName) {
        Matcher name = STORED_NAME.matcher(fileName == null ? "" : fileName);
        if (!name.matches()) return Optional.empty();
        Path path = baseDir.resolve(fileName);
        if (!Files.isRegularFile(path)) return Optional.empty();
        return ImageFormat.fromExtension(name.group(1))
                .map(format -> new StoredImage(new FileSystemResource(path), format.mediaType()));
    }

    public void delete(String fileName) {
        if (!isStoredName(fileName)) return;
        try {
            Files.deleteIfExists(baseDir.resolve(fileName));
        } catch (IOException e) {
            log.warn("Failed to delete asset {}: {}", fileName, e.getMessage());
        }
    }

    private Optional<String> store(byte[] bytes) {
        if (bytes == null || bytes.length == 0 || bytes.length > RemoteImageFetcher.MAX_BYTES) return Optional.empty();
        Optional<ImageFormat> format = ImageFormat.detect(bytes);
        if (format.isEmpty()) {
            log.warn("Skipped storing a file that is not a supported image ({} bytes)", bytes.length);
            return Optional.empty();
        }
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + format.get().extension();
        try {
            Files.write(baseDir.resolve(fileName), bytes);
        } catch (IOException e) {
            log.warn("Failed to write asset {}: {}", fileName, e.getMessage());
            return Optional.empty();
        }
        return Optional.of(fileName);
    }
}
