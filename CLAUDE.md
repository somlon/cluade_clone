# ddingconnect

명지대 캡스톤 디자인 — 졸업생/재학생 커뮤니티 플랫폼 백엔드.
원본: `ddingconnect-backend` PR #15 스냅샷 (Merge PR #1, 2026-05-11).

## 기술 스택

- Spring Boot 3.5.13 / Java 17 (toolchain)
- Spring Data JPA + MySQL (`mysql-connector-j`)
- Spring Security + JWT (`jjwt 0.12.6`, HS512)
- Swagger (`springdoc-openapi-starter-webmvc-ui 2.7.0`)
- Lombok, Validation, `spring-dotenv` (`.env`)
- 테스트: JUnit 5, `spring-security-test`

## 빌드 특이사항

- `build.gradle:12` — build 디렉토리를 `C:/gradle-builds/${rootProject.name}` 으로 설정 (OneDrive 동기화 잠금 회피 목적, **Windows 의존**). 리눅스/맥에서 빌드하려면 이 라인을 우회해야 함.
- Spring profiles: `db, s3, oauth, jwt` (기본 활성)
- 설정 분리: `application.yml`, `application-db.yml`, `application-jwt.yml`

## 패키지 구조

```
mju.capstone.ddingconnect
├── DdingconnectApplication.java
├── domain/
│   ├── coffeechat/      # 커피챗 매칭
│   ├── interested_job/  # TargetJob (관심 직군)
│   ├── job_post/        # PostContents + GraduateJobPost
│   ├── member/          # Member + Student/Graduate
│   ├── qna/answer/
│   ├── qna/question/
│   ├── roadmap/
│   └── techstack/
└── global/
    ├── auth/            # 회원가입/로그인, JWT 필터, @LoginMember
    ├── common/          # BaseEntity (createdAt/updatedAt)
    ├── config/          # Security, Swagger, JpaAuditing, WebMvc, Mail, Sse
    ├── jwt/             # JwtUtil(Impl)
    ├── redis/           # RedisUtil (StringRedisTemplate 래퍼)
    ├── response/        # ApiResponse, ErrorStatus, ExceptionAdvice
    └── sse/             # 통합 알람(4종 조회/상세/읽음) + SSE 실시간 푸시
```

각 도메인은 `controller / service / domain(entity+repository) / dto` 4계층으로 일관됨.

## 핵심 도메인 규칙

### 회원 (Member)
- `MemberRole`: `UNKNOWN | STUDENT | GRADUATE`
- 회원가입 시 role에 따라 `Student` 또는 `Graduate` 레코드를 함께 생성 (`AuthServiceImpl.createRoleRecord`)
- **탈퇴 = hard delete (`MemberServiceImpl.withdraw`)**: 회원이 작성/소유/수신한 모든 자식 행을 단일 `@Transactional` 안에서 정리한 뒤 `Member` row 자체를 삭제. 위임 순서 = (1) CoffeeChat 양방향(requester/receiver) 모두 정리(서비스 우회, 알람도 함께 삭제) → (2) 본인 작성 부모 row 들을 각 도메인 `delete()` 로 캐스케이드 위임 (Question/Answer/Roadmap, GRADUATE 면 JobPost) → (3) 본인 수신 leaf 알람·좋아요 `deleteByMemberId` (AnswerAlarm/JobAlarm/RoadmapAlarm/CoffeeChatAlarm/QuestionLike/AnswerLike) → (4) TechStack/TargetJob 본인 row → (5) Student/Graduate 매핑 → (6) Member. `isDeleted` 컬럼은 스키마에 남기되 기본 `false` 고정 (다른 용도 보존). `JwtAuthenticationFilter` 가 `findById` 실패 시 자동으로 인증 실패 처리하므로 토큰 무효화 별도 작업 불필요.
- **소셜 링크 검증 (`UpdateMemberRequest`)**: `githubLink` 는 `^https?://(www\.)?github\.com/.+`, `linkedinLink` 는 `^https?://(www\.)?linkedin\.com/.+` 패턴 매칭 (Bean Validation `@Pattern`). `null` 통과 (미입력 = 변경 없음), 빈 문자열·도메인 불일치는 400. 컨트롤러는 `@Valid` 필수. 검증 실패 시 `MethodArgumentNotValidException` 핸들러에서 `_BAD_REQUEST` 로 매핑.
- **`Student.grade` 검증 (`MemberServiceImpl.sanitizeGrade`)**: `grade < 1` → 400 (`MEMBER_INVALID_GRADE`). `grade > 4` → 4 로 클램프 (`Math.min(raw, 4)`)해서 저장. `null` 은 기존값 유지.
- **역할 외 필드 거부 (`MemberServiceImpl.validateRoleFields`)**: 단일 `UpdateMemberRequest` 에 STUDENT/GRADUATE 양쪽 필드가 다 있어, 본인 역할 외 필드를 non-null 로 보내면 즉시 400 (`MEMBER_FIELD_ROLE_MISMATCH`). STUDENT → graduate 필드 거부, GRADUATE → grade 거부, UNKNOWN → 양쪽 다 거부. 공통 필드만 보내는 건 항상 통과.

### 인증/JWT
- `/api/v1/auth/signup`, `/api/v1/auth/login` 만 화이트리스트
- 화이트리스트: `swagger-ui/**`, `v3/api-docs/**`, `api/v1/auth/**`, `actuator/health`
- 토큰: HS512, `Authorization: Bearer <token>`, 클레임 키 `id` (회원 PK)
- `JwtAuthenticationFilter` → `CustomUserDetails` → `@LoginMember`로 컨트롤러에 주입
- 비밀번호: `BCryptPasswordEncoder`

