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
│   ├── alarm/           # 4종 알람 통합
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
    ├── config/          # Security, Swagger, JpaAuditing, WebMvc
    ├── jwt/             # JwtUtil(Impl)
    └── response/        # ApiResponse, ErrorStatus, ExceptionAdvice
```

각 도메인은 `controller / service / domain(entity+repository) / dto` 4계층으로 일관됨.

## 핵심 도메인 규칙

### 회원 (Member)
- `MemberRole`: `UNKNOWN | STUDENT | GRADUATE`
- 회원가입 시 role에 따라 `Student` 또는 `Graduate` 레코드를 함께 생성 (`AuthServiceImpl.createRoleRecord`)
- 탈퇴는 `isDeleted` 플래그 (soft delete)

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

### 관심 직군 (interested_job)
- 학생 마이페이지의 **관심 직군 칩** — 회원 ↔ `TargetJobCategory` enum 단순 매핑. `PostContents`(구직 공고)와는 FK 관계 **없음**.
- 등록 시 필요한 입력은 카테고리(`interestedJob`) 하나뿐. 공고 ID 같은 FK는 받지 않음.
- `TargetJob` 엔티티는 `member_id`(NOT NULL FK) + `Interested_Job`(enum) + `key2`(미사용, ERD 잔재) 로 구성.
- 본인 소유자만 수정/삭제 (`TARGET_JOB_UNAUTHORIZED`).
- `TargetJobCategory` 와 `JobType` 은 같은 원티드 직군 태그 소스에서 11개 동일 값을 받지만 **타입 분리**. 두 enum 의 값 정렬은 사람이 직접 유지해야 함.
- 공고 ↔ 관심 직군 매칭(예: "내 관심 카테고리에 해당하는 새 공고 알람")이 필요해지면, FK 가 아닌 `PostContents.jobType` 과 `TargetJob.interestedJob` 의 **enum 값 비교**로 처리. 이 매칭이 두 enum 이 같은 값을 갖는 본질적 이유.
- **(보류) 두 enum 통합 검토**: 같은 원티드 taxonomy 에서 받는 값이므로 `TargetJobCategory` + `JobType` → 단일 `JobCategory` 로 통합하면 `==` 매칭이 가능해지고 sync 부담이 사라짐. 현재는 의도적으로 보류 — 원티드 API 의 런타임 동적 연동 구조(static enum vs `String` + 화이트리스트 vs 참조 테이블)까지 함께 결정한 뒤 진행. 보류 동안 두 enum 의 값 정렬은 사람이 수동으로 유지.

### 커피챗 (coffeechat)
- 흐름: 요청자가 카카오 오픈채팅 링크 포함해 생성 (`PENDING`) → 수신자만 `ACCEPTED`/`REJECTED`로 상태 변경
- 알람 발행 규칙:
  - 생성(PENDING): 수신자에게 1건
  - ACCEPTED: **요청자/수신자 양쪽**에 카카오 링크 포함 알람 2건
  - REJECTED: 요청자에게만 1건
- 삭제(취소)는 요청자만 가능
- **삭제 캐스케이드 (서비스 레벨)**: `CoffeeChatServiceImpl.delete` 에서 `CoffeeChatAlarm` 먼저 `deleteByCoffeeChatId` 로 정리 → `CoffeeChat` 삭제. `CoffeeChatAlarm.coffeeChat` 가 `nullable = false` FK 라 정리 없이는 MySQL FK constraint 위반.

### 통합 알람 (alarm)
- 4종(`AnswerAlarm`, `JobAlarm`, `RoadmapAlarm`, `CoffeeChatAlarm`)을 합쳐 단일 `AlarmResponse` 리스트로 반환
- 정렬: `createdAt DESC`, null은 뒤로
- 읽음 처리는 Builder 패턴으로 새 인스턴스 만들어 저장 (불변 스타일)
- 각 타입별 소유자 검증 헬퍼 분리 (`verify*AlarmOwner`)
- **알람 발행 위치 = 각 도메인 `*ServiceImpl` 의 본체 save 직후, 같은 `@Transactional` 안** (이벤트/AOP 미사용). 본체 저장 실패 시 알람도 롤백되어 원자성 보장.
  - `AnswerAlarm` — `AnswerServiceImpl.create()`, 질문 작성자에게 발행. 단, **본인이 본인 질문에 답변한 경우는 미발행** (자기 자신 알람 방지).
  - `RoadmapAlarm` — `RoadmapServiceImpl.create()`, **본인(생성자)** 에게 발행.
  - `JobAlarm` — `JobPostServiceImpl.create()`, `PostContents.jobType` 과 `TargetJob.interestedJob` 이 같은 학생들에게 N건 발행. **등록한 졸업생 본인 제외**, 같은 멤버가 동일 카테고리 중복 보유 시 1건만. enum 매칭은 `TargetJobCategory.valueOf(jobType.name())` 으로 (CLAUDE.md `interested_job` 의 값 매칭 방침과 정합).
  - `CoffeeChatAlarm` — `CoffeeChatServiceImpl.create()` / `updateStatus()`, PENDING 1건(수신자), ACCEPTED 2건(요청자+수신자, 카카오링크), REJECTED 1건(요청자).
- **상대 시간 표시 (`AlarmResponse.relativeTime`)**: 응답 변환 시점에 `RelativeTimeFormatter.format(createdAt)` 으로 계산 ("방금 전 / N분 전 / N시간 전 / N일 전 / N개월 전 / N년 전"). 매 조회마다 재계산되어 시간 흐름에 따라 자연스럽게 업데이트됨. `createdAt` 도 함께 응답에 포함 (프론트 자체 포맷팅 여지 보존).

### Q&A
- `Question` 1:N `Answer`
- 좋아요: `QuestionLike`, `AnswerLike` (복합키 `AnswerLikeId`), toggle 방식
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
| 홈/알람 패널 | `alarm` (4종 통합) |
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

> 사용자가 "to-do 리스트 수행" / "to-do 진행" / "TODO #N 작업" 등 유사 명령을 내리면 아래 항목을 **즉시 코드 작성 → 테스트 보강 → CLAUDE.md 갱신 → 커밋/푸시 → PR 생성까지 일사천리로** 수행한다. 결정 사항은 이미 확정돼 있으니 다시 묻지 말고 명시된 디폴트로 진행할 것.
>
> 각 항목은 완료(머지) 후 이 섹션에서 삭제하거나 "완료" 로 표시하고, 본문 도메인 섹션에 정식 규칙으로 통합 기록한다.

### TODO #1: 구직 공고 jobType 변경 시 JobAlarm 디스패치

**배경**
현재 `JobPostServiceImpl.create()` 에서만 `JobAlarm` 을 발행함. `update()` 로 `jobType` 이 바뀌면 알람 변화가 반영되지 않음. update 시점에:
- 기존 알람을 받았지만 새 `jobType` 에 매칭 안 되는 멤버 → "벗어남" 알람 추가
- 매칭되지 않았다가 새 `jobType` 에 매칭되는 멤버 → "새 공고" 알람 추가
가 동작해야 함.

**확정된 결정 사항 (재논의 금지)**

| # | 결정 |
|---|---|
| 1 | Removed 유저의 **기존 `JobAlarm` row 는 그대로 유지**. 새 "벗어남" 알람을 추가 INSERT. (삭제·갱신 아님) |
| 2 | Removed 알람 `content` = `"관심 직군에서 벗어난 공고로 변경되었습니다."` |
| 3 | Added 알람 `content` = `"관심 직무에 새로운 공고가 등록되었습니다."` (POST 시 메시지와 동일) |
| 4 | **prev 집합** = `jobAlarmRepository.findByPostContentsId(jobPostId)` 결과에서 멤버 ID 기준 중복 제거. **`JobAlarm` 만** 본다 — 다른 알람 타입(`CoffeeChatAlarm`/`AnswerAlarm`/`RoadmapAlarm`) 무관, 다른 공고의 `JobAlarm` 무관. 멤버가 이 공고에 대한 `JobAlarm` 을 1건이라도 가지면 prev. |
| 5 | `jobType` 이 안 바뀐 경우(`newType == oldType`) → 아무것도 안 함 (현재 update 동작 유지) |
| 6 | 등록 졸업생 본인(`creatorId`) 은 prev/curr 어느 쪽에도 포함되지 않게 **항상 제외** |
| 7 | enum 매칭은 `TargetJobCategory.valueOf(newJobType.name())` — 기존 패턴 동일 |

**구현 단계**

#### Step 1: `JobAlarmRepository.findByPostContentsId(Long)` 추가
파일: `src/main/java/mju/capstone/ddingconnect/domain/job_post/domain/repository/JobAlarmRepository.java`

기존 메서드 옆에 추가:
```java
List<JobAlarm> findByPostContentsId(Long postContentsId);
```

#### Step 2: `JobPostServiceImpl.update()` 에 알람 디스패치 로직
파일: `src/main/java/mju/capstone/ddingconnect/domain/job_post/service/JobPostServiceImpl.java`

`update()` 메서드 흐름:
1. `oldJobType = postContents.getJobType()` 을 빌더 호출 **전에** 추출 (영속 엔티티의 값)
2. `newJobType` 변수 추출 = `request.jobType() != null ? request.jobType() : postContents.getJobType()`
3. 기존 빌더로 `updated` 생성, `save(updated)` 호출 후 `saved` 변수에 보관
4. `if (newJobType != oldJobType)` 일 때만 `dispatchJobTypeChangeAlarms(saved, member.getId())` 호출
5. `return JobPostResponse.from(saved);`

private 헬퍼 `dispatchJobTypeChangeAlarms(PostContents post, Long creatorId)` 시그니처:
- `existing = jobAlarmRepository.findByPostContentsId(post.getId())` 호출
- `prevIds` = `existing` 에서 `member.id` 추출한 `Set<Long>` (자동 중복 제거)
- `prevMemberById` = `existing` 의 `Map<Long, Member>` (멤버 ID → Member, 중복 시 첫 값 유지)
- `newCategory = TargetJobCategory.valueOf(post.getJobType().name())`
- `matched = targetJobRepository.findByInterestedJob(newCategory)`
- `currIds`, `currMemberById` 를 구축하면서 `creatorId` 와 같으면 건너뛰고 중복 제거
- Removed 루프: `prevIds` 의 각 `mid` 에 대해 `currIds` 에 없고 `creatorId` 아니면 "벗어남" 알람 발행 (`prevMemberById.get(mid)` 사용)
- Added 루프: `currIds` 의 각 `mid` 에 대해 `prevIds` 에 없으면 "새 공고" 알람 발행 (`currMemberById.get(mid)` 사용)
- 알람 빌드 패턴은 `create()` 의 기존 코드 그대로 (isRead=false)

필요 import: `java.util.HashMap`, `java.util.HashSet`, `java.util.Map`, `java.util.Set`, `java.util.stream.Collectors`.

#### Step 3: 테스트 보강
파일: `src/test/java/mju/capstone/ddingconnect/domain/job_post/service/JobPostServiceImplTest.java`

추가 시나리오 4개 (이미 mock 으로 주입된 `jobAlarmRepository`, `targetJobRepository` 활용):

1. `update_jobType_변경없음_알람미발행`
   - `UpdateJobPostRequest.jobType()` 을 `null` 로 두거나 기존 `BACKEND` 와 같은 값으로 호출
   - `jobAlarmRepository.save(JobAlarm)` 호출 0회, `findByPostContentsId` 호출 0회 검증

2. `update_jobType_변경_removedOnly`
   - 기존 jobType = BACKEND, 새 jobType = FRONTEND
   - prev = [멤버 A (BACKEND)], curr = [] (FRONTEND 매칭 없음)
   - 결과: A 에게 "벗어남" 알람 1건만 발행. `content` 가 "벗어난" 포함되는지 캡쳐 검증

3. `update_jobType_변경_addedOnly`
   - 기존 jobType = BACKEND, 새 jobType = FRONTEND
   - prev = [], curr = [멤버 B (FRONTEND)]
   - 결과: B 에게 "새 공고" 알람 1건만. `content` 가 "새로운 공고" 포함 검증

4. `update_jobType_변경_mixed`
   - prev = {A, B, C}, curr = {B, C, D}
   - 결과: A 에게 Removed 1건, D 에게 Added 1건, B/C 는 미발행
   - `ArgumentCaptor<JobAlarm>` 로 발행된 알람 멤버 ID 세트 검증

스텁: `when(jobAlarmRepository.findByPostContentsId(...)).thenReturn(...)`, `when(targetJobRepository.findByInterestedJob(...)).thenReturn(...)`.

#### Step 4: CLAUDE.md 갱신
파일: `CLAUDE.md`, `### 구직 공고 (job_post)` 섹션에 다음 bullet 추가:

