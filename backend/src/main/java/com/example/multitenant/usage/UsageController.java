package com.example.multitenant.usage;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants/{tenantId}/usage")
public class UsageController {
	private final UsageService usageService;

	public UsageController(UsageService usageService) {
		this.usageService = usageService;
	}

	@GetMapping
	public List<TenantUserUsageDto> list(@PathVariable long tenantId) {
		return usageService.getUsage(tenantId);
	}

	@PostMapping("/refresh")
	public List<TenantUserUsageDto> refresh(@PathVariable long tenantId) {
		return usageService.refreshUsage(tenantId);
	}

	@GetMapping("/health")
	public Map<String, Object> health(@PathVariable long tenantId) {
		return Map.of("tenantId", tenantId, "cached", usageService.getUsage(tenantId).size());
	}
}

