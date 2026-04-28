package com.example.multitenant.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "tenants")
public class Tenant {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "tenant_id")
	private Long tenantId;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "nc_group_id", nullable = false)
	private String ncGroupId;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	public Long getTenantId() {
		return tenantId;
	}

	public String getName() {
		return name;
	}

	public String getNcGroupId() {
		return ncGroupId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}