### 구직 공고 (job_post)
- **졸업생만 등록 가능** (`MemberRole.GRADUATE` 검증, `POST_CONTENTS_NOT_GRADUATE`)
- `PostContents` (공고 본문) + `GraduateJobPost` (졸업생-공고 매핑)으로 분리
- 수정/삭제는 매핑 테이블 통해 소유자 확인
- `PostContents.jobType` enum 은 공고의 직무 속성. 구직 정보 화면의 **직무 필터** 용도 (`JobType` enum)
- **삭제 캐스케이드 (서비스 레벨)**: `PostContents` 삭제 시 자식 행을 **서비스 코드에서 명시적으로 먼저 삭제** (`JobPostServiceImpl.delete`). 순서 = `JobAlarm` → `GraduateJobPost` → `PostContents`. 이유: `GraduateJobPost.postContents`, `JobAlarm.postContents` 가 `nullable = false` FK 인데 JPA cascade 설정이 없어, 부모만 지우면 영속성 컨텍스트의 자식이 transient 부모를 참조하게 되어 `TransientObjectException` 으로 터짐. 알람도 함께 hard delete 정책 (사용자의 알람 이력은 손실되지만, 공고가 사라진 상태의 dangling 알람을 막음).
- **update 시 jobType 변경 알람 디스패치 (`JobPostServiceImpl.update`)**: `oldJobType != newJobType` 일 때, 이 공고의 기존 `JobAlarm` 수신자 (= prev) 와 새 `jobType` 매칭 학생 (= curr) 의 차집합을 계산해 두 종류 알람을 발행. **Removed = prev − curr** → `"관심 직군에서 벗어난 공고로 변경되었습니다."`, **Added = curr − prev** → `"관심 직무에 새로운 공고가 등록되었습니다."`. 기존 알람 row 는 보존(삭제·갱신 X). 등록 졸업생 본인 항상 제외. prev 집합 정의는 이 공고(`postContentsId`)에 연결된 `JobAlarm` row 만 본다 (다른 알람 타입/다른 공고는 무관).

### 관심 직군 (interested_job)
- 학생 마이페이지의 **관심 직군 칩** — 회원 ↔ `TargetJobCategory` enum 단순 매핑. `PostContents`(구직 공고)와는 FK 관계 **없음**.
- 등록 시 필요한 입력은 카테고리(`interestedJob`) 하나뿐. 공고 ID 같은 FK는 받지 않음.
- `TargetJob` 엔티티는 `member_id`(NOT NULL FK) + `Interested_Job`(enum) + `key2`(미사용, ERD 잔재) 로 구성.
- 본인 소유자만 수정/삭제 (`TARGET_JOB_UNAUTHORIZED`).
- `TargetJobCategory` 와 `JobType` 은 같은 원티드 직군 태그 소스에서 11개 동일 값을 받지만 **타입 분리**. 두 enum 의 값 정렬은 사람이 직접 유지해야 함.
- 공고 ↔ 관심 직군 매칭(예: "내 관심 카테고리에 해당하는 새 공고 알람")이 필요해지면, FK 가 아닌 `PostContents.jobType` 과 `TargetJob.interestedJob` 의 **enum 값 비교**로 처리. 이 매칭이 두 enum 이 같은 값을 갖는 본질적 이유.
- **관심 직군 중복 거부 (`TargetJobServiceImpl.create` / `update`)**: 같은 회원이 같은 `TargetJobCategory` 로 중복 등록·변경 시 409 (`TARGET_JOB_DUPLICATE`). update 는 자기 row 의 카테고리 그대로 두는 건 통과 (변경 없을 때 existsBy 조회 생략). 회원별 독립이라 다른 회원은 같은 카테고리 등록 가능. `JobPostServiceImpl.create` 의 알람 dedup 안전망(`notifiedMemberIds.add`) 은 보존.
- **(보류) 두 enum 통합 검토**: 같은 원티드 taxonomy 에서 받는 값이므로 `TargetJobCategory` + `JobType` → 단일 `JobCategory` 로 통합하면 `==` 매칭이 가능해지고 sync 부담이 사라짐. 현재는 의도적으로 보류 — 원티드 API 의 런타임 동적 연동 구조(static enum vs `String` + 화이트리스트 vs 참조 테이블)까지 함께 결정한 뒤 진행. 보류 동안 두 enum 의 값 정렬은 사람이 수동으로 유지.

### 기술 스택 (techstack)
- 회원 ↔ `TechStackName` enum 단순 매핑. 본인 소유자만 삭제 (`TECH_STACK_UNAUTHORIZED`).
- **기술 스택 중복 거부 (`TechStackServiceImpl.add`)**: 같은 회원이 같은 `TechStackName` 으로 중복 등록 시 409 (`TECH_STACK_DUPLICATE`). 회원별 독립이라 다른 회원은 같은 이름 등록 가능. 레포의 `existsByMemberIdAndName` 활용.

### 커피챗 (coffeechat)
- 흐름: 요청자가 카카오 오픈채팅 링크 포함해 생성 (`PENDING`) → 수신자만 `ACCEPTED`/`REJECTED`로 상태 변경
- **생성 검증 (`CoffeeChatServiceImpl.create`)**: receiver 조회 직후 → (1) `requester.id == receiver.id` 면 400 (`COFFEE_CHAT_SELF_REQUEST`); (2) role 쌍이 (STUDENT, GRADUATE) 또는 (GRADUATE, STUDENT) 가 아니면 400 (`COFFEE_CHAT_ROLE_MISMATCH`) — UNKNOWN 양쪽 모두 거부. 도메인 의도 = 학생 ↔ 졸업생 멘토링 매칭.
- 알람 발행 규칙:
  - 생성(PENDING): 수신자에게 1건. content = `String.format("%s %s님이 커피챗을 요청했어요!", requester.department, requester.nickname)` 으로 동적 생성 (요청자 정보 노출). 헤더 텍스트는 프론트가 type 별 상수로 표시.
  - ACCEPTED: **요청자/수신자 양쪽**에 카카오 링크 포함 알람 2건 (content 정적)
  - REJECTED: 요청자에게만 1건 (content 정적)
- 삭제(취소)는 요청자만 가능
- **삭제 캐스케이드 (서비스 레벨)**: `CoffeeChatServiceImpl.delete` 에서 `CoffeeChatAlarm` 먼저 `deleteByCoffeeChatId` 로 정리 → `CoffeeChat` 삭제. `CoffeeChatAlarm.coffeeChat` 가 `nullable = false` FK 라 정리 없이는 MySQL FK constraint 위반.

