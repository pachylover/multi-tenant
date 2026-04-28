package com.example.multitenant.usage;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class UsageCache {
	private final Cache<Long, List<TenantUserUsageDto>> cache =
			Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(30)).maximumSize(10_000).build();

	public List<TenantUserUsageDto> getIfPresent(long tenantId) {
		return cache.getIfPresent(tenantId);
	}

	public void put(long tenantId, List<TenantUserUsageDto> items) {
		cache.put(tenantId, items);
	}
}

