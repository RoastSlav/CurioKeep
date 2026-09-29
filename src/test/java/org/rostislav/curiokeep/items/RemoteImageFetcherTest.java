package org.rostislav.curiokeep.items;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class RemoteImageFetcherTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};

    private HttpServer server;
    private String base;
    private RemoteImageFetcher lenient;
    private final RemoteImageFetcher strict = new RemoteImageFetcher(RestClient.builder());

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ok", exchange -> respond(exchange, 200, PNG));
        server.createContext("/missing", exchange -> respond(exchange, 404, new byte[0]));
        server.createContext("/big", exchange -> respond(exchange, 200, new byte[RemoteImageFetcher.MAX_BYTES + 1]));
        server.createContext("/limit", exchange -> respond(exchange, 200, new byte[RemoteImageFetcher.MAX_BYTES]));
        server.createContext("/redirect", exchange -> {
            exchange.getResponseHeaders().add("Location", "/ok");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();
        // Local test server: allow loopback and any port, everything else about the fetcher is unchanged.
        lenient = new RemoteImageFetcher(RestClient.builder(), address -> true, uri -> true);
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, int status, byte[] body) throws IOException {
        exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
        if (body.length > 0) exchange.getResponseBody().write(body);
        exchange.close();
    }

    @Test
    void returnsTheBodyOfASuccessfulResponse() {
        assertThat(lenient.fetch(base + "/ok")).hasValueSatisfying(body -> assertThat(body).isEqualTo(PNG));
    }

    @Test
    void followsARedirect() {
        assertThat(lenient.fetch(base + "/redirect")).hasValueSatisfying(body -> assertThat(body).isEqualTo(PNG));
    }

    @Test
    void returnsNothingForAnErrorStatus() {
        assertThat(lenient.fetch(base + "/missing")).isEmpty();
    }

    @Test
    void refusesABodyOverTheLimitButAcceptsOneAtTheLimit() {
        assertThat(lenient.fetch(base + "/big")).isEmpty();
        assertThat(lenient.fetch(base + "/limit")).hasValueSatisfying(body -> assertThat(body).hasSize(RemoteImageFetcher.MAX_BYTES));
    }

    @Test
    void refusesToConnectWhenTheAddressPolicyRejectsTheResolvedAddress() {
        RemoteImageFetcher noAddresses = new RemoteImageFetcher(RestClient.builder(), address -> false, uri -> true);

        assertThat(noAddresses.fetch(base + "/ok")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://127.0.0.1/x.png",
            "http://localhost/x.png",
            "http://[::1]/x.png",
            "http://10.0.0.5/x.png",
            "http://192.168.1.1/x.png",
            "http://169.254.169.254/latest/meta-data/",
            "http://0.0.0.0/x.png",
            "https://127.0.0.1/x.png"
    })
    void productionFetcherNeverConnectsToInternalAddresses(String url) {
        assertThat(strict.fetch(url)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ftp://example.com/x.png",
            "file:///etc/passwd",
            "gopher://example.com/",
            "http://example.com:8080/x.png",
            "http://example.com:5432/",
            "https://example.com:8443/x.png",
            "http://user:pw@example.com/x.png",
            "not a url",
            "/relative/path.png",
            ""
    })
    void productionFetcherRejectsOtherSchemesPortsAndCredentials(String url) {
        assertThat(strict.fetch(url)).isEmpty();
    }

    @Test
    void theFixtureIsAValidPngHeader() {
        assertThat(ImageFormat.detect(Arrays.copyOf(PNG, PNG.length))).contains(ImageFormat.PNG);
    }
}
