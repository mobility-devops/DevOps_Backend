# 택시 배차 서비스 백엔드 (DevOps Backend)

현대오토에버 모빌리티 SW 스쿨 4기 DevOps 프로젝트의 백엔드 저장소.
첫 버전은 **꼭 필요한 CRUD와 호출·운행 흐름만** 구현하는 것을 목표로 함.

## 진행 상황 (2026-09-30 기준)

- 첫 버전 범위의 **Issue #1 ~ #14 전체 완료**, PR #2 ~ #20 `develop` 병합 완료
- 단위·통합 테스트 **133개 통과** (실제 MySQL 8 기준, 통합 테스트 19개 포함)
- 릴리스(`develop → main`, v1.0.0)는 미진행

| Issue | 내용 | PR | 상태 |
|---|---|---|---|
| #1 | Spring Boot 프로젝트 초기 설정 | #2 | 완료 |
| #3 | 공통 응답 및 예외 처리 | #4 | 완료 |
| #5 | 사용자 CRUD 및 사용자 식별 | #6 | 완료 |
| #7 | 기사 상태 및 차량 CRUD | #8 | 완료 |
| #9 | 택시 호출 생성 및 조회 | #15 | 완료 |
| #10 | 택시 호출 수락 (중복 수락 방지) | #16 | 완료 |
| #11 | 운행 상태 변경 (도착·시작·완료) | #17 | 완료 |
| #12 | 택시 호출 취소 | #18 | 완료 |
| #13 | Swagger API 문서 | #19 | 완료 |
| #14 | 택시 호출 전체 흐름 통합 테스트 | #20 | 완료 |

## 기술 스택

- Java 21, Gradle(Groovy), Spring Boot 4.1.1
- Spring Web(MVC), Validation, Spring Data JPA, Flyway, Lombok
- MySQL 8 (포트 3306, DB명 `taxi`, 테스트 전용 DB `taxi_test`)
- springdoc-openapi 3.1.1 (Swagger UI)
- 패키지 루트: `kim.autoever.taxi`

## 호출 상태 흐름

```
SEARCHING ─accept→ ASSIGNED ─arrive→ ARRIVED ─start→ IN_PROGRESS ─complete→ COMPLETED
    │                  │                │
    └──────────────────┴────────────────┴── cancel → CANCELLED
```

- 취소는 `SEARCHING`, `ASSIGNED`, `ARRIVED` 상태에서만 가능함 (`IN_PROGRESS` 이후 취소 불가)
- 상태 전환 규칙은 `Ride` 엔티티의 도메인 메서드(`accept()`, `arrive()`, `start()`, `complete()`, `cancel()`)에 위치
- 완료·취소 시 활성 호출·배정 정보(`active_passenger_rides`, `active_assignments`)를 같은 트랜잭션에서 해제
- 기사 수락 방식: 기사가 `GET /api/v1/rides?status=SEARCHING`으로 목록을 확인한 뒤 직접 수락 (A안)

## API 목록

모든 경로 접두사는 `/api/v1`이며, `POST /users`를 제외한 모든 API는 `X-User-Id` 헤더 필요.

**사용자**

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/users` | 사용자 가입 (PASSENGER / DRIVER) |
| GET | `/me` | 내 정보 조회 |
| PUT | `/me` | 내 정보 수정 |

**기사·차량 (DRIVER 전용)**

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/drivers/me` | 내 기사 정보 조회 (최초 조회 시 OFFLINE으로 자동 생성) |
| PUT | `/drivers/me/availability` | ONLINE / OFFLINE 변경 (운행 중 OFFLINE 변경 불가) |
| POST | `/drivers/me/vehicle` | 차량 등록 (기사당 1대) |
| GET | `/drivers/me/vehicle` | 내 차량 조회 |
| PUT | `/drivers/me/vehicle` | 차량 수정 |
| DELETE | `/drivers/me/vehicle` | 차량 삭제 |

**호출**

| 메서드 | 경로 | 권한 | 설명 |
|---|---|---|---|
| POST | `/rides` | PASSENGER | 호출 생성 (승객당 진행 중인 호출 1개) |
| GET | `/rides/current` | PASSENGER | 내 진행 중 호출 조회 (없으면 `{"ride": null}`) |
| GET | `/rides/{rideId}` | 승객·배정 기사 | 호출 단건 조회 |
| GET | `/rides?status=SEARCHING` | DRIVER | 상태별 호출 목록 (생략 시 SEARCHING) |
| POST | `/rides/{rideId}/accept` | DRIVER | 호출 수락 |
| POST | `/rides/{rideId}/arrive` | 배정 기사 | 출발지 도착 |
| POST | `/rides/{rideId}/start` | 배정 기사 | 운행 시작 |
| POST | `/rides/{rideId}/complete` | 배정 기사 | 운행 완료 |
| POST | `/rides/{rideId}/cancel` | 호출한 승객 | 호출 취소 |

## 핵심 설계

**사용자 식별**
- 인증(JWT) 제외, 요청 헤더 `X-User-Id`로 사용자 식별
- 헤더 처리 로직을 `CurrentUserArgumentResolver` 한 곳에 모아 이후 JWT 교체 용이
- 헤더 누락 `401`, 존재하지 않는 사용자 `404`, 역할 불일치 `403`

