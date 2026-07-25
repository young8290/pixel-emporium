package com.example.business.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix="notification.mail") public record MailNotificationProperties(String baseUrl,String hmacSecret,long connectTimeoutMs,long readTimeoutMs,long dispatchIntervalMs,int batchSize) {}
