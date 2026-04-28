## Multi-tenant Nextcloud storage monitor

구성( `docker compose` ):
- **DB**: Postgres
- **Backend**: Spring Boot (API + Socket.IO)
- **Frontend**: React ( `/admin/storage` )
- **Nextcloud**: OCS provisioning API로 used/quota 조회

### Quick start

1) 환경변수 준비 (선택사항 - 기본값으로 실행 가능)

```bash
copy .env.example .env
# .env 파일을 필요에 따라 수정
```

환경변수 파일을 생성하지 않아도 기본값으로 실행됩니다.

2) 실행

```bash
docker-compose up -d
```

첫 실행 시 이미지 빌드와 Nextcloud 초기화로 인해 2-3분 정도 소요될 수 있습니다.

3) 접속
- **Frontend**: `http://localhost:5173/admin/storage`
- **Backend API**: `http://localhost:8080/api/tenants`
- **Nextcloud**: `http://localhost:8081`
  - 관리자: admin / adminpass
  - 테스트 유저: tenant-a-u1, tenant-b-u1 등 / S3curePass_2026!

4) 종료

```bash
docker-compose down
```

데이터를 포함하여 완전히 제거하려면:
```bash
docker-compose down -v
```

### Nextcloud 초기 데이터(그룹/유저/quota) 자동 init

기본값으로 `docker compose up` 시 아래가 **자동 생성**됩니다.
- 그룹: `tenant-a`, `tenant-b`
- 유저: 각 그룹별 3명
- 유저 quota: 100MB
- trusted domain: `localhost`, `nextcloud`

자동 init을 끄려면 `.env`에서:

```bash
NC_AUTO_INIT=false
```

### Nextcloud 초기 데이터(수동)

Nextcloud 컨테이너 내부에서 `occ`로 그룹/유저를 만들 수 있습니다.

```bash
docker compose exec -u www-data nextcloud php occ group:add tenant-a
docker compose exec -u www-data nextcloud php occ group:add tenant-b

docker compose exec -u www-data nextcloud php occ user:add --password-from-env --display-name "A1" tenant-a-u1
docker compose exec -u www-data nextcloud php occ user:add --password-from-env --display-name "A2" tenant-a-u2
docker compose exec -u www-data nextcloud php occ user:add --password-from-env --display-name "A3" tenant-a-u3

docker compose exec -u www-data nextcloud php occ user:add --password-from-env --display-name "B1" tenant-b-u1
docker compose exec -u www-data nextcloud php occ user:add --password-from-env --display-name "B2" tenant-b-u2
docker compose exec -u www-data nextcloud php occ user:add --password-from-env --display-name "B3" tenant-b-u3

docker compose exec -u www-data nextcloud php occ group:adduser tenant-a tenant-a-u1
docker compose exec -u www-data nextcloud php occ group:adduser tenant-a tenant-a-u2
docker compose exec -u www-data nextcloud php occ group:adduser tenant-a tenant-a-u3
docker compose exec -u www-data nextcloud php occ group:adduser tenant-b tenant-b-u1
docker compose exec -u www-data nextcloud php occ group:adduser tenant-b tenant-b-u2
docker compose exec -u www-data nextcloud php occ group:adduser tenant-b tenant-b-u3
```

신규 유저 비밀번호 정책 때문에, `--password-from-env` 사용 시 **강한 비밀번호**를 `OC_PASS`로 넘겨야 합니다.

```bash
docker compose exec -u www-data -e OC_PASS="S3curePass_2026!" nextcloud php occ user:add --password-from-env --display-name "A1" tenant-a-u1
```

Backend 컨테이너에서 Nextcloud를 호출하려면 `nextcloud` 호스트가 trusted domain에 있어야 합니다(도커 네트워크 내부 호스트명).

```bash
docker compose exec -u www-data nextcloud php occ config:system:set trusted_domains 1 --value=nextcloud
```

Quota 100MB 설정(예: user별):

```bash
docker compose exec -u www-data nextcloud php occ user:setting tenant-a-u1 files quota 100 MB
```

### Notes
- **Nextcloud 인증정보는 환경변수로만 관리**합니다. (코드 하드코딩 없음)
- `GET /api/tenants/{tenantId}/usage` 는 Nextcloud 그룹 멤버를 조회하고, 유저별 used/quota를 가져와 반환합니다.
- 변경 이벤트는 Socket.IO 이벤트 `tenantUsageUpdated` 로 브로드캐스트됩니다.

### 유용한 명령어

서비스 상태 확인:
```bash
docker-compose ps
```

로그 확인:
```bash
# 모든 서비스 로그
docker-compose logs -f

# 특정 서비스 로그
docker-compose logs -f backend
docker-compose logs -f nextcloud
docker-compose logs -f frontend
```

서비스 재시작:
```bash
# 모든 서비스
docker-compose restart

# 특정 서비스만
docker-compose restart backend
```

컨테이너 접속:
```bash
# Backend 컨테이너
docker-compose exec backend sh

# Nextcloud 컨테이너
docker-compose exec nextcloud bash
```

### 문제 해결

**서비스가 시작되지 않는 경우:**
```bash
# 컨테이너 상태 확인
docker-compose ps

# 로그에서 에러 확인
docker-compose logs
```

**Nextcloud 초기화가 안 된 경우:**
```bash
# Nextcloud 로그 확인
docker-compose logs nextcloud

# 수동으로 재초기화 (주의: 기존 데이터 삭제)
docker-compose down -v
docker-compose up -d
```

**Backend가 Nextcloud에 연결하지 못하는 경우:**
```bash
# Backend 로그 확인
docker-compose logs backend

# Nextcloud trusted domain 확인
docker-compose exec -u www-data nextcloud php occ config:system:get trusted_domains
```

**포트 충돌이 발생하는 경우:**

docker-compose.yml의 포트를 수정하세요:
- Frontend: `5173:80` → `다른포트:80`
- Backend: `8080:8080` → `다른포트:8080`
- Nextcloud: `8081:80` → `다른포트:80`

**완전히 재설치하려면:**
```bash
# 모든 컨테이너와 볼륨 삭제
docker-compose down -v

# 이미지도 삭제하고 싶다면
docker-compose down -v --rmi all

# 다시 시작
docker-compose up -d --build
```

