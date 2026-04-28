package com.example.multitenant.usage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "usage")
public record UsageProperties(long defaultQuotaBytes) {}

