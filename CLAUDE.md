# DevOps Backend (택시 배차 서비스)

현대오토에버 모빌리티 SW 스쿨 4기 DevOps 프로젝트의 백엔드. 첫 버전은 **꼭 필요한 CRUD와 호출·운행 흐름만** 구현한다.

- 저장소: https://github.com/mobility-devops/DevOps_Backend.git (`main`, `develop`)
- 설계 원본: Notion "DevOps Project Backend Architecture" (프로젝트 Todo → 완료). 설계 변경 시 Notion 기준을 따른다.

## 기술 스택

- Java 21, Gradle(Groovy), Spring Boot 4.1.1
- Spring Web(MVC), Validation, Spring Data JPA, Flyway(+`flyway-mysql`), Lombok
- MySQL 8 (포트 **3306**, DB명 `taxi`). MariaDB는 사용하지 않는다.
- 패키지 루트: `kim.autoever.taxi`
- Swagger(springdoc)는 Issue #10에서 추가. Boot 4 호환 버전을 확인하고 넣는다.

## 첫 버전 범위

포함: 사용자·기사·차량·호출 CRUD, 호출 생성 → 수락 → 도착 → 시작 → 완료/취소 흐름.

제외: 인증(JWT/Spring Security), Redis·위치·자동 배차, `dispatch_offers`, SSE, outbox, idempotency, `ride_events`, Docker·Jenkins·Kubernetes.
Spring Security 의존성은 넣지 않는다(넣으면 모든 API가 401).

### 수락 방식 (A안)
기사가 `GET /api/v1/rides?status=SEARCHING`으로 호출 목록을 보고 `POST /api/v1/rides/{rideId}/accept`로 직접 수락한다.
중복 수락은 `rides.version`(낙관적 락)과 `active_assignments`(driver_id UNIQUE)로 막는다.

## 아키텍처 규칙

```
Controller → Service → Repository → Database
```

```
kim.autoever.taxi
├── user / driver / vehicle / ride   (각각 controller, service, repository, domain, dto)
├── common (exception, response)
└── config
```

- Entity를 API 응답으로 직접 반환하지 않는다. 요청·응답 DTO를 분리한다.
- Controller에는 업무 로직을 두지 않는다. 권한·상태 검사와 트랜잭션은 Service에서 처리한다.
- 상태 변경 메서드에는 `@Transactional`을 사용한다.
- 상태 전환 규칙은 `Ride` 엔티티 메서드(`accept()`, `arrive()` 등)에 둔다.
- 생성·수정 시간은 공통 `BaseEntity`(JPA Auditing)로 관리한다.
- `GlobalExceptionHandler`로 오류 응답을 통일한다: `{ "code", "message", "requestId" }`.
- 상태(enum)는 `@Enumerated(EnumType.STRING)`으로 저장한다.
- 테이블은 Flyway(`src/main/resources/db/migration/V{n}__설명.sql`)로만 만든다. `ddl-auto: validate`.
- API 경로 접두사: `/api/v1`. 시각은 UTC ISO 8601.

## 상태 정의

호출(ride): `SEARCHING → ASSIGNED → ARRIVED → IN_PROGRESS → COMPLETED`
취소: `SEARCHING`, `ASSIGNED`, `ARRIVED`에서만 `CANCELLED` 가능. 배차 실패는 `NO_DRIVER`(자동 배차 도입 후).

기사: `availability` = `ONLINE` / `OFFLINE`. 운행 중 여부는 `active_assignments`로 판단하며, 기사가 임의로 해제할 수 없다.

| API | 이전 상태 | 변경 상태 |
|---|---|---|
| accept | SEARCHING | ASSIGNED |
| arrive | ASSIGNED | ARRIVED |
| start | ARRIVED | IN_PROGRESS |
| complete | IN_PROGRESS | COMPLETED |
| cancel | SEARCHING, ASSIGNED, ARRIVED | CANCELLED |

완료·취소 시 활성 호출·배정 정보(`active_passenger_rides`, `active_assignments`)를 같은 트랜잭션에서 해제한다.

## 테이블 (첫 버전)

`users`, `drivers`, `vehicles`, `rides`, `active_passenger_rides`, `active_assignments`
- 승객 한 명은 진행 중인 호출 1개, 기사 한 명은 활성 운행 1개, 호출 하나에는 기사 한 명.
- `vehicles.plate_number` UNIQUE.
- `rides.driver_id`는 수락 전 NULL 허용.

