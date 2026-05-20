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

### TODO G: 로드맵 전체 목록 조회 API(`getRoadmaps`) 제거 (roadmap 도메인)

**문제**: `RoadmapController.getRoadmaps` (`GET /api/v1/roadmaps`) 와 호출 체인 `RoadmapService.getList` / `RoadmapServiceImpl.getList` 가 `roadmapRepository.findAll()` 로 **전 회원의 로드맵을 무조건 전부** 반환한다 — `@LoginMember` 인증·회원 스코프 필터·페이징 없음. 로드맵 플로우(프론트 폼 입력 → 데이터 파트 AI 생성 → `Roadmap.content` 저장 → 상세 조회)는 등록(`createRoadmap`)·상세 조회(`getRoadmap`)만 사용하며 `getRoadmaps` 는 어느 단계에서도 호출되지 않는다. `backend.md` 화면 매핑에도 "전체 로드맵 목록" 화면이 없다 (마이페이지는 `countMyRoadmaps` 로 개수만 표시).

**디폴트 결정**: 아래 4곳을 제거한다 — `RoadmapController.getRoadmaps` 엔드포인트, `RoadmapSwagger.getRoadmaps`, `RoadmapService.getList`, `RoadmapServiceImpl.getList`. `RoadmapRepository` 는 손대지 않는다(`getList` 가 쓰던 `findAll` 은 JpaRepository 기본 메서드). `RoadmapControllerTest` 에 목록 조회 케이스가 있으면 함께 정리하고, 본문 도메인 문서(`backend.md` 로드맵 섹션)의 CRUD 설명을 4종 → 3종으로 갱신.

**주의**: 제거 전 프론트에 `GET /api/v1/roadmaps` 호출부가 없는지 최종 확인. 본인 로드맵 목록 화면이 추후 필요하면 `findAll` 이 아닌 `findByMemberId` 기반 **회원 스코프 + `@LoginMember`** 조회로 신설할 것 — 전체 무인증 노출 API 형태로 부활 금지.

**출처**: 로드맵 백엔드↔데이터 파트 연동 플로우 분석 세션 — 생성 플로우 미사용 API 로 식별, 사용자 지시로 제거 확정.

### TODO H: 로드맵 백엔드↔데이터 파트 연동 — 생성 플로우 완성 (roadmap 도메인, 크로스 파트)

**문제**: 로드맵 생성 플로우(프론트 폼 입력 → AI 생성 → DB 저장 → 상세 조회)가 두 레포에 걸쳐 끊겨 있다. 백엔드 `RoadmapServiceImpl.create()` 는 프론트가 보낸 `content` 문자열을 그대로 저장만 하고 AI 를 호출하지 않으며, 데이터 파트 `POST /api/data/generate` 는 독립적으로 AI 생성 후 자기 코드로 DB 에 저장한다. ① 백엔드→데이터 파트 호출 코드가 없고, ② 저장이 양쪽에서 일어나 이원화되며, ③ `/generate` 응답에 DB row `id` 가 없어 프론트가 상세 조회(`GET /api/v1/roadmaps/{id}`)할 id 를 알 수 없다.

**디폴트 결정 (백엔드 중심)**: 데이터 파트는 AI 생성만 담당하고 저장·조회·인증·알람은 백엔드가 맡는다. `POST /api/v1/roadmaps` 가 입력 6필드 수신 → 데이터 파트 `/generate` 호출 → AI 결과를 `Roadmap.content` 에 저장 → `RoadmapResponse(id, …)` 반환 → 프론트는 그 `id` 로 상세 조회. 이러면 id 갭·DB 이원화가 동시에 해소된다. 백엔드(`cluade_clone`) 작업:

1. **데이터 파트 호출 클라이언트 신규** — `RestClient` 로 `POST {data.base-url}/api/data/generate` 호출. 커피챗 `MatchingAlgorithmClient` 패턴 재사용, base URL 설정값화(`application.yml`). 호출 시 `member.getId()` 를 `X-User-Id` 헤더로 전달.
2. **`CreateRoadmapRequest` 교체** — `content` → `grade·gpa·major·targetJob·currentSkills·targetCompany` 6필드. `RoadmapSwagger` 의 `@ExampleObject` 동기화.
3. **`RoadmapServiceImpl.create()` 재구성** — "요청 content 저장" → "데이터 파트 호출 → 응답 결과를 `content` 에 저장". `validateContent` 대상을 응답값으로 변경. 기존 `RoadmapAlarm`·SSE 알람 발행 로직은 유지.
4. **ENUM 정합성 확인** — `targetJob`/`currentSkills` 값이 데이터 파트 `TargetJobCategory`(11종)·`TechStackName`(24종)과 일치하는지 검증.

