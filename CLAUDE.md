# ddingconnect

명지대 캡스톤 디자인 — 졸업생/재학생 커뮤니티 플랫폼 백엔드.
원본: `ddingconnect-backend` PR #15 스냅샷 (Merge PR #1, 2026-05-11).

## 작업 레포 범위 규칙 (필수)

- **기본 작업 레포는 이 레포(`somlon/cluade_clone`) 단 하나다.** 사용자의 별도 지시가 없는 한 모든 코드 변경·브랜치 생성·커밋·푸시·PR 은 `cluade_clone` 에서만 수행한다.
- **`mju-capstone-4/ddingconnect-backend` 레포는 사용자가 해당 요청에서 "ddingconnect-backend 에서 작업하라" 고 명시적으로 지시한 경우에만** 쓰기 작업(파일 수정·브랜치 생성·커밋·푸시·PR)을 한다. 명시적 지시가 없으면 `ddingconnect-backend` 에 어떤 변경도 가하지 않는다.
- 비교·참고 목적의 **읽기 조회**(파일 내용 확인 등)는 `ddingconnect-backend` 에 대해서도 허용된다 — 금지 대상은 쓰기 작업뿐이다.
- 작업 대상 레포가 불확실하면 `ddingconnect-backend` 를 건드리지 말고 사용자에게 먼저 확인한다.

## 기술 스택

- Spring Boot 3.5.13 / Java 17 (toolchain)
- Spring Data JPA + MySQL (`mysql-connector-j`)
- Spring Security + JWT (`jjwt 0.12.6`, HS512)
- Swagger (`springdoc-openapi-starter-webmvc-ui 2.7.0`)
- Lombok, Validation, `spring-dotenv` (`.env`)
- 테스트: JUnit 5, `spring-security-test`

## 빌드 특이사항

- `build.gradle` — build 디렉터리는 `-PbuildDir=...` 또는 `gradle.properties` 의 `buildDir` 로 외부화한다. 미지정 시 Gradle 기본값(`build/`)을 사용하므로 리눅스/맥/CI 에서 그대로 빌드된다. OneDrive 동기화 잠금 회피가 필요한 Windows 환경만 `buildDir` 를 외부 경로(예: `C:/gradle-builds/...`)로 지정한다.
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
    ├── alarm/           # 통합 알람 조회(4종 목록/상세/읽음) + 상대시간 포맷
    ├── auth/            # 회원가입/로그인, JWT 필터, @LoginMember
    ├── common/          # BaseEntity (createdAt/updatedAt), SuccessMessage (성공 메시지 상수)
    ├── config/          # Security, Swagger, JpaAuditing, WebMvc, Mail, Sse
    ├── jwt/             # JwtUtil(Impl)
    ├── redis/           # RedisUtil (StringRedisTemplate 래퍼)
    ├── response/        # ApiResponse, ErrorStatus, ExceptionAdvice
    └── sse/             # SSE 실시간 푸시 + 알람 이벤트 리스너
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

