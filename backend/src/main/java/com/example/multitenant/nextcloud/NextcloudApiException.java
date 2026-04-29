package com.example.multitenant.nextcloud;

/**
 * Custom exception for Nextcloud API errors.
 * Provides clear error messages for authentication, permission, and connection failures.
 */
public class NextcloudApiException extends RuntimeException {
	public NextcloudApiException(String message) {
		super(message);
	}

	public NextcloudApiException(String message, Throwable cause) {
		super(message, cause);
	}
}