**미확정 / 선택**: 입력 6필드를 `Roadmap` 엔티티 컬럼(또는 별도 엔티티)으로 저장할지 여부. 데이터 파트 생성 결과(`content`)에는 입력 원본이 없어, 재생성·입력 이력·수정 화면 프리필이 필요하면 저장, 결과만 보면 되면 미저장 — 제품 요구 확정 후 결정.

**주의 — 크로스 파트**: 데이터 파트(`ddingconnect-data`, 별도 레포)의 `roadmap_router.py` 자체 `db.add/commit` 제거(생성 결과만 반환)가 함께 필요하나, **이 TODO 범위 밖이며 데이터 파트 담당자와 협의** 후 진행한다. `/generate` 의 IP 기준 `3/day` rate limit 은 백엔드가 단일 IP 로 호출하면 전체 사용자가 공유하게 되므로 연동 시 제한 정책 재검토 필요. 두 서비스가 같은 물리 DB 를 보는지도 확인.

**출처**: 로드맵 백엔드↔데이터 연동 플로우 분석 세션 (저장 설계 = 백엔드 중심으로 확정).

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

### TODO K: 커피챗 매칭 요청 스키마 양방향 페어 변환 (coffeechat 도메인, TODO J 후속)

**문제**: `MatchingAlgorithmClientImpl.AlgorithmMatchRequest` 가 단방향(`{requesterId, grade, gpa, major, interestedJob, capability, targetCompany}`) 으로 폼 6필드 + 신청자 ID 만 전송한다. 그러나 데이터 파트(`POST /api/data/coffeechat/match`) 는 양방향 페어 `{requester: UserInfo, receiver: UserInfo}` 를 기대하며 (`UserInfo` = `user_id, job, tech_stacks, goal`), 백엔드가 후보(receiver) 정보를 보내지 않아 알고리즘에 비교 대상 자체가 없다. TODO J(경로 정정) 가 머지돼도 요청 스키마 불일치로 422 가 난다.

**디폴트 결정 (학생 → 졸업생 매칭만 다룸)**: 다음 3단계 구현.

1. **졸업생 후보 풀 조회 쿼리 신규** — `MemberRepository` 에 역할 필터 메서드(예: `findAllByRole(MemberRole.GRADUATE)`) 추가. 신청자 본인 제외. 매칭용 데이터가 비어 있는 후보(`Graduate.jobType` null, `TechStack` 0건, `Graduate.company` blank) 는 풀에서 제외 — 이전 합의("빈 값 후보 제외").
2. **폼·DB → `UserInfo` 매핑** — 알고리즘 양식 변환:
   - **요청자(학생) 측 (폼)**: `user_id = requesterId`, `job = form.interestedJob`, `tech_stacks = form.capability` 콤마 split → `List<String>` (`trim` + `TechStackName` 화이트리스트 대문자 정규화), `goal = form.targetCompany`.
   - **후보(졸업생) 측 (DB)**: `user_id = graduate.member_id`, `job = graduate.jobType.name()`, `tech_stacks = TechStackRepository.findByMemberId(graduate.member_id).stream().map(name).toList()`, `goal = graduate.company`.
3. **페어별 호출 오케스트레이션** — `AlgorithmMatchRequest` 를 `{requester: UserInfo, receiver: UserInfo}` 양방향 record 로 재설계. 데이터 파트가 1대1 페어만 받으므로 `MatchingAlgorithmClient.topMatches` 내부에서 후보 N명 만큼 `POST /api/data/coffeechat/match` 를 N회 호출 (배치 미지원). 호출 결과(점수)의 정렬·top N 추출은 TODO L 에서 처리.

**범위 — 학생 → 졸업생 방향 한정**: 졸업생 → 학생 매칭(반대 방향) 은 본 TODO 범위 밖이며, 진행 시 별도 TODO 로.