### 마이페이지 (member)
- **`GET /api/v1/members/mypage`** — 마이페이지 화면을 1회 호출로 렌더링하기 위한 통합 조회. 컨트롤러는 `MemberController`, 서비스는 `MyPageService(Impl)`.
- **애그리게이터 패턴**: `MyPageServiceImpl` 은 레포지토리를 직접 주입하지 않고, 항목별 도메인 서비스(`MemberService`/`TechStackService`/`TargetJobService`/`CoffeeChatService`/`RoadmapService`/`QuestionService`/`JobPostService`)를 in-process 로 호출해 한 응답으로 조합한다. 모놀리식 내 HTTP 자기호출이 아니라 서비스 계층 직접 호출.
- 응답 `MyPageResponse` = `profile`(`MemberResponse` 재사용) + `activity`(활동 통계) + `techStacks` + `targetJobs`(재학생 항목) + `jobPosts`(졸업생 항목).
- **활동 통계 `ActivityStats`**: 커피챗 수 = 본인이 요청자/수신자로 참여한 `ACCEPTED` 상태 합산(`CoffeeChatService.countMyAcceptedCoffeeChats`), 로드맵 수 = 본인 생성(`RoadmapService.countMyRoadmaps`), 질문 수 = 본인 작성(`QuestionService.countMyQuestions`).
- `targetJobs` 는 `STUDENT`, `jobPosts` 는 `GRADUATE` 역할에서만 채워지고 그 외 역할에선 빈 리스트. `jobPosts` 는 `JobPostService.getMyJobPosts` (졸업생 매핑이 없으면 빈 리스트).
- 마이페이지용 항목별 조회 메서드(`countMy*`, `getMyJobPosts`)는 각 도메인 서비스에 추가돼 향후 단건 화면에서도 재사용 가능. 별도 공개 엔드포인트는 아직 두지 않고 마이페이지 1개만 노출.

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
- **선호 언어는 다중 입력** (`PostContents.preferredLanguages`, `List<String>`): `@ElementCollection` + `@CollectionTable(name = "post_contents_preferred_language", joinColumns = post_contents_id)` 로 별도 컬렉션 테이블에 매핑 — 공고 카드에서 언어별 칩으로 표시. `Create/UpdateJobPostRequest`·`JobPostResponse` 모두 `List<String>`. `update` 는 null-coalescing(요청 리스트가 null 이면 기존값 유지). 컬렉션 테이블 행은 `PostContents` 삭제 시 Hibernate 가 자동 정리하므로 서비스 레벨 수동 캐스케이드 대상 아님.
- **삭제 캐스케이드 (서비스 레벨)**: `PostContents` 삭제 시 자식 행을 **서비스 코드에서 명시적으로 먼저 삭제** (`JobPostServiceImpl.delete`). 순서 = `JobAlarm` → `GraduateJobPost` → `PostContents`. 이유: `GraduateJobPost.postContents`, `JobAlarm.postContents` 가 `nullable = false` FK 인데 JPA cascade 설정이 없어, 부모만 지우면 영속성 컨텍스트의 자식이 transient 부모를 참조하게 되어 `TransientObjectException` 으로 터짐. 알람도 함께 hard delete 정책 (사용자의 알람 이력은 손실되지만, 공고가 사라진 상태의 dangling 알람을 막음).
- **update 시 jobType 변경 알람 디스패치 (`JobPostServiceImpl.update`)**: `oldJobType != newJobType` 일 때, 이 공고의 기존 `JobAlarm` 수신자 (= prev) 와 새 `jobType` 매칭 학생 (= curr) 의 차집합을 계산해 두 종류 알람을 발행. **Removed = prev − curr** → `"관심 직군에서 벗어난 공고로 변경되었습니다."`, **Added = curr − prev** → `"관심 직무에 새로운 공고가 등록되었습니다."`. 기존 알람 row 는 보존(삭제·갱신 X). 등록 졸업생 본인 항상 제외. prev 집합 정의는 이 공고(`postContentsId`)에 연결된 `JobAlarm` row 만 본다 (다른 알람 타입/다른 공고는 무관).

