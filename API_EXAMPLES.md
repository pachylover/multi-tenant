# API 응답 예시

Multi-Tenant Nextcloud Storage Monitor의 모든 API 엔드포인트에 대한 요청 및 응답 예시입니다.

## 목차

- [Tenant API](#tenant-api)
- [Usage API](#usage-api)
- [Webhook API](#webhook-api)
- [Socket.IO 이벤트](#socketio-이벤트)
- [에러 응답](#에러-응답)

---

## Tenant API

### GET /api/tenants

Tenant 목록을 조회합니다.

#### 요청

```bash
curl http://localhost:8080/api/tenants
```

#### 성공 응답 (200 OK)

```json
[
  {
    "tenantId": 1,
    "name": "tenant-a",
    "ncGroupId": "tenant-a"
  },
  {
    "tenantId": 2,
    "name": "tenant-b",
    "ncGroupId": "tenant-b"
  }
]
```

#### 응답 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| `tenantId` | Long | Tenant 고유 ID |
| `name` | String | Tenant 이름 |
| `ncGroupId` | String | Nextcloud 그룹 ID |

---

## Usage API

### GET /api/tenants/{tenantId}/usage

특정 Tenant의 사용량을 조회합니다 (캐시된 데이터 반환).

#### 요청

```bash
curl http://localhost:8080/api/tenants/1/usage
```

#### 성공 응답 (200 OK)

```json
[
  {
    "tenantId": 1,
    "userId": "tenant-a-u1",
    "usedBytes": 104844844,
    "quotaBytes": 104857600,
    "usagePercent": 99.98783493041992,
    "lastCollectedAt": "2026-04-29T10:00:15.123Z"
  },
  {
    "tenantId": 1,
    "userId": "tenant-a-u2",
    "usedBytes": 30211461,
    "quotaBytes": 104857600,
    "usagePercent": 28.811894416809082,
    "lastCollectedAt": "2026-04-29T10:00:15.123Z"
  },
  {
    "tenantId": 1,
    "userId": "tenant-a-u3",
    "usedBytes": 104857600,
    "quotaBytes": 104857600,
    "usagePercent": 100.0,
    "lastCollectedAt": "2026-04-29T10:00:15.123Z"
  }
]
```

#### 응답 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| `tenantId` | Long | Tenant ID |
| `userId` | String | Nextcloud 사용자 ID |
| `usedBytes` | Long | 사용 중인 용량 (바이트) |
| `quotaBytes` | Long | 할당된 Quota (바이트) |
| `usagePercent` | Double | 사용률 (%) |
| `lastCollectedAt` | String (ISO 8601) | 마지막 수집 시각 |

#### 실패 응답 - Tenant 없음 (500 Internal Server Error)

```bash
curl http://localhost:8080/api/tenants/999/usage
```

```json
{
  "error": "Failed to fetch usage data: Unknown tenantId: 999"
}
```

#### 실패 응답 - Nextcloud 연결 불가 (503 Service Unavailable)

Nextcloud 서비스가 중단된 경우:

```json
{
  "error": "Cannot connect to Nextcloud. Service may be unavailable."
}
```

#### 실패 응답 - Nextcloud 인증 실패 (502 Bad Gateway)

Nextcloud 자격증명이 잘못된 경우:

```json
{
  "error": "Nextcloud authentication or authorization failed."
}
```

---

### POST /api/tenants/{tenantId}/usage/refresh

특정 Tenant의 사용량을 강제로 갱신합니다 (Nextcloud API 호출).

#### 요청

```bash
curl -X POST http://localhost:8080/api/tenants/1/usage/refresh
```

#### 성공 응답 (200 OK)

```json
[
  {
    "tenantId": 1,
    "userId": "tenant-a-u1",
    "usedBytes": 104850000,
    "quotaBytes": 104857600,
    "usagePercent": 99.99272155761719,
    "lastCollectedAt": "2026-04-29T10:05:30.456Z"
  },
  {
    "tenantId": 1,
    "userId": "tenant-a-u2",
    "usedBytes": 30211461,
    "quotaBytes": 104857600,
    "usagePercent": 28.811894416809082,
    "lastCollectedAt": "2026-04-29T10:05:30.456Z"
  }
]
```

**참고**: 갱신 후 Socket.IO를 통해 `tenantUsageUpdated` 이벤트가 자동으로 브로드캐스트됩니다.

#### Socket.IO 이벤트

```javascript
// Frontend에서 수신되는 이벤트
{
  "tenantId": 1,
  "atMillis": 1714388730456
}
```

#### 실패 응답 - Nextcloud 그룹 없음 (500 Internal Server Error)

```json
{
  "error": "Failed to fetch usage data: Error fetching group members for tenant-x: Failed to get group members..."
}
```

---

### GET /api/tenants/{tenantId}/usage/health

캐시 상태를 확인합니다.

#### 요청

```bash
curl http://localhost:8080/api/tenants/1/usage/health
```

#### 성공 응답 (200 OK)

```json
{
  "tenantId": 1,
  "cached": 3
}
```

#### 응답 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| `tenantId` | Long | Tenant ID |
| `cached` | Integer | 캐시된 사용자 수 |

---

## Webhook API

### POST /api/webhooks/nextcloud

Nextcloud에서 파일 변경 시 호출되는 엔드포인트입니다.

#### 요청

```bash
curl -X POST http://localhost:8080/api/webhooks/nextcloud \
  -H "Content-Type: application/json" \
  -H "X-Nextcloud-User: tenant-a-u1" \
  -d '{
    "event": "file.created",
    "file": "/Documents/test.txt"
  }'
```

#### 요청 헤더

| 헤더 | 필수 | 설명 |
|------|------|------|
| `Content-Type` | Yes | `application/json` |
| `X-Nextcloud-User` | No | 파일을 수정한 사용자 ID |

#### 요청 바디 (선택사항)

| 필드 | 타입 | 설명 |
|------|------|------|
| `event` | String | 이벤트 타입 (file.created, file.updated 등) |
| `file` | String | 파일 경로 |

#### 성공 응답 (200 OK)

```json
{
  "status": "success",
  "user": "tenant-a-u1"
}
```

#### 응답 - 사용자 없음 (200 OK)

```bash
curl -X POST http://localhost:8080/api/webhooks/nextcloud \
  -H "Content-Type: application/json" \
  -d '{"event": "test"}'
```

```json
{
  "status": "skipped",
  "reason": "no user"
}
```

#### 응답 - 갱신 실패 (200 OK)

모든 Tenant 갱신이 실패한 경우:

```json
{
  "status": "no_tenants_refreshed",
  "user": "tenant-a-u1"
}
```

#### 응답 - 에러 발생 (200 OK)

처리 중 예외 발생:

```json
{
  "status": "error",
  "message": "Error message details"
}
```

**참고**: Webhook은 항상 200 OK를 반환하여 Nextcloud Flow가 실패로 간주하지 않도록 합니다.

---

### GET /api/webhooks/nextcloud/health

Webhook 엔드포인트 상태를 확인합니다.

#### 요청

```bash
curl http://localhost:8080/api/webhooks/nextcloud/health
```

#### 성공 응답 (200 OK)

```json
{
  "status": "ok",
  "endpoint": "/api/webhooks/nextcloud"
}
```

---

## Socket.IO 이벤트

### tenantUsageUpdated

Tenant 사용량이 갱신되면 자동으로 브로드캐스트됩니다.

#### 이벤트 데이터

```json
{
  "tenantId": 1,
  "atMillis": 1714388730456
}
```

#### Frontend 수신 예시

```typescript
socket.on('tenantUsageUpdated', (data: { tenantId: number; atMillis: number }) => {
  console.log(`Tenant ${data.tenantId} 사용량 업데이트됨`);
  // 자동으로 API 재호출하여 최신 데이터 표시
});
```

---

## 에러 응답

### 공통 에러 형식

모든 에러는 다음 형식으로 반환됩니다:

```json
{
  "error": "Error message"
}
```

### HTTP 상태 코드

| 코드 | 설명 | 예시 |
|------|------|------|
| `400 Bad Request` | 잘못된 요청 | 필수 파라미터 누락 |
| `404 Not Found` | 리소스 없음 | 존재하지 않는 엔드포인트 |
| `500 Internal Server Error` | 서버 내부 오류 | 일반적인 서버 에러 |
| `502 Bad Gateway` | Nextcloud 인증/권한 실패 | 잘못된 자격증명 |
| `503 Service Unavailable` | Nextcloud 연결 불가 | Nextcloud 서비스 중단 |

### 에러 시나리오별 응답

#### 1. Nextcloud 서비스 중단

**요청:**
```bash
# Nextcloud 중단
docker compose stop nextcloud

# API 호출
curl http://localhost:8080/api/tenants/1/usage/refresh
```

**응답: 503 Service Unavailable**
```json
{
  "error": "Cannot connect to Nextcloud. Service may be unavailable."
}
```

---

#### 2. Nextcloud 인증 실패

**요청:**
```bash
# 잘못된 비밀번호로 Backend 재시작
# .env: NEXTCLOUD_PASSWORD=wrong_password
docker compose restart backend

# API 호출
curl http://localhost:8080/api/tenants/1/usage/refresh
```

**응답: 502 Bad Gateway**
```json
{
  "error": "Nextcloud authentication or authorization failed."
}
```

---

#### 3. 존재하지 않는 Tenant

**요청:**
```bash
curl http://localhost:8080/api/tenants/9999/usage
```

**응답: 500 Internal Server Error**
```json
{
  "error": "Failed to fetch usage data: Unknown tenantId: 9999"
}
```

---

#### 4. 존재하지 않는 그룹

Tenant의 Nextcloud 그룹이 삭제된 경우:

**응답: 500 Internal Server Error**
```json
{
  "error": "Failed to fetch usage data: Error fetching group members for deleted-group: Failed to get group members..."
}
```

---

#### 5. 민감정보 필터링

에러 메시지에서 민감정보가 자동으로 제거됩니다:

**원본 에러:**
```
Failed to connect to http://admin:adminpass@nextcloud:80/api
Authorization: Bearer abc123def456
```

**반환 메시지:**
```json
{
  "error": "Failed to fetch usage data: Failed to connect to [URL]/api [AUTH]"
}
```

**필터링 규칙:**
- URL 자격증명: `http://user:pass@host` → `[URL]`
- Authorization 헤더: `Authorization: Bearer token` → `[AUTH]`
- 200자 초과 시 truncate

---

## Socket.IO 이벤트

### 이벤트: tenantUsageUpdated

사용량이 갱신될 때마다 브로드캐스트됩니다.

#### 이벤트 데이터

```javascript
{
  "tenantId": 1,
  "atMillis": 1714388730456  // Unix timestamp in milliseconds
}
```

#### Frontend 구독 예시

```typescript
import { io } from 'socket.io-client';

const socket = io('http://localhost:9092', {
  transports: ['polling', 'websocket'],
  reconnection: true
});

socket.on('tenantUsageUpdated', (data) => {
  console.log('Usage updated:', data);
  // data.tenantId가 현재 선택된 tenant면 UI 갱신
  if (data.tenantId === currentTenantId) {
    refreshUsageData();
  }
});
```

---

## 응답 시간 참고

| 엔드포인트 | 평균 응답 시간 |
|-----------|---------------|
| `GET /api/tenants` | ~10ms (DB 조회) |
| `GET /api/tenants/{id}/usage` | ~5ms (캐시 조회) |
| `POST /api/tenants/{id}/usage/refresh` | ~200-500ms (Nextcloud API 호출) |
| `POST /api/webhooks/nextcloud` | ~200-500ms (Nextcloud API 호출) |
| `GET /api/*/health` | ~1ms |

---

## 추가 정보

### 캐시 동작

- 사용량 데이터는 메모리에 캐시됩니다
- `GET /usage`는 캐시된 데이터를 즉시 반환합니다
- `POST /usage/refresh` 또는 Webhook 호출 시 캐시가 갱신됩니다
- 캐시 만료 시간 없음 (명시적 갱신만)

### Nextcloud API 제한

- Nextcloud OCS API는 요청 제한이 없습니다 (기본 설정)
- 대량 요청 시 Nextcloud 성능에 영향을 줄 수 있습니다
- 프로덕션에서는 Webhook 사용을 권장합니다 (폴링 비활성화됨)

### 보안 고려사항

- 모든 Nextcloud 자격증명은 환경변수로 관리됩니다
- API 응답에서 민감정보가 자동으로 필터링됩니다
- Webhook 엔드포인트는 인증이 없습니다 (내부 네트워크 전용)
- 프로덕션 환경에서는 Webhook 인증 추가를 고려하세요

---

## 관련 문서

- [README.md](README.md) - 프로젝트 개요 및 설정 가이드