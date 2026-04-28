INSERT INTO tenants(name, nc_group_id)
VALUES
  ('tenant-a', 'tenant-a'),
  ('tenant-b', 'tenant-b')
ON CONFLICT DO NOTHING;

-- Optional seed users; Nextcloud may not have them yet.
-- Backend primarily discovers group members via Nextcloud API.