### 관심 직군 (interested_job)
- 학생 마이페이지의 **관심 직군 칩** — 회원 ↔ `TargetJobCategory` enum 단순 매핑. `PostContents`(구직 공고)와는 FK 관계 **없음**.
- 입력은 카테고리(`TargetJobCategory`) 값뿐. 공고 ID 같은 FK는 받지 않음.
- `TargetJob` 엔티티는 `member_id`(NOT NULL FK) + `Interested_Job`(enum) + `key2`(미사용, ERD 잔재) 로 구성.
- `TargetJobCategory` 와 `JobType` 은 같은 원티드 직군 태그 소스에서 11개 동일 값을 받지만 **타입 분리**. 두 enum 의 값 정렬은 사람이 직접 유지해야 함.
- 공고 ↔ 관심 직군 매칭(예: "내 관심 카테고리에 해당하는 새 공고 알람")이 필요해지면, FK 가 아닌 `PostContents.jobType` 과 `TargetJob.interestedJob` 의 **enum 값 비교**로 처리. 이 매칭이 두 enum 이 같은 값을 갖는 본질적 이유.
- **REPLACE 단일 endpoint (`TargetJobServiceImpl.replace`)**: `PATCH /api/v1/target-jobs` 로 본인 관심 직군 리스트 전체를 요청 리스트로 교체. 단일 `@Transactional` 안에서 `deleteByMemberId` 후 `save` N회. 입력 리스트 내부 중복은 서버에서 `Stream.distinct()` 로 제거. 빈 리스트 = 본인 row 전부 삭제(정상 200), `categories` 가 null = 400 (`_BAD_REQUEST`, `TargetJobHandler`). 응답은 교체 후 `List<TargetJobResponse>`. 마이페이지 수정 화면이 유일 진입점이라 단건 add/update/delete endpoint 는 두지 않는다. REPLACE 자체는 `JobAlarm` 을 발행하지 않으며, 공고↔관심직군 알람 매칭은 새 공고 등록 시점의 `JobPostServiceImpl.create` 가 담당.
- **(보류) 두 enum 통합 검토**: 같은 원티드 taxonomy 에서 받는 값이므로 `TargetJobCategory` + `JobType` → 단일 `JobCategory` 로 통합하면 `==` 매칭이 가능해지고 sync 부담이 사라짐. 현재는 의도적으로 보류 — 원티드 API 의 런타임 동적 연동 구조(static enum vs `String` + 화이트리스트 vs 참조 테이블)까지 함께 결정한 뒤 진행. 보류 동안 두 enum 의 값 정렬은 사람이 수동으로 유지.

### 기술 스택 (techstack)
- 회원 ↔ `TechStackName` enum 단순 매핑. 마이페이지 수정 화면이 유일 진입점이라 단건 add/delete endpoint 는 두지 않는다.
- **REPLACE 단일 endpoint (`TechStackServiceImpl.replace`)**: `PATCH /api/v1/tech-stacks` 로 본인 기술 스택 리스트 전체를 요청 리스트로 교체. 단일 `@Transactional` 안에서 `deleteByMemberId` 후 `save` N회. 입력 리스트 내부 중복은 서버에서 `Stream.distinct()` 로 제거. 빈 리스트 = 본인 row 전부 삭제(정상 200), `names` 가 null = 400 (`_BAD_REQUEST`, `TechStackHandler`). 응답은 교체 후 `List<TechStackResponse>`.

### 커피챗 (coffeechat)
- 흐름: 요청자가 카카오 오픈채팅 링크 포함해 생성 (`PENDING`) → 수신자만 `ACCEPTED`/`REJECTED`로 상태 변경
- **생성 검증 (`CoffeeChatServiceImpl.create`)**: receiver 조회 직후 → (1) `requester.id == receiver.id` 면 400 (`COFFEE_CHAT_SELF_REQUEST`); (2) role 쌍이 (STUDENT, GRADUATE) 또는 (GRADUATE, STUDENT) 가 아니면 400 (`COFFEE_CHAT_ROLE_MISMATCH`) — UNKNOWN 양쪽 모두 거부; (3) **중복 신청 방지** — requester→receiver 로 `PENDING`/`ACCEPTED` 상태 커피챗이 있으면 400 (`COFFEE_CHAT_ALREADY_REQUESTED`), `REJECTED` 만 있으면 재신청 허용; (4) **재요청 쿨다운** — (3) 통과 후에도 requester→receiver 의 가장 최근 `CoffeeChat.createdAt` 이 24시간(`RE_REQUEST_COOLDOWN` = `Duration.ofHours(24)` 상수) 이내면 429 (`COFFEE_CHAT_REQUEST_TOO_SOON`). 도메인 의도 = 학생 ↔ 졸업생 멘토링 매칭 + 거절 직후 빠른 반복 재신청 차단. (3)·(4) 는 각각 `CoffeeChatRepository.existsByRequesterIdAndReceiverIdAndStatusIn` · `existsByRequesterIdAndReceiverIdAndCreatedAtAfter` 파생 쿼리로 검사.
- 알람 발행 규칙:
  - 생성(PENDING): 수신자에게 1건. content = `String.format("%s %s님이 커피챗을 요청했어요!", requester.department, requester.nickname)` 으로 동적 생성 (요청자 정보 노출). 헤더 텍스트는 프론트가 type 별 상수로 표시.
  - ACCEPTED: **요청자/수신자 양쪽**에 카카오 링크 포함 알람 2건 (content 정적)
  - REJECTED: 요청자에게만 1건 (content 정적)
