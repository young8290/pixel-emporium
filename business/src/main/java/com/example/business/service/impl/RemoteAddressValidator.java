package com.example.business.service.impl;

import com.example.common.exception.CustomValidationException;

import java.net.IDN;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.URI;
import java.util.List;
import java.util.Locale;

final class RemoteAddressValidator {
    private final HttpRemoteFileFetcher.HostResolver resolver;
    private final List<String> allowedHosts;

    RemoteAddressValidator(HttpRemoteFileFetcher.HostResolver resolver, List<String> allowedHosts) {
        this.resolver = resolver;
        this.allowedHosts = allowedHosts == null ? List.of() : allowedHosts.stream()
                .map(RemoteAddressValidator::normalizeRule)
                .filter(rule -> !rule.isBlank())
                .toList();
    }

    ResolvedTarget validate(URI uri) {
        if (uri == null || !uri.isAbsolute() || uri.getUserInfo() != null) {
            throw new CustomValidationException("远程地址无效");
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new CustomValidationException("仅允许 HTTP 或 HTTPS 远程地址");
        }

        String host = normalizeHost(uri.getHost());
        if (host.isBlank() || host.equals("localhost") || host.endsWith(".localhost")) {
            throw new CustomValidationException("远程地址不允许访问本机");
        }
        if (!allowedHosts.isEmpty() && allowedHosts.stream().noneMatch(rule -> host.equals(rule) || host.endsWith("." + rule))) {
            throw new CustomValidationException("远程地址不在允许范围内");
        }

        InetAddress[] addresses;
        try {
            addresses = resolver.resolve(host);
        } catch (UnknownHostException exception) {
            throw new CustomValidationException("远程地址无法解析");
        }
        if (addresses == null || addresses.length == 0) {
            throw new CustomValidationException("远程地址无法解析");
        }
        for (InetAddress address : addresses) {
            if (address == null || isBlocked(address)) {
                throw new CustomValidationException("远程地址不允许访问内网或本地资源");
            }
        }
        return new ResolvedTarget(host, addresses);
    }

    record ResolvedTarget(String host, InetAddress[] addresses) {
        ResolvedTarget {
            addresses = addresses.clone();
        }

        @Override
        public InetAddress[] addresses() {
            return addresses.clone();
        }
    }

    static String normalizeHost(String host) {
        if (host == null || host.isBlank()) {
            return "";
        }
        try {
            String normalized = host.strip();
            if (normalized.startsWith("[") && normalized.endsWith("]")) {
                normalized = normalized.substring(1, normalized.length() - 1);
            }
            return IDN.toASCII(normalized).toLowerCase(Locale.ROOT).replaceAll("\\.$", "");
        } catch (IllegalArgumentException exception) {
            throw new CustomValidationException("远程地址无效");
        }
    }

    private static String normalizeRule(String rule) {
        if (rule == null) {
            return "";
        }
        return normalizeHost(rule.trim().replaceFirst("^\\.", ""));
    }

    private static boolean isBlocked(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) {
            return true;
        }
        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            return isBlockedIpv4(bytes[0] & 0xff, bytes[1] & 0xff, bytes[2] & 0xff, bytes[3] & 0xff);
        }
        if (bytes.length == 16) {
            if (isIpv4Mapped(bytes)) {
                return isBlockedIpv4(bytes[12] & 0xff, bytes[13] & 0xff, bytes[14] & 0xff, bytes[15] & 0xff);
            }
            if (isIpv4Compatible(bytes)) {
                return isBlockedIpv4(bytes[12] & 0xff, bytes[13] & 0xff, bytes[14] & 0xff, bytes[15] & 0xff);
            }
            if (isNat64(bytes)) {
                return isBlockedIpv4(bytes[12] & 0xff, bytes[13] & 0xff, bytes[14] & 0xff, bytes[15] & 0xff);
            }
            if ((bytes[0] & 0xff) == 0x20 && (bytes[1] & 0xff) == 0x02) {
                return isBlockedIpv4(bytes[2] & 0xff, bytes[3] & 0xff, bytes[4] & 0xff, bytes[5] & 0xff);
            }
            return (bytes[0] & 0xfe) == 0xfc;
        }
        return false;
    }

    private static boolean isIpv4Mapped(byte[] bytes) {
        for (int i = 0; i < 10; i++) {
            if (bytes[i] != 0) {
                return false;
            }
        }
        return bytes[10] == (byte) 0xff && bytes[11] == (byte) 0xff;
    }

    private static boolean isIpv4Compatible(byte[] bytes) {
        for (int i = 0; i < 12; i++) {
            if (bytes[i] != 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean isNat64(byte[] bytes) {
        return (bytes[0] & 0xff) == 0x00
                && (bytes[1] & 0xff) == 0x64
                && (bytes[2] & 0xff) == 0xff
                && (bytes[3] & 0xff) == 0x9b;
    }

    private static boolean isBlockedIpv4(int first, int second, int third, int fourth) {
        return first == 10
                || (first == 172 && second >= 16 && second <= 31)
                || (first == 192 && second == 168)
                || (first == 127)
                || (first == 169 && second == 254)
                || (first == 100 && second >= 64 && second <= 127)
                || (first == 100 && second == 100 && third == 100 && fourth == 200)
                || first == 0;
    }
}