### 통합 알람 (global/sse)
- **패키지**: 통합 알람 조회 계층(`AlarmController` / `AlarmService(Impl)` / `AlarmResponse` / `AlarmType` / `RelativeTimeFormatter`)은 `global/sse/` 에 SSE 실시간 푸시(`SseController` / `SseService(Impl)` / `SseEmitterRepository` / `SseTestController` / `SseSwagger`)·도메인 알람의 커밋 후 SSE 발행(`AlarmNotificationEvent` / `AlarmNotificationListener`)과 함께 배치. 별도 `domain/alarm/` 도메인은 없음 — 알람은 자체 엔티티 없이 4개 도메인의 알람 엔티티를 집계하는 cross-cutting 관심사라 `global` 에 둔다. 알람 엔티티(`AnswerAlarm` / `JobAlarm` / `RoadmapAlarm` / `CoffeeChatAlarm`) 자체는 각 도메인 패키지에 그대로 존속.
- `AlarmType` enum(`global/sse/AlarmType`)은 통합 알람 조회와 SSE 푸시 이벤트가 **공유** (단일 정의).
- 4종(`AnswerAlarm`, `JobAlarm`, `RoadmapAlarm`, `CoffeeChatAlarm`)을 합쳐 단일 `AlarmResponse` 리스트로 반환 (`GET /api/v1/alarms`)
- 정렬: `createdAt DESC`, null은 뒤로
- 읽음 처리는 Builder 패턴으로 새 인스턴스 만들어 저장 (불변 스타일)
- 각 타입별 소유자 검증 헬퍼 분리 (`verify*AlarmOwner`)
- **알람 row 저장 위치 = 각 도메인 `*ServiceImpl` 의 본체 save 직후, 같은 `@Transactional` 안**. 본체 저장 실패 시 알람 row 도 롤백되어 원자성 보장. SSE 실시간 푸시는 이벤트로 분리해 커밋 후 발행 (아래 "알람 SSE 발행" 참조).
  - `AnswerAlarm` — `AnswerServiceImpl.create()`, 질문 작성자에게 발행. 단, **본인이 본인 질문에 답변한 경우는 미발행** (자기 자신 알람 방지).
  - `RoadmapAlarm` — `RoadmapServiceImpl.create()`, **본인(생성자)** 에게 발행.
  - `JobAlarm` — `JobPostServiceImpl.create()`, `PostContents.jobType` 과 `TargetJob.interestedJob` 이 같은 학생들에게 N건 발행. **등록한 졸업생 본인 제외**, 같은 멤버가 동일 카테고리 중복 보유 시 1건만. enum 매칭은 `TargetJobCategory.valueOf(jobType.name())` 으로 (CLAUDE.md `interested_job` 의 값 매칭 방침과 정합).
  - `CoffeeChatAlarm` — `CoffeeChatServiceImpl.create()` / `updateStatus()`, PENDING 1건(수신자, content 는 요청자 학과·닉네임 포함 동적 생성), ACCEPTED 2건(요청자+수신자, 카카오링크), REJECTED 1건(요청자).
- **상대 시간 표시 (`AlarmResponse.relativeTime`)**: 응답 변환 시점에 `RelativeTimeFormatter.format(createdAt)` 으로 계산 ("방금 전 / N분 전 / N시간 전 / N일 전 / N개월 전 / N년 전"). 매 조회마다 재계산되어 시간 흐름에 따라 자연스럽게 업데이트됨. `createdAt` 도 함께 응답에 포함 (프론트 자체 포맷팅 여지 보존).
- **SSE 실시간 푸시**: `GET /api/v1/notifications/subscribe` 로 SSE 연결, `SseService.send(receiver, AlarmType, content)` 로 `notification` 이벤트 전송, 10초 주기 `ping` heartbeat. `SseTestController`(`POST /api/v1/notifications/test`) 수동 발송도 계속 동작.
- **알람 SSE 발행 (커밋 후)**: 도메인 4개 `*ServiceImpl`(`AnswerServiceImpl`/`JobPostServiceImpl`/`RoadmapServiceImpl`/`CoffeeChatServiceImpl`)이 알람 row `save` 직후 같은 `@Transactional` 안에서 `ApplicationEventPublisher.publishEvent(new AlarmNotificationEvent(receiver, type, content))` 발행 → `global/sse/AlarmNotificationListener` 가 `@TransactionalEventListener(AFTER_COMMIT)` 로 수신해 `SseService.send()` 호출. **본체 커밋 성공 후에만** 푸시되므로, 인라인 `send()` 와 달리 본체 롤백 시 DB 에 없는 알람을 클라이언트가 받는 "유령 알림"이 생기지 않음 (push 유실은 `GET /api/v1/alarms` 재조회로 self-heal — 실패 비대칭상 커밋 후 발행만 "DB = source of truth" 와 정합). 기존 SSE 코드(`SseService`/`SseServiceImpl`/`SseEmitterRepository`/`SseController`/`SseSwagger`/`SseTestController`)와 `AlarmType` 은 동결, 신규 파일은 `AlarmNotificationEvent`(record)·`AlarmNotificationListener` 2개뿐. 알람 content 리터럴은 각 서비스 `private static final` 상수로 추출(알람 row 빌더와 이벤트가 같은 상수 공유).