- 삭제(취소)는 요청자만 가능
- **삭제 캐스케이드 (서비스 레벨)**: `CoffeeChatServiceImpl.delete` 에서 `CoffeeChatAlarm` 먼저 `deleteByCoffeeChatId` 로 정리 → `CoffeeChat` 삭제. `CoffeeChatAlarm.coffeeChat` 가 `nullable = false` FK 라 정리 없이는 MySQL FK constraint 위반.
- **커피챗 매칭 플로우 (`CoffeeChatMatchingController` / `CoffeeChatMatchingService(Impl)`)**: Figma 매칭 화면(정보 입력 → 결과 리스트 → 상대 상세)의 백엔드. 신규 컨트롤러는 base `/api/v1/coffeechat` 로 기존 `CoffeeChatController` 와 메서드 경로가 달라 충돌 없음. 엔드포인트 3개:
  - `POST /api/v1/coffeechat/matching` — 매칭 폼 6필드(`MatchingRequest`) 수신 → 알고리즘 호출 → 후보 카드 리스트(`MatchedCandidateResponse`). 후보 없으면 빈 리스트.
  - `GET /api/v1/coffeechat/matching/{memberId}` — 매칭 상대 상세(`MatchedCandidateDetailResponse`). "내 매칭 결과였는지"는 검증하지 않음.
  - `GET /api/v1/coffeechat/my-activity` — 로그인 회원이 신청자이고 `status=ACCEPTED` 인 커피챗 상대 목록(상세 DTO). 없으면 빈 리스트.
- **매칭은 무상태 pass-through**: 매칭 입력값·결과는 저장하지 않으며 매칭 이력 엔티티가 없다. 알고리즘이 회원 ID 리스트만 반환(점수 없음)하므로 `CoffeeChat.jobScore/ability/goal` 은 계속 미사용 컬럼.
- **매칭 알고리즘 연동 (`MatchingAlgorithmClient` / `MatchingAlgorithmClientImpl`)**: 백엔드가 폼 6필드 + 신청자 ID 를 외부 유사도 알고리즘(타 파트)에 `RestClient` 로 전달하고 후보 회원 ID 리스트를 받는다. base URL 은 설정값 `matching.algorithm.base-url`(env `MATCHING_ALGORITHM_BASE_URL` 오버라이드, 기본 `http://localhost:8000`). 호출 실패·타임아웃·빈 응답은 모두 502 (`MATCHING_ALGORITHM_FAILED`). **타 파트 협의 미확정**: 알고리즘 엔드포인트 경로(현재 `/match`)·요청/응답 JSON 스키마 — `MatchingAlgorithmClientImpl` 내부 `record`(`AlgorithmMatchRequest`/`AlgorithmMatchResponse`) 매핑만 확정 후 교체하면 된다.
- **후보 프로필 조립 (`CandidateProfileAssembler`)**: `memberId` → 카드/상세 DTO 조립(매칭 결과·상대 상세·나의 활동 공통 로직). 회원 공통 정보 + 역할별 부가 정보(GRADUATE = 입학연도·회사·경력, 상세는 명함·공고 추가 / STUDENT = 학년)를 모은다. 입학연도는 `Member.studentNumber` 의 3·4번째 자리(0-index 2~3) 파생(별도 컬럼 없음, 4자리 미만이면 null), `region` 은 소스 미확정으로 항상 null. 직군은 `TargetJobRepository.findByMemberId`, 기술스택은 `TechStackRepository.findByMemberId` 로 전체 리스트 조회(0개면 빈 리스트). 타 도메인 레포 직접 주입은 조회 전용 조립 목적이며 QnA 선례상 허용. 응답 DTO `MatchedCandidateResponse`(카드)/`MatchedCandidateDetailResponse`(상세)는 한 record 에 역할별 nullable 필드를 두는 `MemberResponse` 패턴 — 상세에 "나와의 커피챗 상태"는 넣지 않는다(수락/거절은 기존 커피챗 알람으로 전달).
- **(현황) 카카오 오픈채팅 링크 생성 API 미구현**: 카카오 오픈채팅 링크를 발급/생성하는 API 는 아직 없다 (카카오 연동 미구현). 현재는 `CreateCoffeeChatRequest.kakaoOpenChatLink` (`String`, 검증 없음) 로 요청자가 링크를 직접 전달하면 `CoffeeChat.kakaoOpenChatLink` (`varchar(255)`) 에 그대로 저장만 한다. 추후 다른 방식으로 링크를 연결할 예정이며 연동 방식은 미정 — 커피챗 매칭 플로우의 신청 단계(`커피챗 신청하기`)에서 링크 입력/획득 경로를 함께 확정해야 한다.

