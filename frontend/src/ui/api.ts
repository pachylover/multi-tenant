export type TenantDto = {
  tenantId: number;
  name: string;
  ncGroupId: string;
};

export type TenantUserUsageDto = {
  tenantId: number;
  userId: string;
  usedBytes: number;
  quotaBytes: number;
  usagePercent: number;
  lastCollectedAt: string;
};

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

async function http<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: {
      'content-type': 'application/json',
      ...(init?.headers ?? {})
    }
  });
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
  return (await res.json()) as T;
}

export function listTenants() {
  return http<TenantDto[]>('/api/tenants');
}

export function getTenantUsage(tenantId: number) {
  return http<TenantUserUsageDto[]>(`/api/tenants/${tenantId}/usage`);
}

