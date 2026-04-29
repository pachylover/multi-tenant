package com.example.multitenant.usage;

import com.corundumstudio.socketio.SocketIOServer;
import com.example.multitenant.nextcloud.NextcloudClient;
import com.example.multitenant.tenant.Tenant;
import com.example.multitenant.tenant.TenantRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsageService {
	private final TenantRepository tenantRepository;
	private final NextcloudClient nextcloudClient;
	private final UsageCache usageCache;
	private final UsageProperties usageProperties;
	private final SocketIOServer socketIOServer;

	public UsageService(
			TenantRepository tenantRepository,
			NextcloudClient nextcloudClient,
			UsageCache usageCache,
			UsageProperties usageProperties,
			SocketIOServer socketIOServer) {
		this.tenantRepository = tenantRepository;
		this.nextcloudClient = nextcloudClient;
		this.usageCache = usageCache;
		this.usageProperties = usageProperties;
		this.socketIOServer = socketIOServer;
	}

	@Transactional(readOnly = true)
	public List<TenantUserUsageDto> getUsage(long tenantId) {
		List<TenantUserUsageDto> cached = usageCache.getIfPresent(tenantId);
		if (cached != null) {
			return cached;
		}
		return refreshUsage(tenantId);
	}

	@Transactional(readOnly = true)
	public List<TenantUserUsageDto> refreshUsage(long tenantId) {
		Tenant tenant =
				tenantRepository
						.findById(tenantId)
						.orElseThrow(() -> new IllegalArgumentException("Unknown tenantId: " + tenantId));

		List<String> members = nextcloudClient.getGroupMembers(tenant.getNcGroupId());
		Instant now = Instant.now();
		List<TenantUserUsageDto> out = new ArrayList<>(members.size());

		for (String userId : members) {
			NextcloudClient.UserQuota quota = nextcloudClient.getUserQuota(userId);
			long quotaBytes = quota.quotaBytes() > 0 ? quota.quotaBytes() : usageProperties.defaultQuotaBytes();
			long usedBytes = Math.max(0, quota.usedBytes());
			double percent = quotaBytes <= 0 ? 0.0 : (usedBytes * 100.0) / quotaBytes;

			out.add(new TenantUserUsageDto(tenantId, userId, usedBytes, quotaBytes, percent, now));
		}

		usageCache.put(tenantId, out);

		// notify interested clients that tenant usage changed
		socketIOServer.getBroadcastOperations().sendEvent("tenantUsageUpdated", 
				new TenantUsageUpdatedEvent(tenantId, now.toEpochMilli()));
		return out;
	}

	public record TenantUsageUpdatedEvent(long tenantId, long atMillis) {}
}

