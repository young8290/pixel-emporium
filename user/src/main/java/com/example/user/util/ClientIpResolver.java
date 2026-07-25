package com.example.user.util;

import com.example.user.config.MailCenterProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import java.net.InetAddress;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ClientIpResolver {
    private final MailCenterProperties properties;
    public ClientIpResolver(MailCenterProperties properties) { this.properties = properties; }
    public String resolve(HttpServletRequest request) {
        String remote = literal(request.getRemoteAddr()); if (remote == null) return "unknown";
        Set<String> trusted = properties.verification() == null || properties.verification().trustedProxyAddresses() == null ? Set.of("127.0.0.1", "::1") : properties.verification().trustedProxyAddresses().stream().map(this::literal).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        if (!trusted.contains(remote)) return remote;
        String forwarded = request.getHeader("X-Forwarded-For"); if (forwarded == null) return remote;
        String first = forwarded.split(",", 2)[0].trim(); String candidate = literal(first); return candidate == null ? remote : candidate;
    }
    private String literal(String value) { try { if (value == null || value.isBlank() || !value.matches("^[0-9a-fA-F:.%]+$")) return null; return InetAddress.getByName(value).getHostAddress(); } catch (Exception e) { return null; } }
}
