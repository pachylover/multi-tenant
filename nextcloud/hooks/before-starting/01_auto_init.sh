#!/usr/bin/env bash
set -euo pipefail

echo "[auto-init] starting nextcloud auto init"

: "${POSTGRES_HOST:=db}"
: "${POSTGRES_DB:=nextcloud}"
: "${POSTGRES_USER:=app}"
: "${POSTGRES_PASSWORD:=app}"

: "${NEXTCLOUD_ADMIN_USER:=admin}"
: "${NEXTCLOUD_ADMIN_PASSWORD:=adminpass}"

: "${NC_AUTO_INIT:=true}"
: "${NC_AUTO_INIT_PASSWORD:=S3curePass_2026!}"
: "${NC_AUTO_INIT_QUOTA:=100 MB}"

if [ "${NC_AUTO_INIT}" != "true" ]; then
  echo "[auto-init] NC_AUTO_INIT!=true, skipping"
  exit 0
fi

cd /var/www/html

wait_for_tcp() {
  local host="$1"
  local port="$2"
  local i
  for i in $(seq 1 60); do
    if (echo >/dev/tcp/"$host"/"$port") >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done
  return 1
}

echo "[auto-init] waiting for postgres ${POSTGRES_HOST}:5432"
wait_for_tcp "${POSTGRES_HOST}" 5432 || {
  echo "[auto-init] postgres not reachable"
  exit 1
}

run_occ() {
  php occ "$@"
}

is_installed() {
  if [ -f /var/www/html/config/config.php ]; then
    # occ returns non-zero before install, so use status.php check by reading config presence only.
    return 0
  fi
  return 1
}

if ! is_installed; then
  echo "[auto-init] installing nextcloud..."
  run_occ maintenance:install \
    --database "pgsql" \
    --database-host "${POSTGRES_HOST}" \
    --database-name "${POSTGRES_DB}" \
    --database-user "${POSTGRES_USER}" \
    --database-pass "${POSTGRES_PASSWORD}" \
    --admin-user "${NEXTCLOUD_ADMIN_USER}" \
    --admin-pass "${NEXTCLOUD_ADMIN_PASSWORD}"
else
  echo "[auto-init] nextcloud already installed"
fi

echo "[auto-init] ensuring trusted domains include 'localhost' and 'nextcloud'"
run_occ config:system:set trusted_domains 0 --value=localhost >/dev/null 2>&1 || true
run_occ config:system:set trusted_domains 1 --value=nextcloud >/dev/null 2>&1 || true

echo "[auto-init] ensuring groups tenant-a / tenant-b"
run_occ group:add tenant-a >/dev/null 2>&1 || true
run_occ group:add tenant-b >/dev/null 2>&1 || true

create_user_if_missing() {
  local user="$1"
  local display="$2"
  if run_occ user:info "$user" >/dev/null 2>&1; then
    return 0
  fi
  OC_PASS="${NC_AUTO_INIT_PASSWORD}" run_occ user:add --password-from-env --display-name "${display}" "${user}"
}

echo "[auto-init] ensuring users"
create_user_if_missing tenant-a-u1 "A1"
create_user_if_missing tenant-a-u2 "A2"
create_user_if_missing tenant-a-u3 "A3"
create_user_if_missing tenant-b-u1 "B1"
create_user_if_missing tenant-b-u2 "B2"
create_user_if_missing tenant-b-u3 "B3"

echo "[auto-init] ensuring group membership"
run_occ group:adduser tenant-a tenant-a-u1 >/dev/null 2>&1 || true
run_occ group:adduser tenant-a tenant-a-u2 >/dev/null 2>&1 || true
run_occ group:adduser tenant-a tenant-a-u3 >/dev/null 2>&1 || true
run_occ group:adduser tenant-b tenant-b-u1 >/dev/null 2>&1 || true
run_occ group:adduser tenant-b tenant-b-u2 >/dev/null 2>&1 || true
run_occ group:adduser tenant-b tenant-b-u3 >/dev/null 2>&1 || true

echo "[auto-init] setting quota (${NC_AUTO_INIT_QUOTA})"
run_occ user:setting tenant-a-u1 files quota "${NC_AUTO_INIT_QUOTA}" >/dev/null 2>&1 || true
run_occ user:setting tenant-a-u2 files quota "${NC_AUTO_INIT_QUOTA}" >/dev/null 2>&1 || true
run_occ user:setting tenant-a-u3 files quota "${NC_AUTO_INIT_QUOTA}" >/dev/null 2>&1 || true
run_occ user:setting tenant-b-u1 files quota "${NC_AUTO_INIT_QUOTA}" >/dev/null 2>&1 || true
run_occ user:setting tenant-b-u2 files quota "${NC_AUTO_INIT_QUOTA}" >/dev/null 2>&1 || true
run_occ user:setting tenant-b-u3 files quota "${NC_AUTO_INIT_QUOTA}" >/dev/null 2>&1 || true

echo "[auto-init] done"