**주의**: 폼 `capability` 가 자유 텍스트라 콤마 split·정규화 견고하지 않다 — `TechStackName` enum 24종에 매칭 안 되는 토큰은 버리는 정책 권장. 매핑 결과가 0개면 알고리즘 ability 점수가 0 으로 떨어지므로, 요청자 측 변환 결과 빈 리스트 시 즉시 400 으로 반환할지 그대로 호출할지 결정 필요.

**문서 동기화**: `backend.md` 의 `### 커피챗` 매칭 알고리즘 연동 설명을 "단방향 → 양방향 페어 + 후보 N회 호출 + 백엔드 정렬" 로 갱신. `data.md` 의 "백엔드 연동 시 주의 — 계약 불일치" 중 "현재 구현 불일치"·"네이밍 불일치"·"엔드포인트 경로(머지 시점 따라)" 항목 정리.

**출처**: 커피챗 매칭 플로우 분석 세션 — TODO J 와 함께 발견된 4건 불일치 중 요청 스키마 1건 분리.

### TODO L: 커피챗 매칭 응답 스키마 점수 → memberIds 변환 (coffeechat 도메인, TODO J 후속)

**문제**: `MatchingAlgorithmClientImpl.AlgorithmMatchResponse` 는 `List<Long> memberIds` 를 기대하지만, 데이터 파트는 한 페어의 점수 dict `{status: "success", match_results: {jobScore, ability, goal, totalMatchRate}}` 를 반환한다. 응답 구조가 다르고 정렬·top N 선택도 데이터 파트가 수행하지 않으므로 백엔드가 직접 정렬해 후보 ID 리스트로 변환해야 한다.

**디폴트 결정**: 다음 2단계 구현.

1. **응답 record 재설계** — `AlgorithmMatchResponse` 를 `{status: String, matchResults: MatchScores}` 로 변경. `MatchScores = {jobScore: double, ability: double, goal: double, totalMatchRate: double}`. 응답 키 네이밍 혼용(`ability`·`goal` 은 소문자, `jobScore`·`totalMatchRate` 는 camelCase) 대응 — 필드별 `@JsonProperty` 명시 권장.
2. **백엔드 측 정렬·top N 추출** — TODO K 의 페어 호출 루프 결과를 `Map<Long candidateId, Double totalMatchRate>` 로 모은 뒤 `totalMatchRate DESC` 정렬, 상위 N 명의 `candidateId` 만 `List<Long> memberIds` 로 반환. N 은 설정값 `matching.algorithm.top-n`(`application.yml`, 기본 3) 으로 외부화 — 하드코딩 금지 규칙 정합.

**범위**: 응답 매핑 + 정렬·top N 추출만. 점수 자체는 응답 DTO 에 노출하지 않는다(`CoffeeChatMatchingServiceImpl` 가 `memberIds` 만 사용하는 현재 흐름 유지). 점수 0 후보 필터링은 추후 필요 시 추가.

**주의**: TODO K 와 같은 PR 로 묶어도 되고 분리해도 된다 — 둘 다 머지돼야 매칭 결과 화면이 실제 렌더링되므로 함께 진행이 자연스럽다. 작업자 판단.

**문서 동기화**: `backend.md` 의 `### 커피챗` 매칭 알고리즘 연동 설명에 "백엔드가 점수 받아 정렬·top N 선택" 명시.

**출처**: 커피챗 매칭 플로우 분석 세션 — TODO J·K 와 함께 발견된 4건 불일치 중 응답 스키마 1건 분리.

### 11개 작업자 노트 (TODO #1~#11 머지 완료 후 보존되는 일반 가이드)

- **브랜치 정책**: 각 TODO 를 **개별 브랜치 + 개별 PR** 로 처리하는 것을 기본으로 한다. 영향 범위가 큰 TODO 는 단독 PR 필수. 같은 도메인 내 작은 변경은 묶어서 1개 PR 도 허용 (작업자 판단). 사용자가 "TODO N 작업" 으로 단일 항목 지목 시 그 항목만 단독 PR.
- **공통 커밋 메시지 컨벤션**: `feat(<도메인>): ...` / `fix(<도메인>): ...` / `refactor(<도메인>): ...` / `docs(CLAUDE.md): ...` prefix. 마지막 줄에 항상 `https://claude.ai/code/session_...` 포함.
- **Swagger 검증**: 가능하면 PR 본문 Test plan 에 Swagger 시나리오 체크박스 포함.
- **테스트 실패 시**: `--no-verify` 등으로 우회 금지. 원인 분석 후 수정.
