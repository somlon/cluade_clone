# 진행 예정 작업 (To-do)

> 이 파일은 루트 `CLAUDE.md` 가 `@import` 하지 않는다(자주 바뀌어 자동 로드 제외). 사용자가 TODO 관련 명령을 내리면 이 파일을 직접 읽고 진행한다. 완료 후 항목은 이 파일에서 삭제하고 `docs/agent/backend.md` 의 해당 도메인 섹션에 정식 규칙으로 통합한다.

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

### TODO F: ddingconnect-backend 전 계층 동기화 (cluade_clone 기준, 크로스 레포)

**문제**: `mju-capstone-4/ddingconnect-backend` 가 이 레포(`cluade_clone`) 대비 controller/service/dto 계층과 global 인프라가 대거 누락된 상태. 마지막 동기화 커밋(`fe032d9 chore(db): cluade_clone 기준 DB 계층 동기화`)으로 도메인 엔티티·레포지토리 계층만 따라잡았고, 메인 소스 약 86개·테스트 약 26개 파일이 미반영. 일부 공통 파일은 내용 불일치, 잘못된 패키지 위치 등 구조 이슈 3건도 존재.

**주의 — 크로스 레포**: 이 작업은 `ddingconnect-backend` 에 쓰기 작업을 한다. `## 작업 레포 범위 규칙` 에 따라 실행 전 사용자의 명시적 지시("ddingconnect-backend 에서 작업하라")가 반드시 필요하다 — 지시 없이 자동 실행하지 말 것. 모든 단계의 기준(source of truth)은 `cluade_clone` 이다.

**작업 순서** (컴파일 의존성 순):

1. **global 공통 상수 + ErrorStatus** — `global/common/SuccessMessage`·`ValidationPattern` 신규 추가. `ErrorStatus` 에 누락된 에러코드(Member·CoffeeChat·PostContents·Question·Answer·Roadmap·Alarm, 약 26개) 추가. 이후 전 계층 컴파일의 전제.
2. **global/alarm 패키지 + sse 정합** — `AlarmController`·`AlarmSwagger`·`AlarmService(+Impl)`·`AlarmResponse`·`RelativeTimeFormatter` 신설. `AlarmType` 을 `global/sse/` → `global/alarm/` 로 이동. `global/sse/AlarmNotificationEvent`·`AlarmNotificationListener` 추가. `SseService`·`SseServiceImpl`·`SseTestController` 의 `AlarmType` import 경로 수정.
3. **exception handler 8종** — `AlarmHandler`·`AnswerHandler`·`CoffeeChatHandler`·`JobPostHandler`·`QuestionHandler`·`RoadmapHandler`·`TargetJobHandler`·`TechStackHandler` 추가.
4. **8개 비즈니스 도메인 (dto → service → controller 순)** — coffeechat·interested_job·job_post·member·qna(answer·question)·roadmap·techstack 의 dto/service/serviceImpl/controller/swagger 추가. 함께: `Member.name`·`Graduate.jobType` 필드 엔티티 재동기화. member — 잘못 위치한 `domain/member/domain/MemberController.java` stub 제거 후 `controller/` 정식 버전으로 교체. techstack — `TechStackRepository.existsByMemberIdAndName`(cluade_clone 미존재) 역방향 불일치 처리 방침 결정.
5. **global/auth 내용 동기화 + 빌드/설정** — auth DTO 3종(`CodeSendRequest`·`SignupRequest`·`VerifyCodeRequest`) 인라인 정규식 → `ValidationPattern` 상수 참조, `AuthServiceImpl` → `SuccessMessage.SIGNUP_SUCCESS`, `JwtAuthenticationFilter` 화이트리스트에 `/swagger-ui.html` 추가. `build.gradle` 의 하드코딩된 `C:/gradle-builds/...` buildDir → `cluade_clone` 외부화 방식으로 교체, `application.yml` 에 `matching.algorithm.base-url` 추가.
6. **테스트 보강** — 누락된 도메인별 controller/service 테스트(~21개), `global/alarm` 테스트 3종, `AlarmNotificationListenerTest`, `EntityIntegrationTest`, `support/WithMockLoginMember`, `CoffeeChatMatchingTestConstants` 추가. 테스트 리소스 `application.properties`·`logback-test.xml` 추가. 기존 SSE 테스트 3종(`SseControllerTest`·`SseEmitterRepositoryTest`·`SseServiceTest`)의 한글 메서드명 → 영문 camelCase 변경.

**범위 제외**: `ddingconnect-backend` 에만 존재하는 `DdingconnectApplicationTests.java` 삭제는 사용자 지시에 따라 이 TODO 범위에서 제외한다.

**출처**: `cluade_clone` ↔ `ddingconnect-backend` 전체 코드 비교 세션 (png·CLAUDE.md 제외).

### TODO I: `target_job.key2` 미사용 컬럼 제거 (interested_job 도메인)