### Q&A
- `Question` 1:N `Answer`
- **답변 등록은 졸업생만** (`AnswerServiceImpl.create`): STUDENT/UNKNOWN 이 호출 시 403 (`ANSWER_NOT_GRADUATE`). 도메인 의도 = Q&A 답변은 멘토(졸업생) 가 학생 질문에 답하는 흐름. 수정/삭제는 기존 작성자 권한만 검증 (역할 제한 X).
- 좋아요: `QuestionLike`, `AnswerLike` (복합키 `AnswerLikeId`), toggle 방식. **본인이 작성한 글에는 좋아요 불가** (`QuestionServiceImpl.toggleLike` / `AnswerServiceImpl.toggleLike` 진입부에서 작성자 == 호출자 체크. 위반 시 400 `QUESTION_SELF_LIKE` / `ANSWER_SELF_LIKE`).
- **카운트·내 좋아요 여부**는 응답 DTO 에 포함 (`QuestionResponse.likeCount`/`likedByMe`/`answerCount`, `AnswerResponse.likeCount`/`likedByMe`). 매 GET 시 `countBy*` / `existsByMemberIdAnd*` 쿼리 호출 (캐시 컬럼 미도입). 모든 Q&A GET API (`getList`, `getOne`, `Answer.getList`) 가 `Member` 를 받아 `likedByMe` 채움.
- 좋아요 토글 응답: `LikeToggleResponse(liked, likeCount)` — 토글 후 새 상태·카운트 즉시 반환. 프론트 추가 GET 불필요.
- 작성자만 수정/삭제
- **삭제 캐스케이드 (서비스 레벨)**:
  - `Answer` 삭제 (`AnswerServiceImpl.delete`): `AnswerAlarm` → `AnswerLike` → `Answer` 순. 자식 FK 모두 `nullable = false`/복합 PK 라 정리 필수.
  - `Question` 삭제 (`QuestionServiceImpl.delete`): 손자(`AnswerAlarm`, `AnswerLike`) → `Answer` → `QuestionLike` → `Question` 순. 손자는 2-level 경로(`@Modifying @Query`) 로 일괄 삭제. `QuestionServiceImpl` 가 `AnswerRepository/AnswerAlarmRepository/AnswerLikeRepository` 를 직접 주입받는 도메인 간 약결합 허용 (각 답변별 소유자가 다를 수 있어 `AnswerService.delete` 위임은 부적합).

### 로드맵 (roadmap)
- 흐름: 데이터 파트가 화면 폼(학년/학점/전공/관심 직무/보유 역량/목표 기업) 입력값으로 AI 호출 → 결과 JSON만 백엔드가 저장. **AI 호출 자체는 백엔드 미포함** (Spring 측에 OpenAI/HTTP 클라이언트 의존성 없음).
- `Roadmap.content`: MySQL JSON 컬럼 (`@JdbcTypeCode(SqlTypes.JSON)`) — INSERT 시 `CAST(? AS JSON)` 수행되므로 valid JSON이 아니면 DB가 SQLException을 던짐.
- 등록(`POST /api/v1/roadmaps`) 시 `RoadmapServiceImpl.validateJsonContent()`로 사전 검증:
  - null / blank / JSON 파싱 실패 / JSON primitive(string·number·boolean·null) → `ROADMAP_INVALID_CONTENT` (HTTP 400)
  - **JSON object 또는 array만 통과** (구조만 검증; content 내부 키 스키마는 미강제 — 데이터 파트와 별도 합의)
  - DB SQLException으로 500이 떨어지지 않도록 컨트롤러→서비스 경계에서 막는 게 목적
- update API 미지원 (재생성 = 새 create + 기존 delete)
- 삭제는 소유자(member.id 일치)만 가능 (`ROADMAP_UNAUTHORIZED`)
- **삭제 캐스케이드 (서비스 레벨)**: `RoadmapServiceImpl.delete` 에서 `RoadmapAlarm` 먼저 `deleteByRoadmapId` 로 정리 → `Roadmap` 삭제. `RoadmapAlarm.roadmap` 이 `nullable = false` FK 라 정리 없이는 MySQL FK constraint 위반.
- Swagger 기본 예시값은 valid JSON object 문자열로 지정되어 있음 (`RoadmapSwagger.createRoadmap`의 `@ExampleObject`) — Try it out 즉시 200 통과

## 공통 패턴

- **부분 업데이트**: 엔티티를 Builder로 재구성, `null`인 필드는 기존 값 유지 (`request.x() != null ? request.x() : entity.getX()`)
- **권한 검증**: 작성자 ID 비교 후 도메인별 `*Handler` 예외 throw
- **응답 포맷**: 모든 컨트롤러는 `ApiResponse<T>` 래퍼 사용
- **에러 코드**: `ErrorStatus` enum에 모두 정의 (`AUTH401`, `POST403`, `ALARM404` …)
- **감사**: `BaseEntity` 상속 → `createdAt`/`updatedAt` 자동 (`@JpaAuditingConfig`)
- **Swagger 어노테이션**: 컨트롤러는 `*Swagger` 인터페이스 implements 패턴으로 분리

## 화면 ↔ 도메인 매핑 (Figma 화면 구성 기준)

| 화면 섹션 | 백엔드 도메인 |
|---|---|
| 인증/온보딩 | `global/auth` + `member` |
| 홈/알람 패널 | `global/sse` (4종 통합 조회 + SSE 실시간 푸시) |
| 폼 입력 (보라/초록) | `roadmap` / `techstack` / `interested_job` 등록·수정 |
| Q&A 스레드 (핑크) | `qna/question` + `qna/answer` |
| 구직 공고 (파랑) | `job_post` |
| 프로필 | `member` + `techstack` + `interested_job` |
| 커피챗 + 모달 | `coffeechat` (요청/수락/거절) |

### Figma 원본 PNG (lazy 로딩)

화면 구성·UI 플로우 관련 작업 시 아래 PNG를 `Read` 도구로 읽어 시각 정보를 확보한 뒤 작업한다. 위 매핑 표만으로 부족한 경우(픽셀 단위 배치, 색상, 컴포넌트 형태, 화면 전이 확인 등)에만 로드하여 토큰 소모를 줄인다.

- `0409.png` — Figma export (레포 루트, 모든 환경에서 접근 가능)

운영 규칙:
- 텍스트 매핑·도메인 규칙으로 답이 나오면 PNG를 굳이 읽지 않는다 (lazy)
- 화면 레이아웃/색상/컴포넌트 형태 질문, UI 명세 작성, 새 화면-도메인 매핑 추가 시에는 반드시 읽고 진행
- 새 Figma export가 추가되면 위 목록에 경로를 같이 등재할 것 (CLAUDE.md 자동 유지관리 규칙 적용)

