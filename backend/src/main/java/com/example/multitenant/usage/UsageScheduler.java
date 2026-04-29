package com.example.multitenant.usage;

import com.example.multitenant.tenant.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * DISABLED: Automatic polling removed in favor of webhook-based updates.
 * Uncomment @Component to re-enable periodic polling.
 */
// @Component
public class UsageScheduler {
	private static final Logger log = LoggerFactory.getLogger(UsageScheduler.class);

	private final TenantRepository tenantRepository;
	private final UsageService usageService;

	public UsageScheduler(TenantRepository tenantRepository, UsageService usageService) {
		this.tenantRepository = tenantRepository;
		this.usageService = usageService;
	}

	@Scheduled(fixedDelayString = "${usage.refresh.fixed-delay-ms:30000}")
	public void refreshAllTenants() {
		tenantRepository
				.findAll()
				.forEach(
						t -> {
							try {
								usageService.refreshUsage(t.getTenantId());
							} catch (Exception e) {
								// keep the scheduler alive even if Nextcloud is down / auth wrong
								log.warn("Failed to refresh usage for tenantId={}: {}", t.getTenantId(), e.getMessage());
							}
						});
	}
}

