package com.example.multitenant.nextcloud;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class NextcloudClient {
	private final WebClient webClient;

	public NextcloudClient(NextcloudProperties props, WebClient.Builder builder) {
		this.webClient =
				builder.baseUrl(props.baseUrl())
						.defaultHeader("OCS-APIRequest", "true")
						.defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
						.defaultHeader(HttpHeaders.AUTHORIZATION, basicAuth(props.username(), props.password()))
						.build();
	}

	/**
	 * OCS provisioning API: Get group members.
	 * Endpoint: /ocs/v2.php/cloud/groups/{groupId}?format=json
	 * Response shape (simplified): ocs.data.users: [ "user1", ... ]
	 */
	public List<String> getGroupMembers(String groupId) {
		JsonNode root =
				webClient
						.get()
						.uri(
								uriBuilder ->
										uriBuilder
												.path("/ocs/v2.php/cloud/groups/{groupId}")
												.queryParam("format", "json")
												.build(groupId))
						.retrieve()
						.bodyToMono(JsonNode.class)
						.block();

		JsonNode data = OcsResponseParser.requireOcsData(root);
		JsonNode users = data.path("users");
		List<String> out = new ArrayList<>();
		if (users.isArray()) {
			for (JsonNode u : users) {
				out.add(u.asText());
			}
		}
		return out;
	}

	/**
	 * OCS provisioning API: Get user details including quota.
	 * Endpoint: /ocs/v2.php/cloud/users/{userId}?format=json
	 * Response: ocs.data.quota.used, ocs.data.quota.quota (bytes)
	 */
	public UserQuota getUserQuota(String userId) {
		JsonNode root =
				webClient
						.get()
						.uri(
								uriBuilder ->
										uriBuilder
												.path("/ocs/v2.php/cloud/users/{userId}")
												.queryParam("format", "json")
												.build(userId))
						.retrieve()
						.bodyToMono(JsonNode.class)
						.block();

		JsonNode data = OcsResponseParser.requireOcsData(root);
		JsonNode quota = data.path("quota");
		long used = quota.path("used").asLong(0);
		long quotaBytes = quota.path("quota").asLong(0);
		return new UserQuota(used, quotaBytes);
	}

	private static String basicAuth(String username, String password) {
		String token = username + ":" + password;
		return "Basic " + Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
	}

	public record UserQuota(long usedBytes, long quotaBytes) {}
}