**문제**: `TargetJob.key2` (`target_job.key2`, varchar(255)) 는 ERD 잔재 컬럼으로 entity 클래스 헤더 주석에 "(미사용, ERD 잔재)" 로 자가 명시되어 있다. signup·`TargetJobServiceImpl.replace` 등 어디에서도 set 하지 않아 저장값이 항상 null 이며, `TargetJobResponse` 가 record 필드로 응답에 노출하지만 항상 null 만 반환한다. 두 레포(`cluade_clone` / `ddingconnect-backend`) DB 스키마 비교 세션에서 양쪽 모두 동일하게 dead column 으로 식별됨.

**디폴트 결정**: 아래 2개 파일에서 `key2` 관련 코드를 제거한다.

1. `backend/src/main/java/mju/capstone/ddingconnect/domain/interested_job/domain/TargetJob.java`
   - `@Column(length = 255) private String key2;` 필드 삭제
   - 클래스 헤더 javadoc 의 `Key2(varchar(255)) → key2 (현재 미사용, ERD 잔재)` 한 줄 삭제
2. `backend/src/main/java/mju/capstone/ddingconnect/domain/interested_job/dto/response/TargetJobResponse.java`
   - record 의 `String key2` 파라미터 삭제
   - javadoc `@param key2 추가 키값` 라인 삭제
   - `from()` 의 `targetJob.getKey2()` 인자 삭제

호출 측 점검: `TargetJobResponse.from(...)` 외에 record 생성자를 직접 호출하는 곳(`new TargetJobResponse(...)`)이 없는지 확인하고 있다면 정리. `TargetJobControllerTest`/`TargetJobServiceImplTest` 에서 `key2` 단언이 있으면 함께 제거.

문서 동기화: `docs/agent/backend.md` 의 `### 관심 직군 (interested_job)` 섹션 `TargetJob 엔티티는 ... + key2(미사용, ERD 잔재) 로 구성.` 문장에서 `+ key2(미사용, ERD 잔재)` 부분을 삭제.

DB 측은 `application-db.yml` 이 `ddl-auto=create` 라 부팅 시 자동으로 컬럼이 사라진다 — 별도 마이그레이션 작성 불필요. 운영 DB 도입(Flyway/Liquibase) 이후라면 `ALTER TABLE target_job DROP COLUMN key2;` 한 줄을 마이그레이션에 추가.

**주의 — 크로스 레포**: 동일 컬럼이 `mju-capstone-4/ddingconnect-backend` 에도 존재한다. 본 TODO 는 `## 작업 레포 범위 규칙` 에 따라 `cluade_clone` 만 다루며, ddingconnect-backend 측 제거는 TODO F 동기화 시 또는 사용자의 별도 지시("ddingconnect-backend 에서 작업하라")로 진행한다.

**출처**: 두 레포 DB 스키마/기능 비교 세션 — 두 레포 entity 헤더 주석에서 모두 "미사용/ERD 잔재" 로 자가 식별된 컬럼.

### TODO J: ddingconnect-backend 커피챗 도메인 동기화 (coffeechat 도메인, 크로스 레포)

**문제**: `mju-capstone-4/ddingconnect-backend` 의 커피챗 도메인이 이 레포(`cluade_clone`) 보다 뒤처져 있다. 두 레포 커피챗 비교 결과(`cluade_clone` `main` 365ccc9 ↔ `ddingconnect-backend` `main`(=`develop`, 동일)) — 커피챗 도메인 소스 23개 중 2개가 내용 불일치, 커피챗 테스트 6개가 ddingconnect-backend 에 전무하다.

**주의 — 크로스 레포**: 이 작업은 `ddingconnect-backend` 에 쓰기 작업을 한다. `## 작업 레포 범위 규칙` 에 따라 실행 전 사용자의 명시적 지시("ddingconnect-backend 에서 작업하라")가 반드시 필요하다 — 지시 없이 자동 실행하지 말 것. 기준(source of truth)은 `cluade_clone` 이다.

**디폴트 결정**: `cluade_clone` 커피챗 도메인을 기준으로 아래 8개 파일을 `ddingconnect-backend` 에 반영한다. 경로는 ddingconnect-backend 기준 `src/{main,test}/java/mju/capstone/ddingconnect/domain/coffeechat/` 이하.

소스 수정 (2):

