package com.example.multitenant.usage;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants/{tenantId}/usage")
public class UsageController {
	private static final Logger log = LoggerFactory.getLogger(UsageController.class);
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

	/**
	 * Handle Nextcloud API errors and return 5xx status codes with safe error messages.
	 * All Nextcloud failures are treated as backend/upstream service issues.
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, String>> handleError(Exception e) {
		log.error("Error in usage controller", e);
		
		String message = e.getMessage();
		if (message == null) {
			message = "Unknown error occurred";
		}
		
		// Sanitize message: remove sensitive information
		String safeMessage = sanitizeErrorMessage(message);
		
		// Detect authentication/authorization failures with upstream service
		if (message.contains("401") || message.contains("Unauthorized") ||
				message.contains("403") || message.contains("Forbidden")) {
			return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
				.body(Map.of("error", "Nextcloud authentication or authorization failed."));
		}
		
		// Connection errors
		if (message.contains("Connection refused") || message.contains("timeout") ||
				message.contains("ConnectException")) {
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(Map.of("error", "Cannot connect to Nextcloud. Service may be unavailable."));
		}
		
		// Generic upstream error
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(Map.of("error", "Failed to fetch usage data: " + safeMessage));
	}
	
	/**
	 * Remove sensitive information from error messages
	 */
	private String sanitizeErrorMessage(String message) {
		if (message == null) return "Unknown error";
		
		// Remove URLs with credentials
		message = message.replaceAll("https?://[^:]+:[^@]+@[^\\s]+", "[URL]");
		
		// Remove authorization headers
		message = message.replaceAll("(?i)authorization[:\\s]+[^\\s,]+", "[AUTH]");
		
		// Truncate if too long
		if (message.length() > 200) {
			message = message.substring(0, 200) + "...";
		}
		
		return message;
	}
}

