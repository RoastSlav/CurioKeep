package org.rostislav.curiokeep.items;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ItemImageServiceTest {

    private static final byte[] PNG = bytes(0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0);
    private static final byte[] JPEG = bytes(0xFF, 0xD8, 0xFF, 0xE0, 0, 0);
    private static final byte[] GIF = bytes('G', 'I', 'F', '8', '9', 'a', 0, 0);
    private static final byte[] WEBP = bytes('R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P');
    private static final byte[] BMP = bytes('B', 'M', 0, 0, 0, 0);

    @TempDir
    Path dir;

    private RemoteImageFetcher fetcher;
    private ItemImageService service;

    private static byte[] bytes(int... values) {
        byte[] out = new byte[values.length];
        for (int i = 0; i < values.length; i++) out[i] = (byte) values[i];
        return out;
    }

    @BeforeEach
    void setUp() {
        fetcher = mock(RemoteImageFetcher.class);
        service = new ItemImageService(fetcher, dir.toString());
    }

    private long storedFiles() throws IOException {
        try (Stream<Path> files = Files.list(dir)) {
            return files.count();
        }
    }

    @Test
    void storesAnImageUnderAGeneratedNameWithTheExtensionOfItsRealType() {
        assertThat(service.storeUploaded(PNG)).hasValueSatisfying(name -> assertThat(name).matches("[0-9a-f]{32}\\.png"));
        assertThat(service.storeUploaded(JPEG)).hasValueSatisfying(name -> assertThat(name).endsWith(".jpg"));
        assertThat(service.storeUploaded(GIF)).hasValueSatisfying(name -> assertThat(name).endsWith(".gif"));
        assertThat(service.storeUploaded(WEBP)).hasValueSatisfying(name -> assertThat(name).endsWith(".webp"));
        assertThat(service.storeUploaded(BMP)).hasValueSatisfying(name -> assertThat(name).endsWith(".bmp"));
    }

    @Test
    void refusesFilesThatCanCarryScriptWhateverTheyClaimToBe() throws IOException {
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>".getBytes(StandardCharsets.UTF_8);
        byte[] html = "<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8);

        assertThat(service.storeUploaded(svg)).isEmpty();
        assertThat(service.storeUploaded(html)).isEmpty();
        assertThat(service.storeUploaded(new byte[0])).isEmpty();
        assertThat(storedFiles()).isZero();
    }

    @Test
    void refusesAnImageOverTheSizeLimit() throws IOException {
        byte[] oversize = new byte[RemoteImageFetcher.MAX_BYTES + 1];
        System.arraycopy(PNG, 0, oversize, 0, PNG.length);

        assertThat(service.storeUploaded(oversize)).isEmpty();
        assertThat(storedFiles()).isZero();
    }

    @Test
    void storesADownloadedImageAndIgnoresAFailedDownload() {
        when(fetcher.fetch("https://example.com/a.png")).thenReturn(Optional.of(PNG));
        when(fetcher.fetch("https://example.com/none.png")).thenReturn(Optional.empty());

        assertThat(service.downloadToLocal("https://example.com/a.png")).isPresent();
        assertThat(service.downloadToLocal("https://example.com/none.png")).isEmpty();
        assertThat(service.downloadToLocal(" ")).isEmpty();
    }

    @Test
    void loadsOnlyFilesItStoredAndReportsTheirRealMediaType() {
        String name = service.storeUploaded(PNG).orElseThrow();

        assertThat(service.load(name)).hasValueSatisfying(image -> assertThat(image.mediaType()).isEqualTo(MediaType.IMAGE_PNG));
        assertThat(service.load("0".repeat(32) + ".png")).isEmpty();
        assertThat(service.load("../" + name)).isEmpty();
        assertThat(service.load("..\\" + name)).isEmpty();
        assertThat(service.load("evil.html")).isEmpty();
        assertThat(service.load(name.replace(".png", ".svg"))).isEmpty();
        assertThat(service.load(null)).isEmpty();
    }

    @Test
    void doesNotServeALegacyFileWithAnUnsafeExtension() throws IOException {
        Files.writeString(dir.resolve("1496f0b6112bf2b340910f41c042f2d0.html"), "<script>alert(1)</script>");

        assertThat(service.load("1496f0b6112bf2b340910f41c042f2d0.html")).isEmpty();
    }

    @Test
    void deletesOnlyItsOwnFiles() throws IOException {
        String name = service.storeUploaded(PNG).orElseThrow();
        Path outside = Files.writeString(dir.getParent().resolve("outside.png"), "keep");

        service.delete("../outside.png");
        service.delete(name);

        assertThat(outside).exists();
        assertThat(storedFiles()).isZero();
        Files.deleteIfExists(outside);
    }
}