## 테스트

- `src/test/java/.../EntityIntegrationTest.java` — 엔티티 관계 통합 검증 (649 lines)
- 도메인별 `*ControllerTest`, `*ServiceImplTest`
- `support/WithMockLoginMember` — `@LoginMember` 인증 mock 어노테이션
- 테스트 설정: `src/test/resources/application.properties`, `logback-test.xml`

## 작업 시 주의

- **하드코딩 절대 금지**: 문자열·숫자 리터럴은 `private static final` 상수 또는 enum/설정값으로 분리한다. 매직 넘버와 반복 문자열(이벤트명·URL·경로·메시지·TTL 등)을 코드에 직접 박지 말 것. 테스트 코드도 동일하게 적용하며, 공유 값은 `*TestConstants` 등 상수 클래스로 참조한다.
- 새 엔티티는 반드시 `BaseEntity` 상속
- 새 에러는 `ErrorStatus`에 정의 후 도메인 `*Handler`에서 throw
- 컨트롤러 응답은 `ApiResponse.onSuccess(...)` 통일
- JWT 화이트리스트 수정 시 `SecurityConfig`, `JwtAuthenticationFilter` 두 곳 모두 동기화
- 부분 업데이트 시 빌더 패턴 + null 체크 관례 유지

## CLAUDE.md 자동 유지관리 (필수)

작업으로 인해 아래 항목 중 하나라도 변경되면, **같은 작업/커밋 내에서 이 CLAUDE.md 파일도 함께 갱신해야 한다.** 코드 변경과 문서 변경을 분리하지 말 것.

갱신 트리거:
- **패키지/폴더 구조 변경**: 도메인 추가·삭제·이름 변경 → `## 패키지 구조` 트리 수정
- **새 도메인 규칙/플로우**: 새 엔티티 규칙, 상태머신, 알람 발행 규칙 등 → `## 핵심 도메인 규칙`에 항목 추가
- **공통 패턴 변경**: 응답 래퍼, 권한 검증, 빌더 패턴 등 관례 변경 → `## 공통 패턴` 수정
- **기술 스택/의존성 변경**: `build.gradle` 의존성 추가·제거·버전 변경 → `## 기술 스택` 수정
- **빌드/프로파일 설정 변경**: `application*.yml`, `build.gradle` 빌드 경로/프로파일 변경 → `## 빌드 특이사항` 수정
- **인증/JWT 정책 변경**: 화이트리스트, 토큰 정책, 클레임 변경 → `### 인증/JWT` 수정
- **테스트 인프라 변경**: 테스트 헬퍼/설정 추가·변경 → `## 테스트` 수정
- **에러 코드 체계 변경**: `ErrorStatus` 네이밍 규칙이나 prefix 변경 → `## 공통 패턴`의 에러 코드 항목 수정

운영 원칙:
- 변경이 사소하더라도(예: 도메인 1개 추가) 위 표/트리/규칙에 영향이 있으면 반드시 반영
- 변경점이 CLAUDE.md의 어느 섹션과도 맞지 않으면, 새 섹션을 만들어서라도 기록
- 단순 버그 수정·리팩터링이라 구조/규칙 변동이 없으면 갱신 불필요 (불필요한 diff 금지)
- 커밋 메시지에 `docs(CLAUDE.md): ...` 항목을 함께 남기거나, 코드 커밋 본문에 갱신 사실 명시
- 의심스러우면 갱신하는 쪽을 택할 것 — 문서 누락보다 과기록이 낫다

## 진행 예정 작업 (To-do)

> 사용자가 "to-do 리스트 수행" / "to-do 진행" / "TODO N 작업" 등 유사 명령을 내리면 아래 항목을 **즉시 코드 작성 → 테스트 보강 → CLAUDE.md 갱신 → 커밋/푸시 → PR 생성까지 일사천리로** 수행한다. 결정 사항은 이미 확정돼 있으니 다시 묻지 말고 명시된 디폴트로 진행할 것.
>
> 각 항목은 완료(머지) 후 이 섹션에서 삭제하고, 본문 도메인 섹션에 정식 규칙으로 통합 기록한다.

_(아래 TODO A · B 는 마이페이지 수정 화면의 "수정 완료" 흐름을 위한 bulk REPLACE 엔드포인트 도입. Figma 검토 결과 마이페이지가 TechStack/TargetJob CRUD 의 유일한 진입점으로 확인되어, 단건 endpoint 는 모두 제거하는 정공법으로 결정.)_

### 배경

마이페이지 수정 화면 (Figma `0409.png` 의 마이페이지 수정 영역) 의 흐름:
1. 사용자가 칩 추가/삭제 — **로컬 state 만 변함, 서버 호출 없음**
2. **"수정 완료"** → 프론트가 최종 리스트 전체를 서버로 전송, 서버가 본인 row 전체를 입력 리스트로 교체 (REPLACE)
3. **"취소"** → 프론트 로컬 state 만 버리고 마이페이지 보기 화면으로 복귀, **서버 호출 없음, DB 원본 그대로**

→ 단건 add/delete 패턴 (POST/DELETE 호출을 화면 동작마다 발생시키는) 이 아니라, **"수정 완료" 시점 단일 호출로 일괄 교체** 가 도메인 의도. 단건 endpoint 는 마이페이지 외에 다른 흐름이 없어 dead code 가 되므로 제거.

### TODO A: TechStack 단건 endpoint 제거 + bulk REPLACE PATCH 추가 (techstack 도메인 내 완결)

**디폴트 결정**:
- HTTP 메서드: `PATCH` (REPLACE 시맨틱이지만 사용자 명시 선택)
- 입력 정책:
  - `{"names": [...]}` → 리스트로 본인 row 전체 교체
  - `{"names": []}` → 본인 row 전부 삭제 (정상 응답, 0개 보유 허용)
  - `{"names": null}` 또는 body 누락 → 400 (`_BAD_REQUEST`)
