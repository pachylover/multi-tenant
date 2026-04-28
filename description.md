 Docker 구성(Docker compose로 구성)

1. backend

2. frontend

3. DB

4. Nextcloud


DB

postgres


backend

Spring Boot


frontend

ReactJS


프로젝트 설명

multi tenant 구조로 구축

회사(tenant) + 회사별 회원 구조(1:n 구조)

회사는 nextcloud group으로 매핑함

회사별 회원에 quota 100MB를 제공함

Nextcloud 인증정보(password 등)는 환경변수로 관리(하드코딩 절대 X)


화면 목록

관리자 페이지(/admin/storage)

 - 회사 선택 select box

 - 사용자의 usage 리스트를 테이블로 표시

 - 선택된 회사의 사용자 usage 리스트를 테이블로 표시

   - 표시 항목

     1. 사용자 ID

     2. 사용 용량(MB)

     3. 할당 용량(MB)

     4. 사용률(%)

 - 사용자별 사용률 progress bar 표시(회색과 빨간색 조합)

 - socketIO 활용하여 변경 시 자동 갱신


API

Nextcloud OCS provisioning API를 활용하여 사용자의 used/quota 를 리스트로 응답하는 API

tenant 별 사용자 사용자 리스트를 제공하는 API

API의 필수 응답필드

  1. tenantId

  2. userId

  3. usedBytes

  4. quotaBytes

  5. usagePercent

  6. lastCollectedAt

socketIO 활용하여 정보 변경 시 해당 회사정보가 변경되었다는 정보를 전송


Nextcloud

tenant-a, tenant-b를 Nextcloud Group으로 생성

tenant별 사용자 3인 생성 후 그룹별로 소속시킴

사용자별 quota 100MB 설정


DB

flyway활용하여 DB 버전 관리 기능 추가

테이블 목록

1. tenants

  - tenant_id(PK), name, nc_group_id, created_at

2. users

  - user_id(PK), tenant_id(FK), email, nc_user_id, created_at


테스트

1. tenant-a 사용자 목록에서

- 사용자 A: quota 100MB, used 50MB → 50%로 표시

2. tenant-b 사용자 목록이 tenant-a 와 분리되어 조회되는지 확인

3. quota 또는 used 가 0 인 사용자도정상 표시되는지 확인

4. Nextcloud API 장애 시(임의로 토큰변경 등)

- Backend 가 적절히 실패처리하는지 확인

5. selenium활용하여 end-to-end 테스트


참고

1. 실제 used/quota 는 Nextcloud 에서조회하여 화면에 표시한다.

2. 캐싱 활용하여 부하를 줄일 수 있게 한다. 