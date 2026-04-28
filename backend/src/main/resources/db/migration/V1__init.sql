CREATE TABLE IF NOT EXISTS tenants (
  tenant_id BIGSERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  nc_group_id VARCHAR(255) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS users (
  user_id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL REFERENCES tenants(tenant_id) ON DELETE CASCADE,
  email VARCHAR(255) NOT NULL,
  nc_user_id VARCHAR(255) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (tenant_id, nc_user_id)
);

CREATE INDEX IF NOT EXISTS idx_users_tenant_id ON users(tenant_id);