- 서버 dedup: `Set<TechStackName>` 또는 `Stream.distinct()` 로 입력 리스트 내부 중복 자동 제거
- 단일 `@Transactional` — `deleteByMemberId` + `save` N회 원자성

**추가**:
- `domain/techstack/dto/request/ReplaceTechStackRequest.java` (record `(List<TechStackName> names)`)
- `TechStackService.replace(Member, ReplaceTechStackRequest)` 인터페이스 메서드
- `TechStackServiceImpl.replace` 구현
- `PATCH /api/v1/tech-stacks` 컨트롤러 핸들러 + `TechStackSwagger` `@Operation` 명세

**제거**:
- `TechStackService.add` / `delete` 인터페이스 메서드 + impl
- `POST /api/v1/tech-stacks` (단건 add) + `DELETE /api/v1/tech-stacks/{techStackId}` 컨트롤러 핸들러 + Swagger 명세
- `domain/techstack/dto/request/CreateTechStackRequest.java`
- `TechStackRepository.existsByMemberIdAndName`
- `ErrorStatus.TECH_STACK_DUPLICATE`
- `ErrorStatus.TECH_STACK_NOT_FOUND` (단건 delete 사라져 사용처 0)
- `ErrorStatus.TECH_STACK_UNAUTHORIZED` (동일)

**보존 (다른 도메인 의존)**:
- `TechStackRepository.deleteByMemberId` — `MemberServiceImpl.withdraw` 가 호출 (회원 hard delete 캐스케이드)
- `TechStackRepository.findByMemberId` — `getMyTechStacks` + REPLACE 응답에 사용
- `TechStackRepository.save` — REPLACE 의 INSERT
- `TechStack` 엔티티, `TechStackName` enum, `TechStackResponse`, `TechStackHandler`

**테스트 갱신** (`TechStackServiceImplTest`, `TechStackControllerTest` 전면 재작성):
- 기존 `add_*`, `delete_*` 케이스 모두 삭제
- 신규 `replace_*` 시나리오:
  - 정상 교체 (deleteByMemberId 호출 후 save N회 검증, InOrder)
  - 빈 리스트 → 본인 row 전부 삭제, save 미호출
  - 입력에 중복 enum 섞이면 dedup 후 1건만 저장
  - null 리스트 → 400
  - 다른 회원 row 격리 (deleteByMemberId 가 본인 id 만 호출)
- `getMyTechStacks` 케이스는 보존
- 컨트롤러 슬라이스: `PATCH /api/v1/tech-stacks` 응답 (`List<TechStackResponse>`) 검증

**CLAUDE.md 갱신**:
- `### 기술 스택 (techstack)` 섹션:
  - "단건 등록 / 단건 삭제 / 단건 중복 거부" 규칙 제거
  - "**REPLACE 단일 endpoint**: `PATCH /api/v1/tech-stacks` 로 본인 리스트 전체 교체. 단건 add/delete endpoint 미지원 (마이페이지가 유일 진입점). 입력 리스트 내부 중복은 서버에서 `Set` dedup. 빈 리스트 = 전부 삭제 의미. null = 400." 으로 갱신
- `### 회원 (Member)` 의 hard delete 위임 순서: 변경 없음 (`techStackRepository.deleteByMemberId` 보존)

### TODO B: TargetJob 단건 endpoint 제거 + bulk REPLACE PATCH 추가 (interested_job 도메인 내 완결)

**디폴트 결정**: TODO A 와 동일 (PATCH 메서드, dedup, null 거부, 빈 리스트 허용, 단일 트랜잭션)

**추가**:
- `domain/interested_job/dto/request/ReplaceTargetJobRequest.java` (record `(List<TargetJobCategory> categories)`)
- `TargetJobService.replace(Member, ReplaceTargetJobRequest)` 인터페이스 메서드
- `TargetJobServiceImpl.replace` 구현
- `PATCH /api/v1/target-jobs` 컨트롤러 핸들러 + `TargetJobSwagger` `@Operation` 명세

**제거**:
- `TargetJobService.create` / `update` / `delete` 인터페이스 메서드 + impl
- `POST /api/v1/target-jobs` / `PATCH /api/v1/target-jobs/{targetJobId}` (단건 카테고리 변경) / `DELETE /api/v1/target-jobs/{targetJobId}` + Swagger 명세
- `domain/interested_job/dto/request/CreateTargetJobRequest.java`
- `domain/interested_job/dto/request/UpdateTargetJobRequest.java`
- `TargetJobRepository.existsByMemberIdAndInterestedJob`
- `TargetJobRepository.findByMemberIdAndInterestedJob` (이미 사용처 0건인 dead code — 함께 제거)
- `ErrorStatus.TARGET_JOB_DUPLICATE`
- `ErrorStatus.TARGET_JOB_NOT_FOUND`
- `ErrorStatus.TARGET_JOB_UNAUTHORIZED`

**보존 (다른 도메인 의존)**:
- `TargetJobRepository.deleteByMemberId` — `MemberServiceImpl.withdraw`
- `TargetJobRepository.findByMemberId` — `getMyTargetJobs` + REPLACE 응답
- **`TargetJobRepository.findByInterestedJob` — `JobPostServiceImpl.create` / `update` 의 알람 매칭 핵심, 반드시 보존**
- `TargetJobRepository.save` — REPLACE 의 INSERT
- `TargetJob` 엔티티, `TargetJobCategory` enum, `TargetJobResponse`, `TargetJobHandler`

**알람 정책**:
- REPLACE 자체는 `JobAlarm` 발행 안 함 (현재 단건 흐름과 동일 정책 유지)
- 새 공고 등록 시점에 `JobPostServiceImpl.create` 가 갱신된 `target_job` 매칭으로 발행
- "REPLACE 직후 본인 카테고리 매칭 기존 공고 일괄 알람 생성" 같은 기능은 이번 TODO 범위 외 — 필요하면 별도 TODO 분리

