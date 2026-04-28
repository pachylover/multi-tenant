package com.example.multitenant.tenant;

public record TenantDto(Long tenantId, String name, String ncGroupId) {
	public static TenantDto from(Tenant t) {
		return new TenantDto(t.getTenantId(), t.getName(), t.getNcGroupId());
	}
}

