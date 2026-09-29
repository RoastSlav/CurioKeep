package org.rostislav.curiokeep.items;

import org.springframework.http.MediaType;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * The raster formats accepted as item covers. The format is decided from the file's own bytes; a client-declared
 * content type or file name is never trusted, so a stored file can only ever be served as one of these image types.
 * SVG is deliberately absent because it can carry script.
 */
enum ImageFormat {
    PNG("png", MediaType.IMAGE_PNG),
    JPEG("jpg", MediaType.IMAGE_JPEG),
    GIF("gif", MediaType.IMAGE_GIF),
    WEBP("webp", MediaType.parseMediaType("image/webp")),
    BMP("bmp", MediaType.parseMediaType("image/bmp"));

    private final String extension;
    private final MediaType mediaType;

    ImageFormat(String extension, MediaType mediaType) {
        this.extension = extension;
        this.mediaType = mediaType;
    }

    String extension() {
        return extension;
    }

    MediaType mediaType() {
        return mediaType;
    }

    static Optional<ImageFormat> detect(byte[] data) {
        if (startsWith(data, 0, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) return Optional.of(PNG);
        if (startsWith(data, 0, 0xFF, 0xD8, 0xFF)) return Optional.of(JPEG);
        if (startsWith(data, 0, 'G', 'I', 'F', '8') && data.length > 5 && (data[4] == '7' || data[4] == '9') && data[5] == 'a') {
            return Optional.of(GIF);
        }
        if (startsWith(data, 0, 'R', 'I', 'F', 'F') && startsWith(data, 8, 'W', 'E', 'B', 'P')) return Optional.of(WEBP);
        if (startsWith(data, 0, 'B', 'M')) return Optional.of(BMP);
        return Optional.empty();
    }

    static Optional<ImageFormat> fromExtension(String extension) {
        String normalized = extension.toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(f -> f.extension.equals(normalized)).findFirst();
    }

    private static boolean startsWith(byte[] data, int offset, int... expected) {
        if (data == null || data.length < offset + expected.length) return false;
        for (int i = 0; i < expected.length; i++) {
            if ((data[offset + i] & 0xFF) != expected[i]) return false;
        }
        return true;
    }
}