### 통합 알람 (global/alarm + global/sse)
- **패키지**: 통합 알람 조회 계층(`AlarmController` / `AlarmSwagger` / `AlarmService(Impl)` / `AlarmResponse` / `RelativeTimeFormatter`)과 알람 종류 enum(`AlarmType`)은 `global/alarm/` 에 둔다. SSE 실시간 푸시 채널(`SseController` / `SseService(Impl)` / `SseEmitterRepository` / `SseTestController` / `SseSwagger`)과 도메인 알람의 커밋 후 SSE 발행 브리지(`AlarmNotificationEvent` / `AlarmNotificationListener`)는 `global/sse/` 에 둔다. 알람 조회는 SSE 와 무관한 순수 REST/JPA 기능이라 `sse` 하위가 아닌 형제 패키지로 분리했다. 의존 방향은 `global/sse → global/alarm` 단방향(`SseService` / `AlarmNotificationEvent` 가 `AlarmType` 참조) — `global/alarm` 은 `global/sse` 를 참조하지 않아 패키지 순환이 없다. 별도 `domain/alarm/` 도메인은 없음 — 알람은 자체 엔티티 없이 4개 도메인의 알람 엔티티를 집계하는 cross-cutting 관심사라 `global` 에 둔다. 알람 엔티티(`AnswerAlarm` / `JobAlarm` / `RoadmapAlarm` / `CoffeeChatAlarm`) 자체는 각 도메인 패키지에 그대로 존속.
- `AlarmType` enum(`global/alarm/AlarmType`)은 통합 알람 조회와 SSE 푸시 이벤트가 **공유** (단일 정의).
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
- **답변 등록은 졸업생 + 질문 작성자 본인** (`AnswerServiceImpl.create`): 기본은 졸업생만 답변 가능하나, **본인이 작성한 질문글에 한해 학생(STUDENT)도 답변 허용**. 검증식 = `member.getRole() != GRADUATE && !question.getMember().getId().equals(member.getId())` 일 때만 403 (`ANSWER_NOT_GRADUATE`). 도메인 의도 = Q&A 답변은 멘토(졸업생)가 학생 질문에 답하는 흐름이되, 질문자 본인의 자문자답은 예외 허용. `Question` 조회를 역할 검증보다 **먼저** 수행하므로 존재하지 않는 questionId 는 403 이 아닌 404 (`QUESTION_NOT_FOUND`)가 먼저 발생. 본인 질문 본인 답변 시 알람 미발행 분기는 그대로 동작. 수정/삭제는 기존 작성자 권한만 검증 (역할 제한 X).
- 좋아요: `QuestionLike`, `AnswerLike` (복합키 `AnswerLikeId`), toggle 방식. **본인이 작성한 글에는 좋아요 불가** (`QuestionServiceImpl.toggleLike` / `AnswerServiceImpl.toggleLike` 진입부에서 작성자 == 호출자 체크. 위반 시 400 `QUESTION_SELF_LIKE` / `ANSWER_SELF_LIKE`).
- **카운트·내 좋아요 여부**는 응답 DTO 에 포함 (`QuestionResponse.likeCount`/`likedByMe`/`answerCount`, `AnswerResponse.likeCount`/`likedByMe`). 매 GET 시 `countBy*` / `existsByMemberIdAnd*` 쿼리 호출 (캐시 컬럼 미도입). 모든 Q&A GET API (`getList`, `getOne`, `Answer.getList`) 가 `Member` 를 받아 `likedByMe` 채움.
- 좋아요 토글 응답: `LikeToggleResponse(liked, likeCount)` — 토글 후 새 상태·카운트 즉시 반환. 프론트 추가 GET 불필요.
- 작성자만 수정/삭제
- **삭제 캐스케이드 (서비스 레벨)**:
  - `Answer` 삭제 (`AnswerServiceImpl.delete`): `AnswerAlarm` → `AnswerLike` → `Answer` 순. 자식 FK 모두 `nullable = false`/복합 PK 라 정리 필수.
  - `Question` 삭제 (`QuestionServiceImpl.delete`): 손자(`AnswerAlarm`, `AnswerLike`) → `Answer` → `QuestionLike` → `Question` 순. 손자는 2-level 경로(`@Modifying @Query`) 로 일괄 삭제. `QuestionServiceImpl` 가 `AnswerRepository/AnswerAlarmRepository/AnswerLikeRepository` 를 직접 주입받는 도메인 간 약결합 허용 (각 답변별 소유자가 다를 수 있어 `AnswerService.delete` 위임은 부적합).