**테스트 갱신** (`TargetJobServiceImplTest`, `TargetJobControllerTest` 전면 재작성):
- 기존 `create_*`, `update_*`, `delete_*` 케이스 모두 삭제
- 신규 `replace_*` 시나리오 (TODO A 와 동일 5종)
- `getMyTargetJobs` 케이스는 보존

**CLAUDE.md 갱신**:
- `### 관심 직군 (interested_job)` 섹션:
  - "단건 등록 / 단건 변경 / 단건 삭제 / 단건 중복 거부" 규칙 제거
  - "**REPLACE 단일 endpoint**: `PATCH /api/v1/target-jobs` ..." (TODO A 와 동일 패턴) 로 갱신
  - 보류 사항 ("두 enum 통합 검토") 은 그대로 보존
- `### 회원 (Member)` 의 hard delete 위임 순서: 변경 없음

### TODO C: 회원가입 이메일 도메인 검증 미적용 (PR #22 후속, auth 도메인)

**문제**: `SignupRequest.email` 에 `@Pattern` (`^[a-zA-Z0-9._%+\-]+@mju\.ac\.kr$`) 이 선언돼 있으나, `AuthController.signup` (`AuthController.java:24`) 과 `AuthSwagger.signup` (`AuthSwagger.java:26`) 의 `SignupRequest` 파라미터에 `@Valid` 가 없어 검증이 트리거되지 않음. 같은 컨트롤러의 `sendCode`/`verifyCode` 는 `@Valid` 보유. → 현재 `@mju.ac.kr` 제한이 가입 경로에서 무력화된 상태.

**디폴트 결정**: `signup` 의 `SignupRequest` 파라미터에 `@Valid` 추가. 인터페이스(`AuthSwagger`)·구현(`AuthController`) 양쪽 동기화. 검증 실패는 기존 `MethodArgumentNotValidException` 핸들러가 `_BAD_REQUEST` 로 매핑 (회원 도메인 소셜 링크 검증과 동일 패턴).

**출처**: PR #22 (`ddd4004`) 가 `@Pattern` 만 추가하고 `@Valid` 를 누락.

### TODO D: 이메일 인증 결과가 회원가입에 미반영 (PR #22 후속, auth 도메인)

**문제**: `AuthServiceImpl.signup` (`AuthServiceImpl.java:34-62`) 이 `EmailService`/`RedisUtil` 을 참조하지 않음. `POST /api/v1/auth/verify-code` 통과 여부와 무관하게 가입이 가능 — 인증 안 한 이메일로도 회원가입됨. `EmailServiceImpl.verifyCode` 는 코드 일치 시 Redis 키를 삭제만 하므로 "인증 완료" 상태가 어디에도 남지 않음.

**미확정 (사용자 결정 필요)**: signup 이 인증 통과를 확인하는 방식. 후보 — (a) `verifyCode` 성공 시 `verified:{email}` 플래그를 Redis 에 단기 저장 → `signup` 진입부에서 존재 확인 후 소비, (b) `verifyCode` 가 단기 인증 토큰을 발급해 `signup` 요청에 포함. 방식 확정 전까지 코드 변경 보류.

**출처**: PR #22.

### TODO E: 알람 발송을 SSE 푸시 중심으로 재구성 (기존 알람 코드 ↔ SSE 정합, global/sse)

**확정 아키텍처**: 알람은 SSE 로 실시간 전송한다. 단 영속화와 SSE 는 경쟁이 아니라 **보완 관계** — 다음 구조로 확정:
- **영속화(DB) = 원천 데이터(source of truth)** — `*AlarmRepository.save()` 로 알람 row 저장 유지. 이력·읽음 상태(`isRead`)·상세 조회·정렬의 근거.
- **SSE = 실시간 전송 채널** — 발송 시 수신자에게 즉시 push, 반복 폴링 제거.
- 알람 발생 시 **DB 저장 + SSE 전송 양쪽 모두 수행**. `GET /api/v1/alarms` 조회 API 는 유지 — 초기 로드·재접속·이력 회수용 (SSE 는 폴링을 없앨 뿐 조회 API 를 대체하지 않음).
- 작업 범위 = 이 구조에 맞춰 기존 알람 발행 코드(6개 발송 지점)를 재구성. 단순 `send()` 한 줄 추가가 아니라 발행 지점을 SSE 에 정합하게 정리하는 것 (구체안은 미확정 참조).

**배경 — 현재 알람 발송 방식 (pull 모델, SSE 와 전달 방향 반대)**:
- 전용 알람 도메인 없음. 알람 엔티티 4종(`AnswerAlarm`/`JobAlarm`/`RoadmapAlarm`/`CoffeeChatAlarm`)은 각 도메인 패키지에 존재.
- "발송" = 본체 `save()` 직후 같은 `@Transactional` 안에서 `*AlarmRepository.save()` 로 알람 row INSERT. 이벤트·AOP·push 전혀 없음.
- 발송 지점 6곳:
  - `AnswerServiceImpl.create` (`:57`) — `AnswerAlarm` 1건, 질문 작성자 (self-답변 시 생략)
  - `JobPostServiceImpl.create` (`:83`) — `JobAlarm` N건, jobType 매칭 학생
  - `JobPostServiceImpl.update` → `dispatchJobTypeChangeAlarms` (`:189`/`:199`) — `JobAlarm`, jobType 변경 영향 학생
  - `RoadmapServiceImpl.create` (`:42`) — `RoadmapAlarm` 1건, 생성자 본인
  - `CoffeeChatServiceImpl.create` (`:73`) — `CoffeeChatAlarm` 1건, 수신자
  - `CoffeeChatServiceImpl.updateStatus` (`:127`/`:134`/`:143`) — ACCEPTED 2건 / REJECTED 1건
- 수신자는 `GET /api/v1/alarms` 폴링으로 회수 (`AlarmServiceImpl.getMyAlarms` 가 4개 알람 테이블 집계).
- `SseService.send(Member, AlarmType, String)` 은 구현돼 있으나 실 호출처가 `SseTestController` 수동 발송뿐 — 도메인 알람과 미연결.

