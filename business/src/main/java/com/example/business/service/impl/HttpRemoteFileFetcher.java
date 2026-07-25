package com.example.business.service.impl;

import com.example.business.config.UploadSecurityProperties;
import com.example.business.service.RemoteFileFetcher;
import com.example.common.exception.CustomValidationException;
import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

@Service
public class HttpRemoteFileFetcher implements RemoteFileFetcher {
    @FunctionalInterface
    interface HostResolver {
        InetAddress[] resolve(String host) throws UnknownHostException;
    }

    interface RemoteConnection extends AutoCloseable {
        int status() throws IOException;

        String header(String name);

        URI location();

        InputStream body() throws IOException;

        @Override
        void close() throws IOException;
    }

    @FunctionalInterface
    interface RemoteConnectionFactory {
        RemoteConnection open(URI uri, InetAddress[] resolvedAddresses,
                              int connectTimeoutMillis, int readTimeoutMillis) throws IOException;
    }

    private final UploadSecurityProperties properties;
    private final RemoteAddressValidator addressValidator;
    private final RemoteConnectionFactory connectionFactory;

    @Autowired
    public HttpRemoteFileFetcher(UploadSecurityProperties properties) {
        this(properties, InetAddress::getAllByName, HttpRemoteFileFetcher::openHttpConnection);
    }

    HttpRemoteFileFetcher(UploadSecurityProperties properties, HostResolver resolver,
                          RemoteConnectionFactory connectionFactory) {
        this.properties = properties;
        this.addressValidator = new RemoteAddressValidator(resolver, properties.getRemoteAllowedHosts());
        this.connectionFactory = connectionFactory;
    }

    @Override
    public RemoteFilePayload fetch(URI source) {
        URI current = source;
        for (int redirectCount = 0; ; ) {
            RemoteAddressValidator.ResolvedTarget target = addressValidator.validate(current);
            try (RemoteConnection connection = connectionFactory.open(current, target.addresses(),
                    properties.getConnectTimeoutMillis(), properties.getReadTimeoutMillis())) {
                int status = connection.status();
                if (isRedirect(status)) {
                    if (redirectCount >= properties.getMaxRedirects()) {
                        throw new CustomValidationException("远程地址重定向次数过多");
                    }
                    URI location = connection.location();
                    if (location == null) {
                        throw new CustomValidationException("远程地址重定向无效");
                    }
                    try {
                        current = current.resolve(location);
                    } catch (IllegalArgumentException exception) {
                        throw new CustomValidationException("远程地址重定向无效");
                    }
                    redirectCount++;
                    continue;
                }
                if (status < 200 || status >= 300) {
                    throw new CustomValidationException("远程文件获取失败");
                }

                String contentType = connection.header("Content-Type");
                if (contentType == null || contentType.isBlank() || !contentType.contains("/")) {
                    throw new CustomValidationException("远程文件类型无效");
                }
                rejectDeclaredOversize(connection.header("Content-Length"));
                byte[] bytes = readBounded(connection.body());
                return new RemoteFilePayload(current, contentType, filename(current), bytes);
            } catch (CustomValidationException exception) {
                throw exception;
            } catch (IOException | RuntimeException exception) {
                throw new CustomValidationException("远程文件获取失败");
            }
        }
    }

    private void rejectDeclaredOversize(String contentLength) {
        if (contentLength == null || contentLength.isBlank()) {
            return;
        }
        try {
            long length = Long.parseLong(contentLength.trim());
            if (length < 0) {
                throw new CustomValidationException("远程文件大小无效");
            }
            if (length > properties.getRemoteMaxBytes()) {
                throw new CustomValidationException("远程文件大小超过限制");
            }
        } catch (NumberFormatException exception) {
            throw new CustomValidationException("远程文件大小无效");
        }
    }

    private byte[] readBounded(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new CustomValidationException("远程文件内容为空");
        }
        long maxBytes = properties.getRemoteMaxBytes();
        if (maxBytes < 0 || maxBytes > Integer.MAX_VALUE) {
            throw new CustomValidationException("远程文件大小配置无效");
        }
        try (InputStream body = inputStream;
             ByteArrayOutputStream output = new ByteArrayOutputStream((int) Math.min(maxBytes, 8192))) {
            byte[] buffer = new byte[8192];
            long total = 0;
            int read;
            while ((read = body.read(buffer)) != -1) {
                total += read;
                if (total > maxBytes) {
                    throw new CustomValidationException("远程文件大小超过限制");
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }

    private static boolean isRedirect(int status) {
        return status == 301 || status == 302 || status == 303 || status == 307 || status == 308;
    }

    private static String filename(URI uri) {
        String path = uri.getPath();
        if (path == null || path.isBlank() || path.endsWith("/")) {
            return "download";
        }
        int slash = path.lastIndexOf('/');
        String filename = path.substring(slash + 1);
        return filename.isBlank() ? "download" : filename;
    }

    private static RemoteConnection openHttpConnection(URI uri, InetAddress[] resolvedAddresses,
                                                        int connectTimeoutMillis, int readTimeoutMillis)
            throws IOException {
        if (resolvedAddresses == null || resolvedAddresses.length == 0) {
            throw new UnknownHostException(uri.getHost());
        }
        String pinnedHost = RemoteAddressValidator.normalizeHost(uri.getHost());
        DnsResolver dnsResolver = new DnsResolver() {
            @Override
            public InetAddress[] resolve(String host) throws UnknownHostException {
                if (!pinnedHost.equals(RemoteAddressValidator.normalizeHost(host))) {
                    throw new UnknownHostException(host);
                }
                return resolvedAddresses.clone();
            }

            @Override
            public String resolveCanonicalHostname(String host) throws UnknownHostException {
                if (!pinnedHost.equals(RemoteAddressValidator.normalizeHost(host))) {
                    throw new UnknownHostException(host);
                }
                return pinnedHost;
            }
        };
        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDnsResolver(dnsResolver)
                .build();
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(timeout(connectTimeoutMillis))
                .setConnectTimeout(timeout(connectTimeoutMillis))
                .setResponseTimeout(timeout(readTimeoutMillis))
                .build();
        CloseableHttpClient client = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .disableRedirectHandling()
                .disableAutomaticRetries()
                .build();
        try {
            CloseableHttpResponse response = client.execute(new HttpGet(uri));
            return new HttpClientConnection(client, response);
        } catch (IOException | RuntimeException exception) {
            client.close();
            throw exception;
        }
    }

    private static Timeout timeout(int millis) {
        if (millis < 0) {
            throw new IllegalArgumentException("timeout must not be negative");
        }
        return Timeout.ofMilliseconds(millis);
    }

    private static final class HttpClientConnection implements RemoteConnection {
        private final CloseableHttpClient client;
        private final CloseableHttpResponse response;

        private HttpClientConnection(CloseableHttpClient client, CloseableHttpResponse response) {
            this.client = client;
            this.response = response;
        }

        @Override
        public int status() {
            return response.getCode();
        }

        @Override
        public String header(String name) {
            Header header = response.getFirstHeader(name);
            return header == null ? null : header.getValue();
        }

        @Override
        public URI location() {
            String value = header("Location");
            if (value == null || value.isBlank()) {
                return null;
            }
            try {
                return URI.create(value);
            } catch (IllegalArgumentException exception) {
                return null;
            }
        }

        @Override
        public InputStream body() throws IOException {
            HttpEntity entity = response.getEntity();
            return entity == null ? null : entity.getContent();
        }

        @Override
        public void close() throws IOException {
            try {
                response.close();
            } finally {
                client.close();
            }
        }
    }
}