1. `service/CoffeeChatServiceImpl.java` — 커피챗 신청(생성, PENDING) 시 신청자에게 "신청 접수" 확인 알람 1건 추가 발행 (현재는 수신자 "요청 도착" 알람만 발행). `REQUEST_SENT_CONTENT_FORMAT` 상수 + `CoffeeChatAlarm` 저장 + `AlarmNotificationEvent` 발행 추가. (cluade_clone PR #65)
2. `service/MatchingAlgorithmClientImpl.java` — 매칭 알고리즘 클라이언트를 데이터 파트(`ddingconnect-data`) 실제 스키마로 재정렬. 현재 ddingconnect-backend 는 TODO 스켈레톤(엔드포인트 `/match`, 응답 `memberIds`) 상태 → 확정본(엔드포인트 `/api/data/coffeechat/match`, 요청 6필드 플랫 snake_case `{year,gpa,major,job,tech_stacks,goal}` + `tech_stacks` 의 `TechStackName` 화이트리스트 정규화, 응답 `top_matches` envelope 에서 `id` 추출)으로 교체. `MockRestServiceServer` 용 package-private 생성자 포함. (cluade_clone PR #64)

테스트 신규 추가 (6) — ddingconnect-backend 에 커피챗 테스트가 전무:

3. `CoffeeChatMatchingTestConstants.java`
4. `controller/CoffeeChatControllerTest.java`
5. `controller/CoffeeChatMatchingControllerTest.java`
6. `service/CoffeeChatServiceImplTest.java`
7. `service/CoffeeChatMatchingServiceImplTest.java`
8. `service/MatchingAlgorithmClientImplTest.java`

나머지 커피챗 소스 21개(컨트롤러·엔티티·DTO·`CoffeeChatHandler` 등)와 `MemberRepository.java`·`application.yml` 은 두 레포 동일 — 수정 불필요. 반영 후 `./gradlew test` 로 검증한다.

**TODO F 와의 관계**: TODO F(ddingconnect-backend 전 계층 동기화)의 커피챗 부분을 최신 비교로 재확인·축소한 슬라이스다. 커피챗만 우선 동기화하려면 이 TODO J 를, 전 도메인을 일괄 동기화하려면 TODO F 를 따른다.

**출처**: `cluade_clone` ↔ `ddingconnect-backend` 커피챗 도메인 비교 세션.

### TODO K: ddingconnect-backend 로드맵 도메인 동기화 (roadmap 도메인, 크로스 레포)

**문제**: `mju-capstone-4/ddingconnect-backend` 의 로드맵 도메인이 이 레포(`cluade_clone`) 보다 뒤처져 있다. 두 레포 로드맵 비교 결과(`cluade_clone` `main` d0d4f2e ↔ `ddingconnect-backend` `develop`(=`main`, 로드맵 동일)) — 로드맵 관련 소스 7개가 내용 불일치, 데이터 파트 AI 연동 클라이언트 2개와 로드맵 테스트 3개가 ddingconnect-backend 에 전무하다. 현재 ddingconnect-backend 로드맵은 `content` 문자열을 그대로 받아 저장하는 초기 버전이라, 데이터 파트(`ddingconnect-data`)의 AI 로드맵 생성(`POST /api/data/generate`)과 연동돼 있지 않다.

**주의 — 크로스 레포**: 이 작업은 `ddingconnect-backend` 에 쓰기 작업을 한다. `## 작업 레포 범위 규칙` 에 따라 실행 전 사용자의 명시적 지시("ddingconnect-backend 에서 작업하라")가 반드시 필요하다 — 지시 없이 자동 실행하지 말 것. 기준(source of truth)은 `cluade_clone` 이다.

**디폴트 결정**: `cluade_clone` 로드맵 도메인을 기준으로 아래 14개 파일을 `ddingconnect-backend` 에 반영한다. 경로는 ddingconnect-backend 기준 `src/{main,test}/java/mju/capstone/ddingconnect/` 이하(resources 만 별도 표기).

신규 추가 — 데이터 파트 AI 연동 클라이언트 (2):

1. `domain/roadmap/service/RoadmapAiClient.java` — 로드맵 AI 생성 클라이언트 인터페이스. `generate(form, memberId)` 단일 메서드.
2. `domain/roadmap/service/RoadmapAiClientImpl.java` — `RestClient` 구현체. `POST {data.base-url}/api/data/generate?member_id={id}` 호출, 데이터 파트 `RoadmapRequest` 스키마(6필드 플랫 snake_case) body 전송, 응답 JSON 을 파싱 없이 문자열로 반환. connect 3초/read 60초 타임아웃(AI 생성이 수십 초 소요), 실패 시 `ROADMAP_AI_GENERATION_FAILED`(502). 커피챗 `MatchingAlgorithmClient` 와 동일한 얇은 클라이언트 패턴. `MockRestServiceServer` 용 package-private 생성자 포함.

소스 수정 — 로드맵 도메인 (7):

3. `domain/roadmap/dto/request/CreateRoadmapRequest.java` — 단일 `content` 필드 → 입력 폼 6필드(`grade`·`gpa`·`major`·`targetJob`(`TargetJobCategory`)·`currentSkills`(`List<TechStackName>`)·`targetCompany`)로 교체.
4. `domain/roadmap/dto/response/RoadmapResponse.java` — `createdAt`(`LocalDateTime`) 필드 추가. 마이 로드맵 목록 날짜 표시용, `from()` 매핑도 함께 수정.
5. `domain/roadmap/service/RoadmapService.java` — `create(Member, …)` → `create(Long memberId, …)`, `getList()` → `getList(Member member)` 시그니처 변경.
6. `domain/roadmap/service/RoadmapServiceImpl.java` — `MemberRepository`·`RoadmapAiClient` 주입. `create`: memberId 로 회원 조회(미존재 `MEMBER_NOT_FOUND`) → AI 호출 → content 검증(null/blank → `ROADMAP_INVALID_CONTENT`) → 저장 → 알람 발행. `getList`: `findByMemberIdOrderByCreatedAtDesc` 로 회원 스코프 최신순 조회.
7. `domain/roadmap/controller/RoadmapController.java` — `create` 는 `@RequestParam Long memberId` 수신(기존 `@LoginMember` 제거), `getList` 는 `@LoginMember Member` 수신.
8. `domain/roadmap/controller/RoadmapSwagger.java` — 변경된 시그니처에 맞춰 Swagger 설명·요청 예시(6필드 입력 폼)로 갱신.
9. `domain/roadmap/domain/repository/RoadmapRepository.java` — `findByMemberIdOrderByCreatedAtDesc(Long memberId)` 쿼리 메서드 추가.

소스 수정 — 공통/설정 (2):

10. `global/response/code/status/ErrorStatus.java` — `// Roadmap` 블록에 `ROADMAP_AI_GENERATION_FAILED(HttpStatus.BAD_GATEWAY, "ROADMAP502", "로드맵 AI 생성에 실패했습니다.")` 한 줄 추가.
11. `src/main/resources/application.yml` — `data.base-url: ${DATA_BASE_URL:http://localhost:8000}` 설정 블록 추가(기존 `matching.algorithm.base-url` 과 형제).

테스트 신규 추가 — ddingconnect-backend 에 로드맵 테스트가 전무 (3):

12. `domain/roadmap/controller/RoadmapControllerTest.java` — 컨트롤러 4 케이스(create/list/detail/delete).
13. `domain/roadmap/service/RoadmapAiClientImplTest.java` — AI 클라이언트 5 케이스(`MockRestServiceServer` 기반).
14. `domain/roadmap/service/RoadmapServiceImplTest.java` — 서비스 11 케이스.

수정 불필요(두 레포 내용 동일): `domain/roadmap/domain/Roadmap.java`, `domain/roadmap/domain/RoadmapAlarm.java`, `domain/roadmap/domain/repository/RoadmapAlarmRepository.java`, `global/response/exception/handler/RoadmapHandler.java`.

의존성·주의:

- `RestClient` 는 Spring Boot 3.5 기본 제공 — `build.gradle` 변경 불필요(커피챗 `MatchingAlgorithmClient` 가 같은 패턴 사용 중).
- `RoadmapControllerTest` 가 쓰는 `support/WithMockLoginMember` 는 `develop` 에 이미 존재(커피챗 작업 시 추가) — `main` 기준 작업 시 함께 가져와야 한다.
- `CreateRoadmapRequest` 의 `targetJob`/`currentSkills` enum 값은 데이터 파트 enum(직군 11종/기술스택 24종)과 일치해야 한다.
- **API 변경(프론트 영향)**: `POST /api/v1/roadmaps` 가 `content` 단일 body → `?memberId=` 쿼리 + 6필드 body 로 바뀐다. 프론트 연동 동기화 필요.
- 반영 후 `./gradlew test` 로 검증한다.

**TODO F 와의 관계**: TODO F(ddingconnect-backend 전 계층 동기화)의 로드맵 부분을 최신 비교로 재확인·구체화한 슬라이스다. 로드맵만 우선 동기화하려면 이 TODO K 를, 전 도메인을 일괄 동기화하려면 TODO F 를 따른다 — TODO J(커피챗)와 동일 성격.

**출처**: `cluade_clone` ↔ `ddingconnect-backend` 로드맵 도메인 비교 세션 (`cluade_clone` `main` d0d4f2e ↔ `ddingconnect-backend` `develop`/`main`).

### TODO L: 마이페이지 통합 수정 — 역할별 엔드포인트/DTO 분리 (member 도메인)

**문제**: `backend/src/main/java/mju/capstone/ddingconnect/domain/member/dto/request/UpdateMyPageRequest.java` 가 STUDENT 전용(`targetJobs`)과 GRADUATE 전용(`jobPostsToAdd`·`jobPostIdsToDelete`) 필드를 단일 record 에 혼재해 들고 있어, 클라이언트가 본인 역할과 무관한 필드를 채워 보내도 사후 단계(`JobPostServiceImpl.create` 의 `POST_CONTENTS_NOT_GRADUATE`)에서만 거부된다. 동일 record 안의 `UpdateMemberRequest` 는 `grade`(STUDENT)/`businessCardImage·jobType·company·careerYear`(GRADUATE) 혼재이고 이 부분은 `MemberServiceImpl.validateRoleFields` 가 사전 차단하지만, mypage 통합 수정 경로의 `jobPostsToAdd/Delete` 에는 동일 가드가 없다 — 일관성·DX 양쪽 모두 결함.

**디폴트 결정**: 엔드포인트와 DTO 를 역할별로 분리한다.

- 엔드포인트: `PATCH /api/v1/members/mypage/student`, `PATCH /api/v1/members/mypage/graduate`. 기존 `PATCH /api/v1/members/mypage` 는 deprecate 마킹 후 점진 제거.
- 컨트롤러 진입부에서 `member.getRole()` 과 엔드포인트가 불일치하면 즉시 `MEMBER_FIELD_ROLE_MISMATCH` 반환 (UNKNOWN 도 거부).
- 신규 DTO: `UpdateStudentMyPageRequest(profile, techStacks, targetJobs)`, `UpdateGraduateMyPageRequest(profile, techStacks, jobPostsToAdd, jobPostIdsToDelete)`. 그 안의 프로필 부분은 `UpdateStudentProfileRequest`(공통 + `grade`) / `UpdateGraduateProfileRequest`(공통 + `businessCardImage`·`jobType`·`company`·`careerYear`) 로 분리해 각 역할 필드만 노출.
- `MyPageService.updateMyPage` 를 역할별 메서드 2개로 오버로드 — STUDENT 경로엔 jobPost 분기 자체가 없고, GRADUATE 경로엔 targetJob 분기가 없다. 단일 `@Transactional` 안에서 위임하는 기존 원자성 규약은 유지.

**수정 파일**: `backend/src/main/java/mju/capstone/ddingconnect/domain/member/controller/MemberController.java`, `member/controller/MemberSwagger.java`, `member/service/MyPageService.java`, `member/service/MyPageServiceImpl.java`. 신규 DTO 4개(`member/dto/request/` 아래). 기존 `UpdateMyPageRequest.java`·`UpdateMemberRequest.java` 는 호환성 위해 한동안 유지 후 정리. 관련 테스트 함께 갱신(`MemberControllerTest`·`MyPageServiceImplTest`).

**연관**: TODO N·O·Q 의 multipart 업로드 엔드포인트(`/profile-image`·`/portfolio`·`/business-card`)가 이 역할별 검증 패턴 위에서 동작 — 본 TODO 가 선행되면 이후 작업의 분기 코드를 줄일 수 있다. TODO R 의 `jobPostsToAdd` 타입(`CreateJobPostLinkRequest`)과도 정합.

**출처**: 재학생/졸업생 마이페이지 화면 + `ddingconnect-backend` `develop`(e985b05) 코드 비교 세션.

### TODO M: 나의 활동 페이지 API — 커피챗/로드맵/Q&A 본인 활동 조회 (member·coffeechat·qna 도메인)

**문제**: 마이페이지 상단 통계 카드(`MyPageResponse.ActivityStats` 의 `coffeeChatCount`·`roadmapCount`·`questionCount`)를 클릭하면 "나의 활동" 페이지로 진입해 본인 활동 **목록**을 보여줘야 하나, 현재 백엔드는 카운트만 제공하고 본인 스코프 목록 조회 API 가 없다. 단, 로드맵은 `RoadmapServiceImpl.getList` 가 이미 `findByMemberIdOrderByCreatedAtDesc` 로 본인 글만 반환하므로 그대로 재사용 가능.

**디폴트 결정**: 도메인별로 다음과 같이 처리한다.

1. **로드맵** — `GET /api/v1/roadmaps` 그대로 재사용. **백엔드 변경 0건.** 프론트 "나의 활동/로드맵" 탭에서 동일 엔드포인트 호출.
2. **Q&A 질문** — 신규 엔드포인트 `GET /api/v1/questions/me` + 신규 서비스 메서드 `QuestionService.getMyQuestions(Member)` 추가. 내부 구현은 `questionRepository.findByMemberId(memberId)` (이미 `MemberServiceImpl` 에서 회원탈퇴 캐스케이드 용도로 사용 중) → 기존 `toResponse(q, member)` 매핑. **기존 `GET /api/v1/questions` 의 `getList(member)` 는 절대 수정하지 않는다** — Q&A 게시판은 전체 질문을 보여줘야 하므로 별도 엔드포인트로 분리.
3. **커피챗** — 신규 컨트롤러 `MyActivityController` 와 엔드포인트 `GET /api/v1/members/me/activity/coffeechats` 추가. 신규 서비스 메서드 `CoffeeChatService.getMyActivities(Member)` 는 `findByRequesterId` + `findByReceiverId` 결과를 합쳐 중복 제거하고, 본인이 아닌 쪽(상대방) 의 닉네임/학과/관심직무/기술스택을 묶어 신규 DTO `CoffeeChatPartnerResponse` 로 반환. 카드 UI 가 상대방 정보를 표시하기 때문.

**필터**: 후순위. 컨트롤러 시그니처에 `@RequestParam(required=false)` 자리만 예약하고 구현은 후속 작업.

**수정/신규 파일**:

- `backend/src/main/java/mju/capstone/ddingconnect/domain/qna/question/service/QuestionService.java`·`QuestionServiceImpl.java` — `getMyQuestions` 추가
- `domain/qna/question/controller/QuestionController.java`·`QuestionSwagger.java` — `/me` 엔드포인트 1개 추가
- `domain/coffeechat/service/CoffeeChatService.java`·`CoffeeChatServiceImpl.java` — `getMyActivities` 추가
- `domain/coffeechat/dto/response/CoffeeChatPartnerResponse.java` 신규
- `domain/member/controller/MyActivityController.java`·`MyActivitySwagger.java` 신규
- 관련 테스트 함께 갱신/추가 (`QuestionControllerTest`·`QuestionServiceImplTest`·`CoffeeChatServiceImplTest`·`MyActivityControllerTest`)

**연관**: TODO P(졸업생 로드맵 제외) — 로드맵 엔드포인트 진입 가드 추가는 거기서 처리.

**출처**: 마이페이지 활동 카드 화면 + "나의 활동" 페이지 화면(`나의 활동 - 클릭시.png`) vs `ddingconnect-backend` `develop`(e985b05) 코드 비교.

### TODO N: 프로필 사진 멀티파트 업로드 (member 도메인)

**문제**: 마이페이지 화면 상단의 프로필 사진(동그라미) 영역에 사진을 업로드해 표시하는 기능이 요구된다. 현재 `Member.profileImage` 는 `varchar(255)` URL 문자열만 보관하고(`Member.java`), 사진 업로드 전용 엔드포인트가 없다. `UpdateMemberRequest.profileImage` 가 String 이라 프론트가 URL 을 직접 만들어 보내야 하는 어색한 구조.

**디폴트 결정**: 멀티파트 업로드 엔드포인트를 신설해 S3 업로드 + URL 저장을 백엔드가 책임진다. `S3Service.uploadImage(MultipartFile)` (`backend/src/main/java/mju/capstone/ddingconnect/global/aws/S3Service.java`) 함수가 이미 회원가입 증명서 업로드 흐름(`AuthServiceImpl.signup`)에서 사용 중 → 그대로 재사용. **OCR 검증은 무관**(현재 회원가입 증명서도 OCR 없이 단순 S3 업로드만 수행, `data` 레포의 `routers/ocr_router.py` 는 미연동 상태).

- 신규 엔드포인트: `PATCH /api/v1/members/me/profile-image` (multipart/form-data, `@RequestPart("image") MultipartFile`)
- 응답: 갱신된 `MemberResponse` 또는 `{profileImage: <url>}` (작업자 판단, Swagger 와 일치시키기)
- 서비스 동작: 기존 이미지 URL 이 있으면 `s3Service.deleteImage(oldUrl)` 후 새 URL 업로드 → `Member.profileImage` 갱신
- content-type 화이트리스트: `image/png`·`image/jpeg`·`image/webp`. 크기 제한 5MB. 위반 시 신규 ErrorStatus `_FILE_TYPE_NOT_ALLOWED`·`_FILE_TOO_LARGE` 로 400 거부

**수정 파일**: `member/controller/MemberController.java`, `member/controller/MemberSwagger.java`, `member/service/MemberService.java`·`MemberServiceImpl.java` — `updateProfileImage(Member, MultipartFile)` 추가. `global/response/code/status/ErrorStatus.java` — 두 에러코드 추가. 테스트 보강(`MemberControllerTest`·`MemberServiceImplTest`).

**연관**: TODO O·Q 가 같은 multipart S3 업로드 패턴을 공유. `S3Service` 일반화는 TODO O 에서 처리하며, 본 TODO 는 기존 `uploadImage` 를 그대로 사용해도 무방.

**출처**: 마이페이지 수정 페이지 사진 등록 요구사항 + `AuthServiceImpl.signup` 의 기존 S3 업로드 패턴 분석.

### TODO O: 포트폴리오 PDF 업로드 + S3Service 일반화 (member 도메인 · global/aws 인프라)

**문제**: 마이페이지 수정 시 포트폴리오 영역에 PDF 파일을 업로드 가능해야 한다. `Member.portfolio` 는 `varchar(255)` 단일 문자열로 충분(이미지·PDF 모두 결국 S3 URL 만 DB 에 저장하는 패턴)하나, 업로드 엔드포인트 자체가 없고 기존 `S3Service.uploadImage` 는 이름이 image 전용이며 content-type 검증을 안 해 PDF·기타 파일이 묵시적으로 통과된다 — 명확성 부족.

**디폴트 결정**:

1. **`S3Service` 일반화**: `uploadFile(MultipartFile file, Set<String> allowedContentTypes, long maxBytes)` 메서드 신규 추가. 진입부에서 content-type/크기 검증 후 S3 업로드. 기존 `uploadImage` 는 내부적으로 `uploadFile(file, IMAGE_CONTENT_TYPES, IMAGE_MAX_BYTES)` 를 호출하도록 위임 정리(호출처 영향 0). 가능하면 `deleteImage` 도 임의 키 삭제 가능하므로 `deleteFile` 로 rename(기존 호출처 함께 마이그레이션).
2. **신규 엔드포인트** `PATCH /api/v1/members/me/portfolio` (multipart/form-data, `@RequestPart("file") MultipartFile`). content-type 은 `application/pdf` 만 허용, 크기 제한 20MB. 위반 시 TODO N 과 같은 `_FILE_TYPE_NOT_ALLOWED`·`_FILE_TOO_LARGE` 로 거부.
3. **서비스 동작**: 기존 URL 있으면 `s3Service.deleteFile(oldUrl)` 후 새 URL 업로드 → `Member.portfolio` 갱신.

**수정 파일**: `global/aws/S3Service.java` — `uploadFile` 추가 + `uploadImage` 위임 정리 + (선택) `deleteImage` → `deleteFile` rename. `member/controller/MemberController.java`·`MemberSwagger.java`. `member/service/MemberService(Impl).java` — `updatePortfolio(Member, MultipartFile)` 추가. `global/response/code/status/ErrorStatus.java` — TODO N 과 공유. 관련 테스트 보강.

**주의 (PDF 가 DB 에 저장되지 않음)**: 파일 자체는 S3 에 저장되고 DB(`Member.portfolio`) 에는 public URL 문자열만 들어간다. `varchar(255)` 길이는 현 S3 URL 형식(`https://<bucket>.s3.<region>.amazonaws.com/<key>`)에 충분 — 컬럼 길이 변경 불요.

**연관**: TODO N·Q.

**출처**: 마이페이지 수정 페이지 포트폴리오 요구사항 + `S3Service` 분석.

### TODO P: 졸업생 마이페이지/나의 활동 — 로드맵 항목 제외 (member·roadmap 도메인)

**문제**: 졸업생 마이페이지 화면(2번째 사진)의 활동 통계 카드에는 로드맵 항목 자체가 없다(`12 / 5` 두 개만). 현재 백엔드는 `MyPageServiceImpl.buildResponse` 가 역할 무관하게 `roadmapService.countMyRoadmaps(member)` 를 호출해 GRADUATE 응답에도 `roadmapCount` 가 채워진다. 또한 TODO M 의 `GET /api/v1/roadmaps` 도 GRADUATE 호출 시 차단 가드가 없다.

**디폴트 결정**:

1. **마이페이지 응답**: `MyPageServiceImpl.buildResponse` 에서 `member.getRole() == MemberRole.STUDENT` 일 때만 `countMyRoadmaps` 를 호출하고, 그 외 역할은 `0` 또는 `null` 로 반환. `MyPageResponse.ActivityStats.roadmapCount` 의 nullable 처리(또는 STUDENT/GRADUATE 변형 분리) 중 작업자 판단으로 결정 — 단, 프론트와 합의된 표현이 우선.
2. **백엔드 차단 가드**: `RoadmapController.getRoadmaps`(`/api/v1/roadmaps`) 진입부에 `if (member.getRole() == MemberRole.GRADUATE) throw new MemberHandler(ErrorStatus.MEMBER_FIELD_ROLE_MISMATCH);` 한 줄 추가. 프론트가 GRADUATE 화면에서 탭을 렌더링하지 않더라도 호출은 막아 보안 일관성 확보. 단 졸업생도 로드맵 단건 조회(`/{roadmapId}`)는 허용 — 다른 사람 로드맵을 볼 수 있어야 하므로 가드는 list 만 적용.

**수정 파일**: `member/service/MyPageServiceImpl.java`, `member/dto/response/MyPageResponse.java`(nullable 처리), `roadmap/controller/RoadmapController.java`. 관련 테스트 보강(`MyPageServiceImplTest` 의 GRADUATE 케이스, `RoadmapControllerTest` 의 가드 케이스).

**연관**: TODO M(나의 활동 페이지) — 로드맵 엔드포인트 공유.

**출처**: 졸업생 마이페이지 화면(2번째 사진) 카드 영역 분석 + `MyPageServiceImpl.buildResponse` 분기 결여 확인.

### TODO Q: 졸업생 명함 이미지 업로드 (member 도메인)

**문제**: 졸업생 마이페이지에 명함 등록 영역(네모 칸)이 있고 업로드한 사진이 표시돼야 한다. 현재 `Graduate.businessCardImage`(`Graduate.java`) 는 varchar(255) URL 문자열 보관용이며 업로드 엔드포인트가 없다.

**디폴트 결정**: TODO N(프로필 사진)과 동일한 패턴으로 신설한다.

- 신규 엔드포인트: `PATCH /api/v1/members/me/business-card` (multipart/form-data, `@RequestPart("image") MultipartFile`)
- **GRADUATE 전용**: 진입부에서 STUDENT/UNKNOWN 호출 시 `MEMBER_FIELD_ROLE_MISMATCH` 반환
- 동작: 기존 이미지 있으면 `s3Service.deleteImage`(또는 TODO O 이후 `deleteFile`) 후 새 이미지 업로드 → `Graduate.businessCardImage` 갱신
- content-type/크기 검증은 TODO N 과 동일 (`image/png`·`image/jpeg`·`image/webp`, 5MB)

**수정 파일**: `member/controller/MemberController.java`·`MemberSwagger.java`, `member/service/MemberService.java`·`MemberServiceImpl.java` — `updateBusinessCard(Member, MultipartFile)` 추가. 관련 테스트.

**연관**: TODO N·O 와 S3 업로드 패턴 공유.

**출처**: 졸업생 마이페이지 화면(2번째 사진)의 "내 명함 영역 + 사진 등록" 요구사항.

### TODO R: 졸업생 공고 — 링크 전용 등록 + 선배공고/일반공고 분리 표시 (job_post 도메인)

**문제**: 졸업생이 마이페이지에서 "나의 공고 올리기" 모달(3번째 사진)로 **링크만 입력**해 공고를 등록할 수 있어야 하고, 구직 정보 화면(4번째 사진)에서는 **선배가 올린 공고**와 **일반 공고 목록**이 시각적으로 분리돼 표시돼야 한다. 현재 `CreateJobPostRequest` 는 11 필드(`companyImage`·`region`·`careerType`·`jobType`·`country`·`location`·`fullLocation`·`deadline`·`detailUrl`·`preferredLanguages`·`companyName`) 모두 받는 구조라 링크만 입력하는 흐름과 맞지 않고, `JobPostService.getList()` 는 `postContentsRepository.findAll()` 로 선배 공고/크롤링 공고 구분 없이 전체를 반환한다.

추가 발견 (버그): `JobPostServiceImpl.create` 의 `toCategory(saved.getJobType())` 는 `jobType` 이 null 이면 `TargetJobCategory.valueOf(null)` 로 NPE 발생 — 링크만 등록되는 경로에서는 jobType 이 null 이라 반드시 가드 필요.

**디폴트 결정**:

R-1. **링크 전용 등록 경로 신설**:

- 신규 엔드포인트 `POST /api/v1/job-posts/link`
- 신규 DTO `CreateJobPostLinkRequest(String detailUrl)` — `@NotBlank` + URL 형식 `@Pattern` 검증
- 서비스 동작: `PostContents.builder().detailUrl(...).build()` (다른 필드 null) 저장 → `GraduateJobPost` 매핑 생성. **알람 발행 분기 스킵**(jobType 이 null 이므로 관심직무 매칭 불가)
- TODO L 의 `UpdateGraduateMyPageRequest.jobPostsToAdd` 도 동일 DTO 로 통일 권장 — 이후 변경 비용 절감

R-2. **`JobPostServiceImpl.create` jobType null 가드**:

- `toCategory(saved.getJobType())` 호출 직전에 `if (saved.getJobType() == null) return JobPostResponse.from(saved);` (또는 알람 분기 전체를 `if` 로 감싸기). 기존 11필드 등록 경로는 그대로 동작.

R-3. **선배 공고 / 일반 공고 분리 조회**:

- 분리 기준: `GraduateJobPost` 매핑 **존재 여부** (데이터 모델 변경 없이 가능)
- 신규 엔드포인트 ① `GET /api/v1/job-posts/graduates` — `GraduateJobPost` 매핑이 존재하는 PostContents 만 (선배 공고 = 화면의 "선배가 올린 공고")
- 신규 엔드포인트 ② `GET /api/v1/job-posts/crawled` — `GraduateJobPost` 매핑이 **없는** PostContents 만 (화면의 "공고 목록")
- 신규 응답 DTO `GraduatePostResponse` — 기존 `JobPostResponse` 필드 + 등록자(`Graduate.member.nickname`·`department`·`Graduate.jobType`·`careerYear`) 카드용 정보 포함
- 일반 공고는 기존 `JobPostResponse` 재사용
- 기존 `GET /api/v1/job-posts`(getList) 는 호환성 위해 유지하고 신규 화면은 분리 엔드포인트만 사용. 추후 deprecate 검토.

R-4. **리포지토리 보강**:

- `GraduateJobPostRepository` — `findDistinctPostContentsIds()` 또는 `findAllByPostContentsIdIn(...)` 형태 메서드 추가
- `PostContentsRepository` — `findAllByIdNotIn(Collection<Long>)` 사용 (없으면 추가)

**데이터 일관성 메모**: 데이터팀의 `upload_jobs.py` (크롤링 → 가짜 선배 적재, 추천 알고리즘용 더미 시드) 와 `routers/crawling.py` (`POST /api/data/crawling/sync`, 운영 sync — PostContents 만 적재) 가 적재 방식이 다르다. 분리 기준이 매핑 유무이므로 두 흐름이 혼재해도 R-3 가 정상 동작하나, 운영상 데이터팀과 적재 정책 합의 권장 — 본 TODO 범위 외.

**수정 파일**:

- `job_post/dto/request/CreateJobPostLinkRequest.java` 신규
- `job_post/dto/response/GraduatePostResponse.java` 신규
- `job_post/service/JobPostService.java`·`JobPostServiceImpl.java` — `createFromLink`, `getGraduatePosts`, `getCrawledPosts` 추가 및 `create` 의 null 가드
- `job_post/controller/JobPostController.java`·`JobPostSwagger.java` — 엔드포인트 3개 추가
- `job_post/domain/repository/GraduateJobPostRepository.java`·`PostContentsRepository.java` — 쿼리 메서드 추가
- 관련 테스트 보강

**연관**: TODO L 의 `UpdateGraduateMyPageRequest.jobPostsToAdd` 타입과 일치시켜야 함.

**출처**: 마이페이지 "나의 공고 올리기" 모달(3번째 사진) + 구직 정보 화면(4번째 사진) + `ddingconnect-data` 의 `master_crawler.py`·`routers/crawling.py`·`upload_jobs.py` 분석.

### 11개 작업자 노트 (TODO #1~#11 머지 완료 후 보존되는 일반 가이드)

- **브랜치 정책**: 각 TODO 를 **개별 브랜치 + 개별 PR** 로 처리하는 것을 기본으로 한다. 영향 범위가 큰 TODO 는 단독 PR 필수. 같은 도메인 내 작은 변경은 묶어서 1개 PR 도 허용 (작업자 판단). 사용자가 "TODO N 작업" 으로 단일 항목 지목 시 그 항목만 단독 PR.
- **공통 커밋 메시지 컨벤션**: `feat(<도메인>): ...` / `fix(<도메인>): ...` / `refactor(<도메인>): ...` / `docs(CLAUDE.md): ...` prefix. 마지막 줄에 항상 `https://claude.ai/code/session_...` 포함.
- **Swagger 검증**: 가능하면 PR 본문 Test plan 에 Swagger 시나리오 체크박스 포함.
- **테스트 실패 시**: `--no-verify` 등으로 우회 금지. 원인 분석 후 수정.