> - **update 시 jobType 변경 알람 디스패치 (`JobPostServiceImpl.update`)**: `oldJobType != newJobType` 일 때, 이 공고의 기존 `JobAlarm` 수신자 (= prev) 와 새 `jobType` 매칭 학생 (= curr) 의 차집합을 계산해 두 종류 알람을 발행. **Removed = prev − curr** → `"관심 직군에서 벗어난 공고로 변경되었습니다."`, **Added = curr − prev** → `"관심 직무에 새로운 공고가 등록되었습니다."`. 기존 알람 row 는 보존(삭제·갱신 X). 등록 졸업생 본인 항상 제외. prev 집합 정의는 이 공고(`postContentsId`)에 연결된 `JobAlarm` row 만 본다 (다른 알람 타입/다른 공고는 무관).

이 작업 완료 후 본 To-do 항목(TODO #1)은 이 To-do 섹션에서 삭제.

#### Step 5: 커밋, 푸시, PR 생성

- **브랜치**: 새 브랜치 `claude/feat-jobtype-change-alarm` 권장 (또는 기존 `claude/fix-registration-api-0WsYp` 재사용 가능 — 작업자 판단)
- **커밋 메시지** (예시):
  ```
  feat(job_post): update 시 jobType 변경에 따른 JobAlarm Removed/Added 디스패치

  TODO #1 (CLAUDE.md). PR #11 에서 추가한 POST 시점 알람과 대응해 update
  시점에도 매칭 변화 알람을 발행. 기존 알람은 보존, 새 알람만 추가.

  - JobAlarmRepository.findByPostContentsId 추가
  - JobPostServiceImpl.update 에서 jobType 변경 감지 → 디스패치 헬퍼 호출
  - 4가지 시나리오 테스트 (변경없음/RemovedOnly/AddedOnly/Mixed)
  - docs(CLAUDE.md): TODO #1 항목 제거 + 구직 공고 섹션에 정식 규칙 통합
  ```
- **PR 제목** (예시): `feat(job_post): update 시 jobType 변경 → Removed/Added JobAlarm 디스패치`
- **PR 본문**: 결정 사항 표, 테스트 케이스 목록, Swagger 검증 항목 포함
- **Swagger 검증 항목** (PR 본문에 미체크박스로 포함):
  - jobType 안 바꾸고 update → 새 알람 0건
  - jobType BACKEND → FRONTEND 변경:
    - 이전 BACKEND 관심 학생(FRONTEND 미관심) → "벗어남" 알람 받음
    - 이전 FRONTEND 관심 학생(BACKEND 미관심) → "새 공고" 알람 받음
    - BACKEND + FRONTEND 둘 다 관심 학생 → 새 알람 없음
    - 등록 졸업생 본인이 두 카테고리 다 관심으로 등록해도 본인은 알람 없음

#### 작업자 노트
- 로컬 테스트 실행은 Gradle wrapper 다운로드 실패(503)로 안 될 가능성 높음. 작성한 단위 테스트가 컴파일 가능하면 그대로 PR. CI 또는 머지 후 수동 검증.
- PR 생성 후 작업자가 활동 모니터링 (`subscribe_pr_activity`) 켤지는 사용자 지시에 따름.