### 로드맵 (roadmap)
- 흐름: 데이터 파트가 화면 폼(학년/학점/전공/관심 직무/보유 역량/목표 기업) 입력값으로 AI 호출 → 결과 텍스트만 백엔드가 저장. **AI 호출 자체는 백엔드 미포함** (Spring 측에 OpenAI/HTTP 클라이언트 의존성 없음).
- `Roadmap.content`: MySQL `TEXT` 컬럼 (`@Column(columnDefinition = "TEXT")`) — 일반 문자열을 그대로 저장. 내부 형식 강제 없음 (JSON·평문 무관). 본문이 255자를 넘을 수 있어 `VARCHAR` 가 아닌 `TEXT`.
- 등록(`POST /api/v1/roadmaps`) 시 `RoadmapServiceImpl.validateContent()`로 사전 검증:
  - null / blank → `ROADMAP_INVALID_CONTENT` (HTTP 400)
  - 그 외 비어 있지 않은 모든 문자열 통과 (content 내부 형식 미강제)
- update API 미지원 (재생성 = 새 create + 기존 delete)
- 삭제는 소유자(member.id 일치)만 가능 (`ROADMAP_UNAUTHORIZED`)
- **삭제 캐스케이드 (서비스 레벨)**: `RoadmapServiceImpl.delete` 에서 `RoadmapAlarm` 먼저 `deleteByRoadmapId` 로 정리 → `Roadmap` 삭제. `RoadmapAlarm.roadmap` 이 `nullable = false` FK 라 정리 없이는 MySQL FK constraint 위반.
- Swagger 기본 예시값은 일반 문자열(`{ "content": "string" }`)로 지정되어 있음 (`RoadmapSwagger.createRoadmap`의 `@ExampleObject`) — Try it out 즉시 200 통과

## 공통 패턴

- **부분 업데이트**: 엔티티를 Builder로 재구성, `null`인 필드는 기존 값 유지 (`request.x() != null ? request.x() : entity.getX()`)
- **권한 검증**: 작성자 ID 비교 후 도메인별 `*Handler` 예외 throw
- **응답 포맷**: 모든 컨트롤러는 `ApiResponse<T>` 래퍼 사용
- **성공 메시지**: 삭제·탈퇴 등 컨트롤러가 `ApiResponse` result 로 반환하는 사용자 노출 성공 메시지는 `global/common/SuccessMessage` 상수로 관리 (컨트롤러·`*ControllerTest` 가 동일 심볼 참조)
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
| 마이페이지 | `member`(`MyPageService` 애그리게이터) — 활동 통계(`coffeechat`/`roadmap`/`qna/question`) + `techstack` + `interested_job` + `job_post` 조합 |
| 커피챗 매칭 (정보 입력 → 결과 → 상세) | `coffeechat` (매칭 알고리즘 연동, 무상태 pass-through) |
| 커피챗 요청/수락/거절 + 나의 활동 | `coffeechat` (요청 상태 전이, 수락된 커피챗 이력) |

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

