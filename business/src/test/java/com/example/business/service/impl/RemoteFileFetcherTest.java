package com.example.business.service.impl;

import com.example.business.config.UploadSecurityProperties;
import com.example.business.service.RemoteFileFetcher;
import com.example.common.exception.CustomValidationException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RemoteFileFetcherTest {

    @Test
    void rejectsUnsupportedSchemeAndCredentialBearingUri() {
        HttpRemoteFileFetcher fetcher = fetcher((host) -> publicAddresses(), unusedFactory());

        assertThrows(CustomValidationException.class, () -> fetcher.fetch(URI.create("file:///tmp/image.png")));
        assertThrows(CustomValidationException.class, () -> fetcher.fetch(URI.create("https://user:pass@public.example/image.png")));
    }

    @Test
    void rejectsPrivateLoopbackAndMetadataAddresses() {
        assertPrivate("127.0.0.1");
        assertPrivate("10.0.0.1");
        assertPrivate("172.16.0.1");
        assertPrivate("192.168.1.1");
        assertPrivate("169.254.169.254");
        assertPrivate("100.100.100.200");
        assertPrivate("100.64.0.1");
        assertPrivate("::1");
        assertPrivate("fe80::1");
        assertPrivate("fc00::1");
    }

    @Test
    void rejectsMixedDnsAnswersWhenOneAddressIsPrivate() {
        HttpRemoteFileFetcher fetcher = fetcher(host -> new InetAddress[]{
                address("93.184.216.34"),
                address("10.0.0.7")
        }, unusedFactory());

        assertThrows(CustomValidationException.class,
                () -> fetcher.fetch(URI.create("https://mixed.example/image.png")));
    }

    @Test
    void appliesOptionalAllowlistToExactHostsAndSubdomains() {
        UploadSecurityProperties properties = properties();
        properties.setRemoteAllowedHosts(List.of("cdn.example.com"));
        AtomicInteger opens = new AtomicInteger();
        HttpRemoteFileFetcher fetcher = fetcher(properties, host -> publicAddresses(),
                (uri, resolvedAddresses, connectTimeout, readTimeout) -> {
                    opens.incrementAndGet();
                    return response(200, null, "image/png", new byte[]{1, 2, 3});
                });

        RemoteFileFetcher.RemoteFilePayload payload = fetcher.fetch(URI.create("https://img.cdn.example.com/image.png"));
        assertArrayEquals(new byte[]{1, 2, 3}, payload.bytes());
        assertEquals(1, opens.get());
        assertThrows(CustomValidationException.class,
                () -> fetcher.fetch(URI.create("https://cdn.example.com.evil/image.png")));
    }

    @Test
    void revalidatesRedirectTargetsAndRejectsPrivateDestination() {
        HttpRemoteFileFetcher fetcher = fetcher(host -> host.equals("public.example")
                        ? publicAddresses() : new InetAddress[]{address("127.0.0.1")},
                (uri, resolvedAddresses, connectTimeout, readTimeout) -> response(302, "http://private.example/image.png", "image/png", new byte[0]));

        assertThrows(CustomValidationException.class,
                () -> fetcher.fetch(URI.create("https://public.example/image.png")));
    }

    @Test
    void enforcesRedirectLimit() {
        UploadSecurityProperties properties = properties();
        properties.setMaxRedirects(1);
        AtomicInteger calls = new AtomicInteger();
        HttpRemoteFileFetcher fetcher = fetcher(properties, host -> publicAddresses(),
                (uri, resolvedAddresses, connectTimeout, readTimeout) -> {
                    calls.incrementAndGet();
                    return response(302, uri.resolve("next").toString(), "image/png", new byte[0]);
                });

        assertThrows(CustomValidationException.class,
                () -> fetcher.fetch(URI.create("https://public.example/image.png")));
        assertEquals(2, calls.get());
    }

    @Test
    void rejectsDeclaredAndStreamedBodiesAboveLimit() {
        UploadSecurityProperties properties = properties();
        properties.setRemoteMaxBytes(4);
        HttpRemoteFileFetcher declaredTooLarge = fetcher(properties, host -> publicAddresses(),
                (uri, resolvedAddresses, connectTimeout, readTimeout) -> response(200, null, "image/png", 5L, new byte[]{1}));
        assertThrows(CustomValidationException.class,
                () -> declaredTooLarge.fetch(URI.create("https://public.example/image.png")));

        HttpRemoteFileFetcher streamedTooLarge = fetcher(properties, host -> publicAddresses(),
                (uri, resolvedAddresses, connectTimeout, readTimeout) -> response(200, null, "image/png", null, new byte[]{1, 2, 3, 4, 5}));
        assertThrows(CustomValidationException.class,
                () -> streamedTooLarge.fetch(URI.create("https://public.example/image.png")));
    }

    @Test
    void appliesTimeoutConfigurationAndReturnsFinalPayload() {
        UploadSecurityProperties properties = properties();
        properties.setConnectTimeoutMillis(1234);
        properties.setReadTimeoutMillis(5678);
        AtomicInteger capturedConnect = new AtomicInteger();
        AtomicInteger capturedRead = new AtomicInteger();
        AtomicReference<InetAddress[]> capturedAddresses = new AtomicReference<>();
        HttpRemoteFileFetcher fetcher = fetcher(properties, host -> publicAddresses(),
                (uri, resolvedAddresses, connectTimeout, readTimeout) -> {
                    capturedAddresses.set(resolvedAddresses);
                    capturedConnect.set(connectTimeout);
                    capturedRead.set(readTimeout);
                    return response(200, null, "image/png", new byte[]{8, 9});
                });

        RemoteFileFetcher.RemoteFilePayload payload = fetcher.fetch(URI.create("https://public.example/image.png"));
        assertEquals(1234, capturedConnect.get());
        assertEquals(5678, capturedRead.get());
        assertEquals("93.184.216.34", capturedAddresses.get()[0].getHostAddress());
        assertEquals("image/png", payload.contentType());
        assertEquals("image.png", payload.filename());
        assertArrayEquals(new byte[]{8, 9}, payload.bytes());
    }

    @Test
    void acceptsSafeIpv6LiteralAndPinsItsResolvedAddress() {
        InetAddress publicIpv6 = address("2001:4860:4860::8888");
        AtomicReference<String> resolvedHost = new AtomicReference<>();
        AtomicReference<InetAddress[]> capturedAddresses = new AtomicReference<>();
        HttpRemoteFileFetcher fetcher = fetcher(host -> {
            resolvedHost.set(host);
            return new InetAddress[]{publicIpv6};
        }, (uri, resolvedAddresses, connectTimeout, readTimeout) -> {
            capturedAddresses.set(resolvedAddresses);
            return response(200, null, "image/png", new byte[]{8, 9});
        });

        fetcher.fetch(URI.create("https://[2001:4860:4860::8888]/image.png"));

        assertEquals("2001:4860:4860::8888", resolvedHost.get());
        assertEquals(publicIpv6, capturedAddresses.get()[0]);
    }

    private void assertPrivate(String address) {
        HttpRemoteFileFetcher fetcher = fetcher(host -> new InetAddress[]{address(address)}, unusedFactory());
        assertThrows(CustomValidationException.class,
                () -> fetcher.fetch(URI.create("https://public.example/image.png")));
    }

    private static HttpRemoteFileFetcher fetcher(HttpRemoteFileFetcher.HostResolver resolver,
                                                  HttpRemoteFileFetcher.RemoteConnectionFactory factory) {
        return fetcher(properties(), resolver, factory);
    }

    private static HttpRemoteFileFetcher fetcher(UploadSecurityProperties properties,
                                                  HttpRemoteFileFetcher.HostResolver resolver,
                                                  HttpRemoteFileFetcher.RemoteConnectionFactory factory) {
        return new HttpRemoteFileFetcher(properties, resolver, factory);
    }

    private static HttpRemoteFileFetcher.RemoteConnectionFactory unusedFactory() {
        return (uri, resolvedAddresses, connectTimeout, readTimeout) -> {
            throw new AssertionError("connection should not be opened");
        };
    }

    private static UploadSecurityProperties properties() {
        return new UploadSecurityProperties();
    }

    private static InetAddress[] publicAddresses() {
        return new InetAddress[]{address("93.184.216.34")};
    }

    private static InetAddress address(String value) {
        try {
            return InetAddress.getByName(value);
        } catch (UnknownHostException exception) {
            throw new AssertionError(exception);
        }
    }

    private static FakeConnection response(int status, String location, String contentType, byte[] body) {
        return response(status, location, contentType, null, body);
    }

    private static FakeConnection response(int status, String location, String contentType, Long contentLength, byte[] body) {
        Map<String, String> headers = new HashMap<>();
        if (location != null) {
            headers.put("Location", location);
        }
        if (contentType != null) {
            headers.put("Content-Type", contentType);
        }
        if (contentLength != null) {
            headers.put("Content-Length", contentLength.toString());
        }
        return new FakeConnection(status, headers, body);
    }

    private static final class FakeConnection implements HttpRemoteFileFetcher.RemoteConnection {
        private final int status;
        private final Map<String, String> headers;
        private final byte[] body;

        private FakeConnection(int status, Map<String, String> headers, byte[] body) {
            this.status = status;
            this.headers = headers;
            this.body = body;
        }

        @Override
        public int status() {
            return status;
        }

        @Override
        public String header(String name) {
            return headers.get(name);
        }

        @Override
        public URI location() {
            String location = headers.get("Location");
            return location == null ? null : URI.create(location);
        }

        @Override
        public InputStream body() {
            return new ByteArrayInputStream(body);
        }

        @Override
        public void close() {
        }
    }
}
