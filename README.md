# Multi-Tenant Nextcloud Storage Monitor

실시간으로 Nextcloud 사용자들의 스토리지 사용량을 모니터링하는 멀티 테넌트 시스템입니다.

> **📚 API 문서:** [API_EXAMPLES.md](API_EXAMPLES.md) - 전체 API 응답 예시

## 📋 목차

- [빠른 시작](#-빠른-시작)
- [시스템 아키텍처](#-시스템-아키텍처)
- [주요 기능](#-주요-기능)
- [Webhook 설정](#-webhook-설정)
- [문제 해결](#-문제-해결)

## 🚀 빠른 시작

### 사전 요구사항

- Docker Desktop (Windows/Mac) 또는 Docker Engine + Docker Compose (Linux)
- PowerShell 5.1 이상 (Windows)

### 실행 방법

#### Windows (PowerShell)

```powershell
.\start.ps1
```

#### Linux/Mac

```bash
chmod +x start.sh
./start.sh
```

**첫 실행 시 2-3분 소요됩니다** (이미지 빌드 + Nextcloud 초기화)

스크립트가 자동으로:
1. `.env` 파일 생성 (없는 경우)
2. Docker Compose 실행
3. 접속 정보 출력

---

## 🏗️ 시스템 아키텍처

```
┌─────────────────┐
│   Frontend      │  React + Vite + Socket.IO Client
│  (Port 5173)    │  - Admin UI (/admin/storage)
└────────┬────────┘  - 실시간 사용량 모니터링
         │
         ├─────────────────────────┐
         │ HTTP API                │ Socket.IO
         ↓                         ↓
┌─────────────────────────────────────┐
│         Backend                      │  Spring Boot + netty-socketio
│       (Port 8080 + 9092)             │  - REST API
│                                      │  - 실시간 이벤트 브로드캐스트
└────────┬─────────────────────┬──────┘  - Webhook 수신
         │ OCS API             │ SQL
         ↓                     ↓
┌─────────────────┐    ┌──────────────┐
│   Nextcloud     │    │  PostgreSQL  │
│   (Port 8081)   │    │  (Port 5432) │
│                 │    │              │
│ - 파일 저장      │    │ - Tenant 정보 │
│ - 사용자/그룹    │     │              │
│ - Quota 관리    │     │              │
└─────────────────┘    └──────────────┘
```

### 기술 스택

- **Frontend**: React 18, TypeScript, Vite, Socket.IO Client 4.x
- **Backend**: Spring Boot 3.3.2, Java 21, netty-socketio 2.0.12
- **Database**: PostgreSQL 16 + Flyway
- **Storage**: Nextcloud 28 (Apache)
- **Container**: Docker Compose

## ✨ 주요 기능

### 1. 사용량 모니터링
- 새로고침 버튼으로 최신 데이터 조회
- Nextcloud API를 통한 실시간 용량 확인

### 2. 멀티 테넌트 지원
- Tenant별 그룹 관리
- Tenant별 사용량 집계 및 모니터링

### 3. Nextcloud 통합
- OCS Provisioning API를 통한 사용자/그룹/Quota 조회
- 필요 시 새로고침 버튼으로 최신 데이터 조회

### 4. 에러 핸들링
- Nextcloud API 장애 시 5xx 에러 반환
- 민감정보 자동 필터링
- 상세한 에러 메시지 제공

### 5. 자동 초기화
- Docker Compose 실행 시 자동으로 그룹/사용자/Quota 생성
- 개발 환경 즉시 사용 가능

### 접속

| 서비스 | URL | 계정 |
|--------|-----|------|
| **Frontend** | http://localhost:5173 | - |
| **Admin UI** | http://localhost:5173/admin/storage | - |
| **Backend API** | http://localhost:8080/api/tenants | - |
| **Nextcloud** | http://localhost:8081 | admin / adminpass |
| **Nextcloud 테스트 유저** | http://localhost:8081 | tenant-a-u1 / S3curePass_2026! |

### 확인

```bash
# Backend 헬스 체크
curl http://localhost:8080/actuator/health

# Tenant 목록 조회
curl http://localhost:8080/api/tenants

# Tenant 사용량 조회
curl http://localhost:8080/api/tenants/1/usage
```

### 종료

```bash
# 컨테이너 중지
docker compose down

# 데이터 포함 완전 삭제
docker compose down -v
```

---

## 🔧 Nextcloud 자동 초기화

Docker Compose 실행 시 자동으로 생성되는 테스트 데이터:

- **그룹**: `tenant-a`, `tenant-b`
- **사용자** (각 그룹당 3명):
  - tenant-a: `tenant-a-u1`, `tenant-a-u2`, `tenant-a-u3`
  - tenant-b: `tenant-b-u1`, `tenant-b-u2`, `tenant-b-u3`
- **비밀번호**: `S3curePass_2026!`
- **Quota**: 100MB (사용자당)

자동 초기화 비활성화:

```env
# .env
NC_AUTO_INIT=false
```

---

## 🌐 환경 변수

주요 환경 변수는 `.env.example`을 참고하세요. 기본값으로 실행 가능합니다.

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `NEXTCLOUD_BASE_URL` | `http://nextcloud` | Nextcloud API URL |
| `NEXTCLOUD_USERNAME` | `admin` | API 인증 ID |
| `NEXTCLOUD_PASSWORD` | `adminpass` | API 인증 비밀번호 |
| `USAGE_QUOTA_BYTES_DEFAULT` | `104857600` | 기본 Quota (100MB) |

---

## � 사용량 갱신

사용량 데이터는 자동으로 갱신되지 않으며, 필요 시 수동으로 새로고침할 수 있습니다.

### Frontend UI에서 갱신

Admin Storage 페이지 (`http://localhost:5173/admin/storage`)에서 "🔄 Refresh" 버튼 클릭

### API로 갱신

```bash
# 특정 Tenant 사용량 강제 갱신
curl -X POST http://localhost:8080/api/tenants/1/usage/refresh
```

---

## 🐛 문제 해결

### Backend가 Nextcloud에 연결되지 않음

```bash
# Nextcloud 컨테이너 상태 확인
docker compose ps nextcloud

# Nextcloud 로그 확인
docker compose logs nextcloud | Select-String -Pattern "error" -Context 2

# Backend에서 접근 테스트
docker compose exec backend curl http://nextcloud/status.php
```

### 사용량이 업데이트되지 않음

```bash
# 캐시 상태 확인
curl http://localhost:8080/api/tenants/1/usage/health

# 수동 갱신
curl -X POST http://localhost:8080/api/tenants/1/usage/refresh
```

### 데이터베이스 초기화

```bash
# 모든 데이터 삭제 및 재시작
docker compose down -v
docker compose up -d
```

### 로그 확인

```powershell
# 모든 서비스 로그
docker compose logs -f

# 특정 서비스만
docker compose logs -f backend
docker compose logs -f nextcloud

# 최근 100줄
docker compose logs --tail=100 backend
```

---

## 📖 추가 문서

- [API_EXAMPLES.md](API_EXAMPLES.md) - 전체 API 응답 예시