> **관찰 (의도 확인 필요)**: `ErrorStatus.getReason()`/`getReasonHttpStatus()` 가 실패 코드 enum 인데 `ErrorReasonDTO` 를 `.isSuccess(true)` 로 고정 생성한다(`SuccessStatus` 와 대비). 현재 `ApiResponse.onFailure` 가 `isSuccess=false` 를 따로 지정하므로 실제 응답엔 영향 없으나, 오해를 부르는 죽은 설정값이다 — 의도 확인 후 정리 여부 결정.

### TODO C: 회원가입 이메일 도메인 검증 미적용 (PR #22 후속, auth 도메인)

**문제**: `SignupRequest.email` 에 `@Pattern` (`^[a-zA-Z0-9._%+\-]+@mju\.ac\.kr$`) 이 선언돼 있으나, `AuthController.signup` (`AuthController.java:24`) 과 `AuthSwagger.signup` (`AuthSwagger.java:26`) 의 `SignupRequest` 파라미터에 `@Valid` 가 없어 검증이 트리거되지 않음. 같은 컨트롤러의 `sendCode`/`verifyCode` 는 `@Valid` 보유. → 현재 `@mju.ac.kr` 제한이 가입 경로에서 무력화된 상태.

**디폴트 결정**: `signup` 의 `SignupRequest` 파라미터에 `@Valid` 추가. 인터페이스(`AuthSwagger`)·구현(`AuthController`) 양쪽 동기화. 검증 실패는 기존 `MethodArgumentNotValidException` 핸들러가 `_BAD_REQUEST` 로 매핑 (회원 도메인 소셜 링크 검증과 동일 패턴).

**출처**: PR #22 (`ddd4004`) 가 `@Pattern` 만 추가하고 `@Valid` 를 누락.

### TODO D: 이메일 인증 결과가 회원가입에 미반영 (PR #22 후속, auth 도메인)

**문제**: `AuthServiceImpl.signup` (`AuthServiceImpl.java:34-62`) 이 `EmailService`/`RedisUtil` 을 참조하지 않음. `POST /api/v1/auth/verify-code` 통과 여부와 무관하게 가입이 가능 — 인증 안 한 이메일로도 회원가입됨. `EmailServiceImpl.verifyCode` 는 코드 일치 시 Redis 키를 삭제만 하므로 "인증 완료" 상태가 어디에도 남지 않음.

**미확정 (사용자 결정 필요)**: signup 이 인증 통과를 확인하는 방식. 후보 — (a) `verifyCode` 성공 시 `verified:{email}` 플래그를 Redis 에 단기 저장 → `signup` 진입부에서 존재 확인 후 소비, (b) `verifyCode` 가 단기 인증 토큰을 발급해 `signup` 요청에 포함. 방식 확정 전까지 코드 변경 보류.

**출처**: PR #22.

### 11개 작업자 노트 (TODO #1~#11 머지 완료 후 보존되는 일반 가이드)

- **브랜치 정책**: 각 TODO 를 **개별 브랜치 + 개별 PR** 로 처리하는 것을 기본으로 한다. 영향 범위가 큰 TODO 는 단독 PR 필수. 같은 도메인 내 작은 변경은 묶어서 1개 PR 도 허용 (작업자 판단). 사용자가 "TODO N 작업" 으로 단일 항목 지목 시 그 항목만 단독 PR.
- **공통 커밋 메시지 컨벤션**: `feat(<도메인>): ...` / `fix(<도메인>): ...` / `refactor(<도메인>): ...` / `docs(CLAUDE.md): ...` prefix. 마지막 줄에 항상 `https://claude.ai/code/session_...` 포함.
- **Swagger 검증**: 가능하면 PR 본문 Test plan 에 Swagger 시나리오 체크박스 포함.
- **테스트 실패 시**: `--no-verify` 등으로 우회 금지. 원인 분석 후 수정.
