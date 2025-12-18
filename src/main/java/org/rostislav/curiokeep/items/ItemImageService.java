package org.rostislav.curiokeep.items;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class ItemImageService {

    private static final Logger log = LoggerFactory.getLogger(ItemImageService.class);
    private static final int MAX_BYTES = 5 * 1024 * 1024; // 5MB safety cap

    private final RestClient restClient;
    private final Path baseDir;

    public ItemImageService(RestClient restClient, @Value("${curiokeep.assets.dir:./data/assets}") String baseDir) {
        this.restClient = restClient;
        this.baseDir = Path.of(baseDir);
        try {
            Files.createDirectories(this.baseDir);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create assets directory: " + this.baseDir, e);
        }
    }

    public String downloadToLocal(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            byte[] bytes = fetchBytes(url);
            if (bytes.length == 0) return null;
            if (bytes.length > MAX_BYTES) {
                log.warn("Skipped storing asset (too large): {} length={}b", url, bytes.length);
                return null;
            }

            String contentType = probeContentType(bytes, url);
            if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
                log.warn("Skipped storing asset (non-image): {} contentType={} length={}b", url, contentType, bytes.length);
                return null;
            }

            String ext = extensionFromContentType(contentType);
            if (ext == null) {
                ext = extensionFromUrl(url);
            }
            if (ext == null) {
                ext = "bin";
            }

            String fileName = buildFileName(url, ext);
            Path dest = baseDir.resolve(fileName);
            Files.write(dest, bytes);
            return fileName;
        } catch (Exception ex) {
            log.warn("Failed to cache provider asset {}: {}", url, ex.getMessage());
            return null;
        }
    }

    public Resource load(String fileName) {
        if (fileName == null || fileName.isBlank()) return null;
        if (fileName.contains("..") || fileName.contains("/")) return null;
        Path path = baseDir.resolve(fileName);
        if (!Files.exists(path)) return null;
        return new FileSystemResource(path);
    }

    private byte[] fetchBytes(String url) throws IOException {
        byte[] body = restClient.get()
                .uri(URI.create(url))
                .header(HttpHeaders.USER_AGENT, "CurioKeep/1.0")
                .retrieve()
                .toEntity(byte[].class)
                .getBody();
        return body == null ? new byte[0] : body;
    }

    private String probeContentType(byte[] data, String url) {
        try {
            Path tmp = Files.createTempFile("asset-probe", extensionFromUrl(url) == null ? "" : ("." + extensionFromUrl(url)));
            Files.write(tmp, data);
            String type = Files.probeContentType(tmp);
            Files.deleteIfExists(tmp);
            return type;
        } catch (IOException ignored) {
            return null;
        }
    }

    private String buildFileName(String url, String ext) throws NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(url.getBytes());
        md5.update(Long.toString(Instant.now().toEpochMilli()).getBytes());
        String hash = HexFormat.of().formatHex(md5.digest());
        return hash + "." + ext;
    }

    private String extensionFromContentType(String contentType) {
        if (contentType == null) return null;
        String ct = contentType.toLowerCase(Locale.ROOT);
        if (ct.contains("jpeg") || ct.contains("jpg")) return "jpg";
        if (ct.contains("png")) return "png";
        if (ct.contains("gif")) return "gif";
        if (ct.contains("webp")) return "webp";
        if (ct.contains("bmp")) return "bmp";
        return null;
    }

    private String extensionFromUrl(String url) {
        if (url == null) return null;
        int idx = url.lastIndexOf('.') ;
        if (idx < 0) return null;
        String ext = url.substring(idx + 1).toLowerCase(Locale.ROOT);
        if (ext.contains("?")) {
            ext = ext.substring(0, ext.indexOf('?'));
        }
        if (ext.isBlank()) return null;
        return ext;
    }
}