**제약 (절대 준수)**:
- **기존 SSE 푸시 코드는 수정 금지** — `global/sse` 의 `SseController`/`SseService`/`SseServiceImpl`/`SseEmitterRepository`/`SseSwagger`/`SseTestController`. `SseService.send()` 시그니처·동작을 그대로 둔 채 알람 코드가 거기에 맞춘다.
- **`AlarmType` enum 완전 동결** — frozen `SseService.send()` 시그니처·`SseServiceImpl`(`type.name()`)·`SseTestController`(`defaultValue="ANSWER"`) 가 의존하므로 동결 대상에 포함. 기존 4상수·이름·위치·타입은 물론 **상수 추가·필드/메서드 추가까지 포함해 TODO E 범위 내 일절 수정 금지.** 새 알람 타입이 필요하면 별도 TODO.
- 그 외 기존 알람 코드(도메인 4곳 발행 로직, 알람 엔티티/레포, `global/sse` 의 `Alarm*` 조회 계층 — 단 `AlarmType` 제외)는 자유롭게 변경 가능.

**확정 완료 (① · ②) — 본 PR 에서 TODO E 구현**: 두 항목 모두 확정(아래 `확정 결정 로그`). 코드 구현은 이 PR 에 포함되며, 머지 후 이 TODO E 항목을 삭제하고 규칙은 `### 통합 알람 (global/sse)` 도메인 섹션에 통합한다.

**확정 결정 로그**:
- `AlarmType` 변경 범위 → **완전 동결** (frozen SSE 코드 의존, 상수 추가 포함 일절 수정 금지).
- 영속화 유지 여부 → **유지** — DB = source of truth. SSE 는 전송 수단이라 저장을 대체 못 함 (이력·읽음·상세는 저장 필요 기능). DB 영속화 + SSE 실시간 전송 병행이 효율적 정답이며, 영속화 제거는 효율 개선이 아니라 기능 상실.
- ① 트랜잭션 경계 → **커밋 이후 발행** (`@TransactionalEventListener(phase = AFTER_COMMIT)`). 알람 row `*AlarmRepository.save()` 는 본체 `@Transactional` 안에 그대로 둬 본체와 원자적으로 커밋, SSE `send()` 만 커밋 성공 후 실행. 이유: `send()` 는 롤백 불가한 네트워크 전송이라 인라인 발행 시 커밋이 실패하면 DB 에 없는 알람을 클라이언트가 수신하는 "유령 알림"이 발생. 커밋 후 발행은 push 유실 시 `GET /api/v1/alarms` 재조회로 self-heal 되지만 그 반대(유령 알림)는 불가 — 실패 비대칭상 커밋 후 발행만 "DB = source of truth" 와 정합. 부수 효과: 네트워크 I/O 가 트랜잭션 밖으로 빠져 DB 커넥션 점유 시간 단축, 발행 지점이 `SseService` 와 디커플링.
- ② 재구성 구체안 → **이벤트 발행 + 단일 AFTER_COMMIT 리스너**. 도메인 4개 `*ServiceImpl` 이 알람 row save 직후 `ApplicationEventPublisher.publishEvent(new AlarmNotificationEvent(receiver, type, content))` 발행, `global/sse/AlarmNotificationListener` 가 `@TransactionalEventListener(AFTER_COMMIT)` 로 수신해 `SseService.send()` 호출. 신규 파일은 `AlarmNotificationEvent`(record)·`AlarmNotificationListener` 2개뿐 — `global/sse` 의 기존 SSE 코드·`AlarmType` 0줄 수정. 디스패처/헬퍼 클래스 없이 발행 지점마다 `publishEvent` 인라인(불필요한 간접화 회피). 알람 content 리터럴은 각 서비스 `private static final` 상수로 추출(알람 row 빌더와 이벤트가 같은 상수 공유, 하드코딩 제거). `@Async` 미적용(커밋 후 동기 발행) — 비동기화는 별도 작업.

**출처**: PR #22 (`ddd4004`) 가 SSE 인프라만 추가하고 도메인 연결을 누락. 본 대화에서 "SSE 1차 채널 + 기존 알람 코드 재구성, SSE 코드 동결" 로 방향 재확정 (구 TODO E 재작성).

### 공통 진행 정책

| 항목 | 결정 |
|---|---|
| HTTP 메서드 | `PATCH` (REPLACE 시맨틱이지만 사용자 명시) |
| TODO A · B 머지 단위 | 단일 PR 권장 ("마이페이지 수정 화면용 REPLACE 도입" 단일 의도). 분리 PR 도 가능 |
| 다른 도메인 영향 | 0 — `MemberServiceImpl.withdraw` 와 `JobPostServiceImpl` 의 의존 메서드는 모두 보존 |
| 머지 충돌 위험 | 낮음 — 본인 도메인 내부 변경. 동시 작업 중인 다른 브랜치 없음 (검토 시점 기준) |
| 검증 | 머지 전 `gradlew compileJava + compileTestJava` 통과 필수. Swagger UI 시나리오: 정상 / 빈 리스트 / 중복 입력 / null 거부 / 다른 회원 격리 |

### 11개 작업자 노트 (TODO #1~#11 머지 완료 후 보존되는 일반 가이드)

- **브랜치 정책**: 각 TODO 를 **개별 브랜치 + 개별 PR** 로 처리하는 것을 기본으로 한다. 영향 범위가 큰 TODO 는 단독 PR 필수. 같은 도메인 내 작은 변경은 묶어서 1개 PR 도 허용 (작업자 판단). 사용자가 "TODO N 작업" 으로 단일 항목 지목 시 그 항목만 단독 PR.
- **공통 커밋 메시지 컨벤션**: `feat(<도메인>): ...` / `fix(<도메인>): ...` / `refactor(<도메인>): ...` / `docs(CLAUDE.md): ...` prefix. 마지막 줄에 항상 `https://claude.ai/code/session_...` 포함.
- **Swagger 검증**: 가능하면 PR 본문 Test plan 에 Swagger 시나리오 체크박스 포함.
- **테스트 실패 시**: `--no-verify` 등으로 우회 금지. 원인 분석 후 수정.

