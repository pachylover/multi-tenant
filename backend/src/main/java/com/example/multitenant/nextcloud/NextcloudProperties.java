package com.example.multitenant.nextcloud;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nextcloud")
public record NextcloudProperties(String baseUrl, String username, String password) {}

