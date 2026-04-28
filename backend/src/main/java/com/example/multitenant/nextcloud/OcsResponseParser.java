package com.example.multitenant.nextcloud;

import com.fasterxml.jackson.databind.JsonNode;

public final class OcsResponseParser {
	private OcsResponseParser() {}

	public static JsonNode requireOcsData(JsonNode root) {
		if (root == null) {
			throw new IllegalStateException("Empty Nextcloud response");
		}
		JsonNode ocs = root.get("ocs");
		if (ocs == null) {
			throw new IllegalStateException("Missing 'ocs' wrapper in Nextcloud response");
		}
		JsonNode meta = ocs.get("meta");
		if (meta != null) {
			int statusCode = meta.path("statuscode").asInt(200);
			if (statusCode < 200 || statusCode >= 300) {
				String message = meta.path("message").asText("Nextcloud error");
				throw new IllegalStateException("Nextcloud OCS error " + statusCode + ": " + message);
			}
		}
		JsonNode data = ocs.get("data");
		if (data == null) {
			throw new IllegalStateException("Missing 'ocs.data' in Nextcloud response");
		}
		return data;
	}
}