### 기사·차량 (확정)
- 기사 정보(`drivers`)는 DRIVER 사용자가 처음 사용할 때 `OFFLINE`으로 자동 생성한다(`DriverService.getOrCreate`).
- 차량은 기사 1명당 1대. 경로는 `/api/v1/drivers/me/vehicle`(GET/POST/PUT/DELETE), 상태 변경은 `PUT /api/v1/drivers/me/availability`.
- 기사 전용 API는 `loginUser.requireRole(UserRole.DRIVER)`로 막는다.
- 호출 수락 구현 시 운행 중인 기사는 OFFLINE으로 바꿀 수 없게 막는다(`active_assignments` 도입 후).

### 사용자 식별 (확정)
인증(JWT)은 제외하고 요청 헤더 `X-User-Id`로 사용자를 식별한다. 요청 본문의 사용자 ID는 신뢰하지 않는다.
- `users`(id, name, phone, role = `PASSENGER` / `DRIVER`), 가입은 `POST /api/v1/users`.
- 헤더 값을 읽는 로직은 한 곳(ArgumentResolver 또는 Filter)에 모아 나중에 JWT로 교체하기 쉽게 한다.
- 헤더 누락 `401`, 존재하지 않는 사용자 `404`, role 불일치(승객이 기사 API 호출 등) `403`.
- `/rides/current`, `/drivers/me/...` 같은 "내 것" 경로는 이 헤더 기준으로 동작한다.

## 로컬 실행

- DB 접속 정보는 환경변수로만 받는다: `DB_USERNAME`(기본 `taxi`), `DB_PASSWORD`. **비밀번호를 코드·커밋에 넣지 않는다.**
- `application-local.properties`, `.env`는 `.gitignore` 대상이다.
- 포트 충돌 확인: `netstat -ano | findstr ":3306"` (MariaDB 서비스는 비활성화되어 있어야 한다.)

## Git·GitHub 흐름

`Issue → feature 브랜치 → 구현 → PR(develop 대상) → Squash and merge`

- `main`: 완성 버전, `develop`: 통합. 버전 완성 시 `develop → main` PR로 v1.0.0. 모든 PR(`develop → main` 포함)은 **Squash and merge**로 병합한다(팀 Notion 규칙).
- 브랜치명에 `#`을 넣지 않는다: `chore/1-project-setup`, `feature/3-...`, `fix/`, `docs/`, `test/`.
- 브랜치는 항상 최신 `develop`에서 만든다. **`develop`/`main`에 직접 커밋하지 않는다.**
- 커밋 메시지: `feat: …`, `fix: …`, `chore: …`, `test: …`, `docs: …`, `refactor: …`. 작업 단위별로 나눠 커밋하고 Issue 번호는 매번 넣지 않는다.
- PR 제목 `[Feat] 설명`, 본문에 `Closes #번호`, 병합 커밋은 `feat: 설명 (#번호)`.
- Issue는 작업 내용 / 세부 작업 / API / 완료 기준을 반드시 포함한다.
- Issue 생성, PR 생성·병합, push, 기본 브랜치 변경은 사용자 확인 후에 한다.

## Issue 순서

1. [Chore] Spring Boot 프로젝트 초기 설정 (Java 21, Gradle, 패키지 구조, application.properties, MySQL, Flyway)
2. [Feat] 공통 응답 및 예외 처리
3. [Feat] 사용자 구현 (식별 방식 확정 후)
4. [Feat] 기사 availability 및 차량 CRUD
5. [Feat] 택시 호출 생성 및 조회
6. [Feat] 택시 호출 수락 (A안, 중복 수락 방지)
7. [Feat] 운행 상태 변경 (arrive / start / complete)
8. [Feat] 택시 호출 취소
9. [Docs] Swagger API 문서
10. [Test] 택시 호출 전체 흐름 통합 테스트

## 현재 진행 상태

- 저장소 clone 완료, `develop` 브랜치, IntelliJ에서 프로젝트 열림.
- 패키지를 `kim.autoever.taxi`로 이동했고 `group`도 `kim.autoever`로 변경했다. 설정 파일은 `application.properties`를 사용한다(yml 아님). `V1__init.sql` 추가, `HELP.md` 삭제. 이 변경은 Issue #1 브랜치에서 커밋한다.
- GitHub Issue는 사용자가 직접 생성·관리한다(Issue 번호와 라벨은 저장소에서 확인). 저장소는 비공개(PRIVATE)다.
