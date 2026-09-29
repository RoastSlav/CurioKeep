package org.rostislav.curiokeep.items;

import org.apache.hc.client5.http.SystemDefaultDnsResolver;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Downloads a remote file for a user-supplied URL without letting that URL reach internal services.
 * <p>
 * Every connection, including each redirect hop, is checked when the host name is resolved, so a name that resolves to a
 * private address (or changes its answer between a check and the connection) is refused. Only http and https on the
 * standard ports are allowed, and the body is read with a hard size cap.
 */
@Component
class RemoteImageFetcher {

    static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final int MAX_REDIRECTS = 3;
    private static final Logger log = LoggerFactory.getLogger(RemoteImageFetcher.class);

    private final RestClient client;

    private final Predicate<URI> uriAllowed;

    @Autowired
    RemoteImageFetcher(RestClient.Builder builder) {
        this(builder, OutboundAddressPolicy::isPublic, RemoteImageFetcher::isAllowedUri);
    }

    /** The policies are parameters only so tests can reach a local server; production always uses the strict ones. */
    RemoteImageFetcher(RestClient.Builder builder, Predicate<InetAddress> addressAllowed, Predicate<URI> uriAllowed) {
        this.uriAllowed = uriAllowed;
        PoolingHttpClientConnectionManager connections = PoolingHttpClientConnectionManagerBuilder.create()
                .setDnsResolver(new AllowedAddressesOnly(addressAllowed))
                .setDefaultConnectionConfig(ConnectionConfig.custom().setConnectTimeout(Timeout.ofSeconds(5)).build())
                .build();
        CloseableHttpClient http = HttpClients.custom()
                .setConnectionManager(connections)
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectionRequestTimeout(Timeout.ofSeconds(5))
                        .setResponseTimeout(Timeout.ofSeconds(10))
                        .setMaxRedirects(MAX_REDIRECTS)
                        .build())
                .build();
        this.client = builder.clone().requestFactory(new HttpComponentsClientHttpRequestFactory(http)).build();
    }

    /** The response body, or empty when the URL is not allowed, the request fails, or the body is too large. */
    Optional<byte[]> fetch(String url) {
        URI uri = parse(url);
        if (uri == null || !uriAllowed.test(uri)) {
            log.warn("Refused to download from a disallowed URL");
            return Optional.empty();
        }
        try {
            return client.get().uri(uri).exchange((request, response) -> {
                if (!response.getStatusCode().is2xxSuccessful()) return Optional.<byte[]>empty();
                if (response.getHeaders().getContentLength() > MAX_BYTES) return Optional.<byte[]>empty();
                byte[] body = response.getBody().readNBytes(MAX_BYTES + 1);
                return body.length > MAX_BYTES ? Optional.<byte[]>empty() : Optional.of(body);
            });
        } catch (RuntimeException e) {
            log.warn("Download from {} failed: {}", uri.getHost(), e.getMessage());
            return Optional.empty();
        }
    }

    private static URI parse(String url) {
        try {
            return URI.create(url.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean isAllowedUri(URI uri) {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        boolean https = scheme.equals("https");
        if ((!https && !scheme.equals("http")) || uri.getHost() == null || uri.getUserInfo() != null) return false;
        int port = uri.getPort();
        return port == -1 || port == (https ? 443 : 80);
    }

    private static final class AllowedAddressesOnly extends SystemDefaultDnsResolver {
        private final Predicate<InetAddress> allowed;

        AllowedAddressesOnly(Predicate<InetAddress> allowed) {
            this.allowed = allowed;
        }

        @Override
        public InetAddress[] resolve(String host) throws UnknownHostException {
            InetAddress[] addresses = super.resolve(host);
            for (InetAddress address : addresses) {
                if (!allowed.test(address)) {
                    throw new UnknownHostException("Refusing to connect to a non-public address");
                }
            }
            return addresses;
        }
    }
}
