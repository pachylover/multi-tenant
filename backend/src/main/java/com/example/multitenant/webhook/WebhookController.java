package com.example.multitenant.webhook;

import com.example.multitenant.tenant.Tenant;
import com.example.multitenant.tenant.TenantRepository;
import com.example.multitenant.usage.UsageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {
	private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

	private final TenantRepository tenantRepository;
	private final UsageService usageService;

	public WebhookController(TenantRepository tenantRepository, UsageService usageService) {
		this.tenantRepository = tenantRepository;
		this.usageService = usageService;
	}

	/**
	 * Nextcloud Workflow webhook endpoint
	 * Nextcloud Flow에서 파일 생성/수정/삭제 시 호출됨
	 */
	@PostMapping("/nextcloud")
	public ResponseEntity<Map<String, String>> handleNextcloudWebhook(
			@RequestHeader(value = "X-Nextcloud-User", required = false) String userId,
			@RequestBody(required = false) Map<String, Object> payload) {

		log.info("[WEBHOOK] Received Nextcloud webhook: user={}, payload={}", userId, payload);

		try {
			// userId로부터 tenant 찾기
			if (userId == null || userId.isEmpty()) {
				log.warn("[WEBHOOK] No user ID provided in webhook");
				return ResponseEntity.ok(Map.of("status", "skipped", "reason", "no user"));
			}

			// 모든 tenant를 확인하고 해당 사용자가 속한 tenant의 usage 갱신
			boolean refreshed = false;
			for (Tenant tenant : tenantRepository.findAll()) {
				try {
					// tenant usage를 갱신하면 자동으로 Socket.IO 이벤트가 발생함
					usageService.refreshUsage(tenant.getTenantId());
					refreshed = true;
					log.info("[WEBHOOK] Refreshed usage for tenantId={} (user={})", tenant.getTenantId(), userId);
				} catch (Exception e) {
					log.warn("[WEBHOOK] Failed to refresh tenantId={}: {}", tenant.getTenantId(), e.getMessage());
				}
			}

			if (refreshed) {
				return ResponseEntity.ok(Map.of("status", "success", "user", userId));
			} else {
				return ResponseEntity.ok(Map.of("status", "no_tenants_refreshed", "user", userId));
			}

		} catch (Exception e) {
			log.error("[WEBHOOK] Error processing webhook", e);
			return ResponseEntity.ok(Map.of("status", "error", "message", e.getMessage()));
		}
	}

	/**
	 * Webhook 상태 확인용 엔드포인트
	 */
	@GetMapping("/nextcloud/health")
	public ResponseEntity<Map<String, String>> health() {
		return ResponseEntity.ok(Map.of("status", "ok", "endpoint", "/api/webhooks/nextcloud"));
	}
}
