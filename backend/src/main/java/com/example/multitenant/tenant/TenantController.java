package com.example.multitenant.tenant;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {
	private final TenantRepository tenantRepository;

	public TenantController(TenantRepository tenantRepository) {
		this.tenantRepository = tenantRepository;
	}

	@GetMapping
	public List<TenantDto> listTenants() {
		return tenantRepository.findAll().stream().map(TenantDto::from).toList();
	}
}

