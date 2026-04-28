package com.example.multitenant.usage;

import java.time.Instant;

public record TenantUserUsageDto(
		Long tenantId,
		String userId,
		long usedBytes,
		long quotaBytes,
		double usagePercent,
		Instant lastCollectedAt) {}

