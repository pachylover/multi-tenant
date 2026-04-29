#!/usr/bin/env bash
# Nextcloud 파일 변경 모니터링 및 Webhook 호출 스크립트

set -euo pipefail

: "${WEBHOOK_URL:=http://backend:8080/api/webhooks/nextcloud}"
: "${MONITOR_PATH:=/var/www/html/data}"
: "${MONITOR_ENABLED:=false}"

if [ "${MONITOR_ENABLED}" != "true" ]; then
  echo "[file-monitor] MONITOR_ENABLED!=true, skipping file monitoring"
  exit 0
fi

echo "[file-monitor] Starting file change monitor for ${MONITOR_PATH}"
echo "[file-monitor] Webhook URL: ${WEBHOOK_URL}"

# inotify-tools 설치 확인
if ! command -v inotifywait >/dev/null 2>&1; then
  echo "[file-monitor] inotify-tools not installed, installing..."
  apt-get update -qq && apt-get install -y -qq inotify-tools
fi

# 파일 변경 감지 및 webhook 호출
inotifywait -m -r -e modify,create,delete,move "${MONITOR_PATH}" \
  --exclude '(\.ocTransferId|\.part|\.lock)' \
  --format '%w%f %e' | while read -r file event; do
  
  # 사용자 ID 추출 (경로에서 username 파싱)
  user_id=$(echo "$file" | grep -oP '(?<=data/)[^/]+' || echo "unknown")
  
  echo "[file-monitor] File change detected: user=${user_id}, event=${event}"
  
  # Webhook 호출 (비동기)
  curl -X POST "${WEBHOOK_URL}" \
    -H "Content-Type: application/json" \
    -H "X-Nextcloud-User: ${user_id}" \
    -d "{\"event\":\"${event}\",\"file\":\"${file}\"}" \
    --max-time 5 \
    --silent \
    --fail \
    >/dev/null 2>&1 || echo "[file-monitor] Webhook call failed"
  
done