**오류 응답 형식 통일**
- `GlobalExceptionHandler`로 `{ "code", "message", "requestId" }` 형식 통일 (입력값 오류 시 `errors` 추가)
- `X-Request-Id` 응답 헤더와 `requestId` 값 일치

**중복 처리 방지**
- 승객당 진행 중인 호출 1개: `active_passenger_rides.passenger_id` UNIQUE
- 기사당 활성 운행 1개, 호출당 배정 기사 1명: `active_assignments`의 `driver_id`, `ride_id` UNIQUE
- 중복 수락: `rides.version` 낙관적 락 + UNIQUE 제약
- 동시성 실패(낙관적 락 충돌, 데드락, 락 대기 초과)는 `409`로 통일 응답

**동시 수락 데드락 수정 (통합 테스트에서 발견)**
- 실제 MySQL 동시성 테스트에서 `active_assignments` 삽입 시 데드락으로 `500` 발생 확인
- `rides` 갱신을 먼저 flush한 뒤 배정 정보를 저장하도록 순서 변경
- `ConcurrencyFailureException` 전체를 `409`로 처리

**API 문서**
- Swagger UI에서 우측 상단 **Authorize**에 사용자 ID를 입력하면 모든 요청에 `X-User-Id` 헤더 포함
- 모든 4xx·5xx 응답에 공통 `ErrorResponse` 스키마 자동 연결

## 데이터베이스

Flyway 마이그레이션(`src/main/resources/db/migration`)으로만 테이블 생성, `ddl-auto: validate`.

| 버전 | 내용 |
|---|---|
| V1 | 초기화 |
| V2 | `users` |
| V3 | `drivers`, `vehicles` |
| V4 | `rides`, `active_passenger_rides` |
| V5 | `active_assignments` |

- 상태(enum)는 `@Enumerated(EnumType.STRING)`으로 저장
- 생성·수정 시각은 `BaseEntity`(JPA Auditing)로 관리, 시각은 UTC ISO 8601

## 프로젝트 구조

```
kim.autoever.taxi
├── user / driver / vehicle / ride   (controller, service, repository, domain, dto)
├── common                           (exception, response, domain)
└── config                           (JPA Auditing, WebConfig, OpenAPI)
```

- 계층: `Controller → Service → Repository → Database`
- Entity 직접 반환 금지, 요청·응답 DTO 분리
- 권한·상태 검사와 트랜잭션은 Service에서 처리

## 실행 방법

**1. MySQL 준비**

```sql
CREATE DATABASE taxi CHARACTER SET utf8mb4;
CREATE DATABASE taxi_test CHARACTER SET utf8mb4;
CREATE USER 'taxi'@'localhost' IDENTIFIED BY '<비밀번호>';
GRANT ALL PRIVILEGES ON taxi.* TO 'taxi'@'localhost';
GRANT ALL PRIVILEGES ON taxi_test.* TO 'taxi'@'localhost';
```

**2. 환경변수 설정 (비밀번호를 코드·커밋에 포함하지 않음)**

```bash
export DB_USERNAME=taxi      # 기본값 taxi
export DB_PASSWORD='<비밀번호>'
```

**3. 애플리케이션 실행**

```bash
./gradlew bootRun
```

- 시작 시 Flyway가 마이그레이션 자동 적용
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI 문서: `http://localhost:8080/v3/api-docs`

**4. 테스트 실행**

```bash
./gradlew test
```

- 통합 테스트는 `test` 프로파일의 `taxi_test` DB 사용 (개발용 `taxi` DB 영향 없음)
- 매 테스트 전에 `taxi_test`의 모든 데이터 삭제
- `gradlew` 실행 권한이 없는 환경에서는 `bash ./gradlew test`로 실행

## 테스트 구성

- 단위 테스트 114개: 서비스·컨트롤러(MockMvc standalone)·예외 처리
- 통합 테스트 19개: 실제 MySQL 사용
  - 전체 흐름: 가입 → 호출 → 수락 → 도착 → 시작 → 완료, 취소 흐름
  - 예외 시나리오: 중복 호출·수락, OFFLINE·운행 중 기사, 상태 순서 위반, 권한 `403`, `404`, `401`, 좌표 `400`
  - 동시성: 동시 수락, 한 기사의 동시 다중 수락, 동시 호출, 취소·수락 동시 요청

## Git·GitHub 흐름

```
Issue → feature 브랜치 → 구현 → PR(develop 대상) → Squash and merge
```

- `main`: 완성 버전, `develop`: 통합 (직접 커밋 금지)
- 브랜치명: `feature/번호-설명`, `fix/`, `docs/`, `test/`, `chore/`
- 커밋 메시지: `feat:`, `fix:`, `docs:`, `test:`, `chore:`, `refactor:` 접두사
- PR 제목 `[Feat] 설명`, 본문에 `Closes #번호`, 병합 커밋 `feat: 설명 (#번호)`
- 모든 PR(`develop → main` 포함)은 Squash and merge

## 범위 및 향후 과제

첫 버전에서 제외한 항목.

- 인증(JWT, Spring Security)
- Redis, 위치 기반 조회, 자동 배차, `dispatch_offers`
- SSE, outbox, idempotency, `ride_events`
- Docker, Jenkins, Kubernetes
- 배차 실패 상태 `NO_DRIVER` (자동 배차 도입 후)
