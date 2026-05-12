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
- **update 시 jobType 변경 알람 디스패치 (`JobPostServiceImpl.update`)**: `oldJobType != newJobType` 일 때, 이 공고의 기존 `JobAlarm` 수신자 (= prev) 와 새 `jobType` 매칭 학생 (= curr) 의 차집합을 계산해 두 종류 알람을 발행. **Removed = prev − curr** → `"관심 직군에서 벗어난 공고로 변경되었습니다."`, **Added = curr − prev** → `"관심 직무에 새로운 공고가 등록되었습니다."`. 기존 알람 row 는 보존(삭제·갱신 X). 등록 졸업생 본인 항상 제외. prev 집합 정의는 이 공고(`postContentsId`)에 연결된 `JobAlarm` row 만 본다 (다른 알람 타입/다른 공고는 무관).

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

_(아래 TODO #1 ~ #11 는 회원(member) 4개 + 기술 스택 1개 + 관심 직군 1개 + 커피챗 2개 + Q&A 3개 이슈. 각 항목 완료 후 본문 `### 회원 (Member)` / `### 기술 스택 (techstack)` / `### 커피챗 (coffeechat)` / `### Q&A` 섹션 등에 정식 규칙으로 통합하고 이 섹션에서 제거.)_

### TODO #1: 회원 탈퇴 시 관련 DB 데이터 전부 hard delete

**배경**
현재 `MemberServiceImpl.withdraw()` 는 `isDeleted = true` 플래그만 세팅하는 soft delete. 사용자 요구 = 탈퇴 시 해당 회원이 만든·받은 모든 자식 행 + 회원 row 자체까지 즉시 삭제.

**확정된 결정 사항 (재논의 금지)**

| # | 결정 |
|---|---|
| 1 | `Member.isDeleted` 컬럼/필드는 **유지**, 기본 `false` 고정. DB 스키마는 그대로 두고 코드 동작만 hard delete 로 전환 (향후 차단/정지 등 다른 용도 보존 여지) |
| 2 | 회원이 작성한 부모 row 는 **기존 도메인 서비스의 `delete()` 호출로 위임** — `Question/Answer/Roadmap/CoffeeChat/JobPost` 서비스에 이미 자식 캐스케이드가 구현돼 있어 재사용 |
| 3 | 회원이 **받기만 한** leaf 알람/좋아요 row 는 `MemberServiceImpl` 가 각 레포에 새로 추가하는 `deleteByMemberId(Long)` 로 일괄 삭제 |
| 4 | `TechStack`, `TargetJob` 본인 row 도 `deleteByMemberId` 로 직접 삭제 |
| 5 | `Student`/`Graduate` 매핑 row 는 위 단계가 끝난 뒤 마지막으로 정리 |
| 6 | `Member` 본인 row 마지막에 `delete()` 호출 — DB 에서 완전히 제거 |
| 7 | 전체는 단일 `@Transactional` 안에서, 중간 실패 시 전부 롤백 |
| 8 | `AuthServiceImpl` 의 회원가입 시 `.isDeleted(false)` 명시 호출은 유지 (의도적) |
| 9 | `JwtAuthenticationFilter` 는 이미 `memberRepository.findById` 결과가 없으면 인증 실패 — hard delete 후 자동으로 토큰 무효화됨. 추가 작업 불필요 |
| 10 | 탈퇴 응답 메시지/엔드포인트(`DELETE /api/v1/members/me`) 는 변경 없음 |

**구현 단계**

#### Step 1: leaf 알람 레포에 `deleteByMemberId(Long)` 추가
- `AnswerAlarmRepository`
- `JobAlarmRepository` (이미 `deleteByPostContentsId` 있음)
- `RoadmapAlarmRepository`
- `CoffeeChatAlarmRepository`

Spring Data 쿼리 메서드 1줄씩.

#### Step 2: 좋아요 레포에 `deleteByMemberId(Long)` 추가
- `QuestionLikeRepository.deleteByMemberId(Long)`
- `AnswerLikeRepository.deleteByMemberId(Long)` (복합 PK 지만 member_id 컬럼 자체는 단일이라 메서드 직접 추가 가능)

#### Step 3: `TargetJobRepository.deleteByMemberId(Long)` 추가
(`TechStackRepository.deleteByMemberId` 는 이미 존재)

#### Step 4: `MemberServiceImpl` 생성자 주입 확장
- `QuestionService`, `AnswerService`, `RoadmapService`, `CoffeeChatService`, `JobPostService`
- 위 step 1~3 의 모든 새 레포: `AnswerAlarmRepository`, `JobAlarmRepository`, `RoadmapAlarmRepository`, `CoffeeChatAlarmRepository`, `QuestionLikeRepository`, `AnswerLikeRepository`, `TargetJobRepository`, `TechStackRepository`
- 추가로 회원이 만든 부모 row 를 조회할 레포: `QuestionRepository`, `AnswerRepository`, `RoadmapRepository`, `CoffeeChatRepository`, `GraduateJobPostRepository`
- 순환 의존 점검: QnA/CoffeeChat/Roadmap/JobPost 의 서비스들이 `MemberService` 를 주입받지 않으면 안전 — 사전 확인 필수

#### Step 5: `withdraw()` 본체 재작성
순서 (각 단계 같은 `@Transactional` 안):
1. 회원이 작성한 `CoffeeChat` 목록을 requester/receiver 양쪽으로 조회 후 `coffeeChatService.delete(member, id)` 위임 (소유자 검증 우회를 위해 service.delete 가 아니라 직접 repo 정리로 변경할지 검토 — 현재 `CoffeeChatServiceImpl.delete` 는 요청자만 가능. 수신자만인 채팅은 부모를 자기가 못 지움 → 이 경우 `coffeeChatAlarmRepository.deleteByCoffeeChatId` 후 채팅 자체는 상대방이 지우거나, **여기서는 정책상 양쪽 다 지운다** — service.delete 대신 service 의 새 헬퍼 또는 직접 정리 사용)
2. 회원이 작성한 `Question` 전부 → `questionService.delete(member, id)` 위임 (질문 삭제가 손자까지 캐스케이드)
3. 회원이 작성한 `Answer` 전부 → `answerService.delete(member, id)` 위임
4. 회원이 만든 `Roadmap` 전부 → `roadmapService.delete(member, id)` 위임
5. GRADUATE 면 `graduateJobPostRepository.findByGraduate*` 로 본인이 등록한 공고 ID 모은 뒤 각각 `jobPostService.delete(member, id)` 호출 — `PostContents`/`GraduateJobPost`/`JobAlarm` 캐스케이드 재사용
6. 회원이 받은 leaf 정리:
   - `answerAlarmRepository.deleteByMemberId(memberId)`
   - `jobAlarmRepository.deleteByMemberId(memberId)`
   - `roadmapAlarmRepository.deleteByMemberId(memberId)`
   - `coffeeChatAlarmRepository.deleteByMemberId(memberId)`
   - `questionLikeRepository.deleteByMemberId(memberId)`
   - `answerLikeRepository.deleteByMemberId(memberId)`
7. 본인 소유 단순 row: `techStackRepository.deleteByMemberId`, `targetJobRepository.deleteByMemberId`
8. `Student` 또는 `Graduate` 매핑 row 삭제
9. `memberRepository.delete(member)` 로 회원 row 자체 제거

**주의**: step 1 의 CoffeeChat 정리는 `service.delete` 의 권한 검증이 수신자 케이스를 거부함. → `MemberServiceImpl` 에서 별도 흐름으로 처리:
```
List<CoffeeChat> all = coffeeChatRepository.findByRequesterId(memberId) ∪ findByReceiverId(memberId)
for each: coffeeChatAlarmRepository.deleteByCoffeeChatId(cc.id); coffeeChatRepository.delete(cc)
```
(coffeeChatService 우회. 알람 캐스케이드는 동일하게 보존.)

#### Step 6: 테스트
`MemberServiceImplTest`:
- `withdraw_정상_모든관련데이터삭제` — STUDENT 가 Q/A/Roadmap/CoffeeChat/TechStack/TargetJob 1건씩 보유한 상태에서 withdraw → 각 레포 delete 호출 검증 + member delete 호출 검증
- `withdraw_졸업생인_경우_본인등록공고와_그_자식들도_삭제` — GRADUATE 가 PostContents 2개 등록한 상태에서 jobPostService.delete 2회 호출 검증
- `withdraw_받기만한_알람도_정리` — 다른 회원의 부모에 대한 JobAlarm/AnswerAlarm 받은 상태에서 각 deleteByMemberId 호출 검증
- `withdraw_커피챗_수신자인_경우도_삭제` — 본인이 만들지 않았어도 수신자인 CoffeeChat 도 삭제되는지 검증

#### Step 7: CLAUDE.md 갱신
`### 회원 (Member)` 섹션:
- 기존 "탈퇴는 `isDeleted` 플래그 (soft delete)" bullet 제거
- 새 bullet 추가:
  > - **탈퇴 = hard delete (`MemberServiceImpl.withdraw`)**: 회원이 작성/소유/수신한 모든 자식 행을 단일 `@Transactional` 안에서 정리한 뒤 `Member` row 자체를 삭제. 위임 순서 = (1) 본인 작성 부모 row 들을 각 도메인 `delete()` 로 캐스케이드 위임 (Question/Answer/Roadmap, GRADUATE 면 JobPost) → (2) CoffeeChat 은 service 우회해 양방향(requester/receiver) 모두 정리 → (3) 본인 수신 leaf 알람·좋아요 `deleteByMemberId` → (4) TechStack/TargetJob 본인 row → (5) Student/Graduate 매핑 → (6) Member. `isDeleted` 컬럼은 스키마에 남기되 기본 `false` 고정 (다른 용도 보존).

또 `## 공통 패턴` 의 "삭제 캐스케이드" 관련 언급이 있다면 회원 hard delete 도 같은 패턴이라는 점 추가.

#### 작업자 노트
- 순환 의존 위험 — 현재 다른 서비스가 `MemberService` 를 의존하면 컴파일 실패. 작업 전 grep 으로 확인.
- 위임된 `Question/Answer/Roadmap/JobPost/CoffeeChat` 서비스의 `delete(member, id)` 가 회원 본인 권한을 요구함. 본인 row 만 삭제하므로 자연스럽게 통과 — 단, CoffeeChat 의 receiver 케이스만 우회 필요.

---

### TODO #2: GitHub / LinkedIn 링크 도메인 검증

**배경**
`UpdateMemberRequest.githubLink`, `linkedinLink` 가 그냥 `String` — 어떤 문자열이든 통과해 저장됨. `MemberServiceImpl.updateMyProfile` L52–53 도 null 체크만.

**확정된 결정 사항 (재논의 금지)**

| # | 결정 |
|---|---|
| 1 | github 링크: `^https?://(www\.)?github\.com/.+` 매칭 (`http`/`https` 둘 다 허용, `www.` optional, 경로 1자 이상) |
| 2 | linkedin 링크: `^https?://(www\.)?linkedin\.com/.+` 동일 규칙 |
| 3 | `null` 통과 (필드 미입력 = 변경 없음, 기존 patch 패턴 유지) |
| 4 | **빈 문자열 `""` 거부**. 링크 제거 메커니즘은 추후 별도 결정 (이 TODO 범위 밖) |
| 5 | 검증 실패 → 400. 새 에러코드 `MEMBER_INVALID_SOCIAL_LINK(BAD_REQUEST, "MEMBER400", "github 또는 linkedin 링크 형식이 올바르지 않습니다.")` |
| 6 | Bean Validation `@Pattern` 사용, 컨트롤러에 `@Valid` 추가 |
| 7 | regex 자체가 빈 문자열을 거부함 (최소 1자 매칭). 추가 `@NotBlank` 불필요. `@Pattern` 은 null 통과 기본 — 그대로 활용 |

**구현 단계**

#### Step 1: `UpdateMemberRequest` 에 `@Pattern` 추가
```java
@Pattern(regexp = "^https?://(www\\.)?github\\.com/.+",
        message = "github.com URL 만 허용됩니다.")
String githubLink,

@Pattern(regexp = "^https?://(www\\.)?linkedin\\.com/.+",
        message = "linkedin.com URL 만 허용됩니다.")
String linkedinLink,
```
필요 import: `jakarta.validation.constraints.Pattern`.

#### Step 2: `ErrorStatus` 에 `MEMBER_INVALID_SOCIAL_LINK` 추가
(현재 `MEMBER_UNAUTHORIZED` 만 있음)

#### Step 3: `MemberController.updateMyProfile` 에 `@Valid` 추가
```java
public ApiResponse<MemberResponse> updateMyProfile(
        @Parameter(hidden = true) @LoginMember Member member,
        @Valid @RequestBody UpdateMemberRequest request) { ... }
```
import 추가: `jakarta.validation.Valid`.

#### Step 4: `ExceptionAdvice` 점검
`MethodArgumentNotValidException` 핸들러가 이미 있으면 OK. 없다면 추가해서 `MEMBER_INVALID_SOCIAL_LINK` 와 매핑 (혹은 `_BAD_REQUEST` 로 매핑하고 message 에 violation 내용 포함).

#### Step 5: 테스트
`MemberControllerTest` 또는 `MemberServiceImplTest`:
- `update_githubLink_정상값_통과` (`https://github.com/honggildong`)
- `update_githubLink_www_허용` (`https://www.github.com/honggildong`)
- `update_githubLink_http_허용` (`http://github.com/honggildong`)
- `update_githubLink_도메인불일치_400` (`https://gitlab.com/foo`)
- `update_githubLink_빈문자열_400`
- `update_githubLink_경로없음_400` (`https://github.com/`)
- linkedin 동일 시나리오 1세트
- `update_링크_null_검증통과_기존값유지`

#### Step 6: CLAUDE.md 갱신
`### 회원 (Member)` 섹션에 다음 bullet:
> - **소셜 링크 검증 (`UpdateMemberRequest`)**: `githubLink` 는 `^https?://(www\.)?github\.com/.+`, `linkedinLink` 는 `^https?://(www\.)?linkedin\.com/.+` 패턴 매칭. `null` 통과(미입력), 빈 문자열·도메인 불일치는 400(`MEMBER_INVALID_SOCIAL_LINK`). 컨트롤러는 `@Valid` 필수.

---

### TODO #3: `grade` 값 검증 (음수/0 거부, >4 → 4 로 클램프)

**배경**
`Student.grade` 무검증. `UpdateMemberRequest.grade` 도 raw `Integer`. `MemberServiceImpl.updateMyProfile` L66 에서 그대로 저장 → `-5`, `0`, `99` 다 통과.

**확정된 결정 사항 (재논의 금지)**

| # | 결정 |
|---|---|
| 1 | `grade < 1` → 400 거부 (음수, 0 모두 포함) |
| 2 | `grade > 4` → 4 로 **클램프** (저장값 = `Math.min(raw, 4)`, 200 OK) |
| 3 | `grade == null` → 기존값 유지 (현재 동작 그대로) |
| 4 | 새 에러코드 `MEMBER_INVALID_GRADE(BAD_REQUEST, "MEMBER400", "학년은 1 이상이어야 합니다.")` |
| 5 | 검증/클램프 로직은 **서비스 레이어** (Bean Validation 만으로는 비대칭 규칙 표현 어려움) |
| 6 | 회원가입 시점(`AuthServiceImpl.createRoleRecord` 의 STUDENT 분기) 에도 동일 규칙 적용 — 단, 현 시점 회원가입 DTO 가 `grade` 를 받지 않으면 step 4 생략 (구현 시 확인) |

**구현 단계**

#### Step 1: `ErrorStatus` 에 `MEMBER_INVALID_GRADE` 추가

#### Step 2: `MemberServiceImpl` 에 private 헬퍼 추출
```java
private Integer sanitizeGrade(Integer raw, Integer fallback) {
    if (raw == null) return fallback;
    if (raw < 1) throw new MemberHandler(ErrorStatus.MEMBER_INVALID_GRADE);
    return Math.min(raw, 4);
}
```
(`MemberHandler` 가 없으면 만들기 — 현재 `MEMBER_UNAUTHORIZED` 만 있고 `MemberHandler` 부재 가능성. 점검.)

#### Step 3: `updateMyProfile` 의 STUDENT 분기에서 헬퍼 호출
```java
.grade(sanitizeGrade(request.grade(), student.getGrade()))
```

#### Step 4: 회원가입에도 적용 (해당되면)
`AuthServiceImpl.createRoleRecord` 의 STUDENT 분기에서 입력 grade 가 있으면 동일 헬퍼 호출. 회원가입 DTO 가 grade 안 받으면 step 생략.

#### Step 5: 테스트
- `update_grade_음수_400` (`-1`)
- `update_grade_0_400`
- `update_grade_1_통과` (경계값)
- `update_grade_4_통과` (경계값, 클램프 X)
- `update_grade_5_4로클램프` — `studentRepository.save` 캡쳐로 저장된 grade=4 검증
- `update_grade_99_4로클램프`
- `update_grade_null_기존값유지`

#### Step 6: CLAUDE.md 갱신
`### 회원 (Member)` 섹션에 다음 bullet:
> - **`Student.grade` 검증 (`MemberServiceImpl.sanitizeGrade`)**: `grade < 1` → 400(`MEMBER_INVALID_GRADE`). `grade > 4` → 4 로 클램프(`Math.min`)해서 저장. `null` 은 기존값 유지. 회원가입(`AuthServiceImpl.createRoleRecord`) 도 동일 규칙 적용.

---

### TODO #4: STUDENT/GRADUATE 역할 외 필드 거부 (silent ignore → loud error)

**배경**
`UpdateMemberRequest` 가 STUDENT 전용(`grade`) + GRADUATE 전용(`businessCardImage`, `company`, `careerYear`) 필드를 모두 포함. 현재 `MemberServiceImpl.updateMyProfile` 는 `member.getRole()` 분기로 본인 역할 외 필드를 **조용히 무시** → 200 OK 인데 미반영. API 계약 모호.

**확정된 결정 사항 (재논의 금지)**

| # | 결정 |
|---|---|
| 1 | **Option B 채택**: 단일 DTO/엔드포인트 유지. 서비스 진입부에서 role-필드 불일치를 명시적 400 으로 거부 |
| 2 | STUDENT 가 `businessCardImage`/`company`/`careerYear` 중 하나라도 non-null 로 보내면 400 |
| 3 | GRADUATE 가 `grade` non-null 로 보내면 400 |
| 4 | UNKNOWN role 의 update 호출은 **모든 역할 전용 필드 non-null 시 400** (양 역할 모두에 속하지 않음). 공통 필드만 보내는 건 통과 |
| 5 | 새 에러코드 `MEMBER_FIELD_ROLE_MISMATCH(BAD_REQUEST, "MEMBER400", "본인 역할과 일치하지 않는 필드는 수정할 수 없습니다.")` |
| 6 | 검증은 서비스 레이어, `Member.builder()` 만들기 **전** 에 수행 (실패 시 DB 접근 없이 즉시 throw) |

**구현 단계**

#### Step 1: `ErrorStatus` 에 `MEMBER_FIELD_ROLE_MISMATCH` 추가

#### Step 2: `MemberServiceImpl.updateMyProfile` 진입부에 검증 헬퍼 호출
```java
private void validateRoleFields(MemberRole role, UpdateMemberRequest req) {
    boolean studentField = req.grade() != null;
    boolean graduateField = req.businessCardImage() != null
            || req.company() != null
            || req.careerYear() != null;
    if (role == MemberRole.STUDENT && graduateField)
        throw new MemberHandler(ErrorStatus.MEMBER_FIELD_ROLE_MISMATCH);
    if (role == MemberRole.GRADUATE && studentField)
        throw new MemberHandler(ErrorStatus.MEMBER_FIELD_ROLE_MISMATCH);
    if (role == MemberRole.UNKNOWN && (studentField || graduateField))
        throw new MemberHandler(ErrorStatus.MEMBER_FIELD_ROLE_MISMATCH);
}
```

`updateMyProfile` 의 첫 줄에서 호출.

#### Step 3: 테스트
- `update_STUDENT_가_businessCardImage_보냄_400`
- `update_STUDENT_가_company_보냄_400`
- `update_STUDENT_가_careerYear_보냄_400`
- `update_GRADUATE_가_grade_보냄_400`
- `update_STUDENT_가_grade만_보냄_정상`
- `update_GRADUATE_가_company만_보냄_정상`
- `update_공통필드_보냄_역할무관_정상` (nickname/githubLink 만)
- `update_UNKNOWN_이_grade_보냄_400`

#### Step 4: CLAUDE.md 갱신
`### 회원 (Member)` 섹션에 다음 bullet:
> - **역할 외 필드 거부 (`MemberServiceImpl.validateRoleFields`)**: 단일 `UpdateMemberRequest` 에 STUDENT/GRADUATE 양쪽 필드가 다 있어, 본인 역할 외 필드를 non-null 로 보내면 즉시 400 (`MEMBER_FIELD_ROLE_MISMATCH`). STUDENT → graduate 필드 거부, GRADUATE → grade 거부, UNKNOWN → 양쪽 다 거부. 공통 필드만 보내는 건 항상 통과.

---

---

### TODO #5: TechStack 같은 이름 중복 추가 거부

**배경**
`TechStackServiceImpl.add` 가 중복 체크 없이 그대로 `save` 함. `TechStackRepository.existsByMemberIdAndName(Long, TechStackName)` 는 **이미 선언돼 있지만 service 가 호출하지 않음** — 누군가 의도해 만들었다가 빠뜨림. 결과: 같은 회원이 같은 이름 기술 스택을 N개 등록 가능.

**확정된 결정 사항 (재논의 금지)**

| # | 결정 |
|---|---|
| 1 | 같은 회원이 같은 `TechStackName` 으로 중복 등록 시 거부 |
| 2 | 응답 코드: **409 Conflict** (precedent: `DUPLICATE_EMAIL` 도 CONFLICT 사용) |
| 3 | 새 에러: `TECH_STACK_DUPLICATE(CONFLICT, "TECHSTACK409", "이미 등록된 기술 스택입니다.")` |
| 4 | 검증 메서드: 기존 레포의 `existsByMemberIdAndName` 활용 (메서드 신설 불필요) |
| 5 | 검증 위치: `add` 진입부, builder 생성 **전** |
| 6 | 다른 회원이 같은 이름 등록하는 건 항상 통과 (회원별 독립) |

**구현 단계**

#### Step 1: `ErrorStatus` 에 `TECH_STACK_DUPLICATE` 추가
기존 `TECH_STACK_NOT_FOUND`, `TECH_STACK_UNAUTHORIZED` 옆에.

#### Step 2: `TechStackServiceImpl.add` 진입부에 검증
```java
if (techStackRepository.existsByMemberIdAndName(member.getId(), request.name())) {
    throw new TechStackHandler(ErrorStatus.TECH_STACK_DUPLICATE);
}
```

#### Step 3: 테스트
- `add_같은이름_중복_409`
- `add_다른이름_정상`
- `add_다른회원이_같은이름_정상` (회원별 독립 검증)

#### Step 4: CLAUDE.md 갱신
`### 핵심 도메인 규칙` 아래에 새 `### 기술 스택 (techstack)` 섹션 신설하거나, 기존 본문에 적절한 위치에 bullet 추가:
> - **기술 스택 중복 거부 (`TechStackServiceImpl.add`)**: 같은 회원이 같은 `TechStackName` 으로 중복 등록 시 409 (`TECH_STACK_DUPLICATE`). 회원별 독립이라 다른 회원은 같은 이름 등록 가능. 레포 `existsByMemberIdAndName` 활용.

---

### TODO #6: TargetJob 같은 카테고리 중복 추가 거부

**배경**
`TargetJobServiceImpl.create` 가 중복 체크 없이 그대로 `save`. 같은 회원이 같은 `TargetJobCategory` 를 N개 등록 가능. 부수 효과로 `JobPostServiceImpl.create` 의 알람 발행 로직이 `notifiedMemberIds.add()` 로 dedup 하고 있음 (즉 알람 N건 발행은 막혀 있지만, DB 에는 row N개 누적). 데이터 무결성 측면에서 거부 필요.

**확정된 결정 사항 (재논의 금지)**

| # | 결정 |
|---|---|
| 1 | 같은 회원이 같은 `TargetJobCategory` 로 중복 등록 시 거부 |
| 2 | 응답 코드: **409 Conflict** |
| 3 | 새 에러: `TARGET_JOB_DUPLICATE(CONFLICT, "TARGETJOB409", "이미 등록된 관심 직군입니다.")` |
| 4 | 검증 메서드: `TargetJobRepository.existsByMemberIdAndInterestedJob(Long, TargetJobCategory)` **신설** (현재 `findByMemberIdAndInterestedJob` 만 있음) |
| 5 | `create` 와 `update` 모두 동일 규칙 적용 — update 로 다른 카테고리에서 이미 보유한 카테고리로 변경하려는 경우도 거부. 단 자기 자신 row 의 카테고리 그대로 update 는 통과 |
| 6 | `JobPostServiceImpl.create` 의 알람 dedup 로직(`notifiedMemberIds.add`) 은 **그대로 유지** — 안전망 (이번 변경으로 DB 중복은 막히지만, 방어층 보존) |

**구현 단계**

#### Step 1: `TargetJobRepository` 에 `existsByMemberIdAndInterestedJob(Long, TargetJobCategory)` 추가
Spring Data 쿼리 메서드 1줄.

#### Step 2: `ErrorStatus` 에 `TARGET_JOB_DUPLICATE` 추가

#### Step 3: `TargetJobServiceImpl.create` 진입부에 검증
```java
if (targetJobRepository.existsByMemberIdAndInterestedJob(member.getId(), request.interestedJob())) {
    throw new TargetJobHandler(ErrorStatus.TARGET_JOB_DUPLICATE);
}
```

#### Step 4: `TargetJobServiceImpl.update` 에도 검증 추가
자기 자신 row 의 카테고리는 변경 없으면 통과해야 하므로:
```java
boolean unchanged = targetJob.getInterestedJob() == request.interestedJob();
if (!unchanged && targetJobRepository.existsByMemberIdAndInterestedJob(member.getId(), request.interestedJob())) {
    throw new TargetJobHandler(ErrorStatus.TARGET_JOB_DUPLICATE);
}
```

#### Step 5: 테스트
- `create_같은카테고리_중복_409`
- `create_다른카테고리_정상`
- `create_다른회원이_같은카테고리_정상` (회원별 독립)
- `update_같은카테고리_그대로_통과`
- `update_이미보유한_다른카테고리로_409`
- `update_새카테고리로_정상`

#### Step 6: CLAUDE.md 갱신
`### 관심 직군 (interested_job)` 섹션에 bullet 추가:
> - **관심 직군 중복 거부 (`TargetJobServiceImpl.create` / `update`)**: 같은 회원이 같은 `TargetJobCategory` 로 중복 등록·변경 시 409 (`TARGET_JOB_DUPLICATE`). update 는 자기 row 의 카테고리 그대로 두는 건 통과. 회원별 독립. `JobPostServiceImpl.create` 의 알람 dedup 안전망(`notifiedMemberIds.add`) 은 보존.

---

### TODO #7: 커피챗 생성 시 자기 자신 / 동일 role 요청 거부

**배경**
`CoffeeChatServiceImpl.create` 가 requester ↔ receiver 의 ID 동일성, role 매칭 어느 것도 검증하지 않음. 결과:
1. **자기 자신**: `receiverId` 에 본인 PK 를 넣어도 통과 → 본인이 본인에게 보낸 알람 생성
2. **동일 role**: STUDENT ↔ STUDENT, GRADUATE ↔ GRADUATE 다 통과
3. **UNKNOWN**: 역할 미설정 회원이 양쪽으로 끼어들 수 있음

도메인 의도 = **학생 ↔ 졸업생 멘토링 매칭** 이므로 STUDENT ↔ GRADUATE 쌍만 허용.

**확정된 결정 사항 (재논의 금지)**

| # | 결정 |
|---|---|
| 1 | `requester.id == receiver.id` → 400 거부 |
| 2 | (requester, receiver) role 쌍이 (STUDENT, GRADUATE) 또는 (GRADUATE, STUDENT) 가 아니면 400 거부 |
| 3 | UNKNOWN role 은 양쪽 어느 쪽이든 끼어들면 거부 (요청자/수신자 둘 다 STUDENT/GRADUATE 여야 함) |
| 4 | 새 에러 2종:<br>- `COFFEE_CHAT_SELF_REQUEST(BAD_REQUEST, "COFFEECHAT400", "자기 자신에게는 커피챗을 요청할 수 없습니다.")`<br>- `COFFEE_CHAT_ROLE_MISMATCH(BAD_REQUEST, "COFFEECHAT400", "커피챗은 학생과 졸업생 사이에만 가능합니다.")` |
| 5 | 검증 순서: receiver 조회 직후 → 자기 자신 체크 → role 매칭 체크. self 가 먼저 (정보 노출 최소). |
| 6 | 검증 위치: `CoffeeChatServiceImpl.create` 진입부, `CoffeeChat.builder()` 호출 **전** |

**구현 단계**

#### Step 1: `ErrorStatus` 에 두 에러 추가
- `COFFEE_CHAT_SELF_REQUEST(HttpStatus.BAD_REQUEST, "COFFEECHAT400", "자기 자신에게는 커피챗을 요청할 수 없습니다.")`
- `COFFEE_CHAT_ROLE_MISMATCH(HttpStatus.BAD_REQUEST, "COFFEECHAT400", "커피챗은 학생과 졸업생 사이에만 가능합니다.")`

#### Step 2: `CoffeeChatServiceImpl.create` 에 검증 추가
```java
Member receiver = memberRepository.findById(request.receiverId())
        .orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));

if (member.getId().equals(receiver.getId())) {
    throw new CoffeeChatHandler(ErrorStatus.COFFEE_CHAT_SELF_REQUEST);
}

boolean studentToGraduate = member.getRole() == MemberRole.STUDENT
        && receiver.getRole() == MemberRole.GRADUATE;
boolean graduateToStudent = member.getRole() == MemberRole.GRADUATE
        && receiver.getRole() == MemberRole.STUDENT;
if (!studentToGraduate && !graduateToStudent) {
    throw new CoffeeChatHandler(ErrorStatus.COFFEE_CHAT_ROLE_MISMATCH);
}
```

#### Step 3: 테스트
- `create_자기자신에게_요청_400`
- `create_STUDENT가_STUDENT에게_400`
- `create_GRADUATE가_GRADUATE에게_400`
- `create_STUDENT가_GRADUATE에게_정상`
- `create_GRADUATE가_STUDENT에게_정상`
- `create_UNKNOWN이_요청자_400`
- `create_UNKNOWN이_수신자_400`
- `create_STUDENT가_UNKNOWN에게_400`

#### Step 4: CLAUDE.md 갱신
`### 커피챗 (coffeechat)` 섹션의 흐름 설명 위에 bullet 추가:
> - **생성 검증 (`CoffeeChatServiceImpl.create`)**: requester.id == receiver.id 면 400 (`COFFEE_CHAT_SELF_REQUEST`). role 쌍이 (STUDENT, GRADUATE) 또는 (GRADUATE, STUDENT) 가 아니면 400 (`COFFEE_CHAT_ROLE_MISMATCH`) — UNKNOWN 양쪽 모두 거부. 도메인 의도 = 학생 ↔ 졸업생 멘토링 매칭.

---

---

### TODO #8: PENDING 커피챗 알람 content 동적 생성 (요청자 정보 노출)

**배경**
현재 `CoffeeChatServiceImpl.create:54-60` 가 PENDING 알람을 발행할 때 content 를 정적 문자열 `"새로운 커피챗 요청이 도착했습니다."` 로 세팅. 결과 = 알람 목록/상세 화면(Figma)에서 요청자가 누구인지 표시할 수 없음. UI 가 요구하는 본문 = `"응용소프트웨어학과 김후배님이 커피챗을 요청했어요!"` 같은 사람 친화 메시지. 백엔드는 PENDING 알람의 content 에 요청자의 `department` + `nickname` 을 동적으로 포함시켜 발행.

**확정된 결정 사항 (재논의 금지)**

| # | 결정 |
|---|---|
| 1 | **방향 A 채택**: `AlarmResponse` DTO 는 그대로 두고 알람 `content` 메시지만 동적 생성. 최소 변경 (한 곳 한 줄). |
| 2 | PENDING 알람 content 형식: `"%s %s님이 커피챗을 요청했어요!"` 에 `requester.department`, `requester.nickname` 순서로 포맷팅 |
| 3 | ACCEPTED / REJECTED 알람 content 는 **변경 없음** (현재 정적 메시지 유지). 화면이 PENDING 상세 모달만 요구한 상태 |
| 4 | 헤더 텍스트 `"새로운 커피챗 신청이 들어왔습니다"` 는 프론트가 type 별 상수로 표시 (백엔드 미관여) |
| 5 | 수락/거절 액션 = 기존 `PATCH /api/v1/coffeechat/{coffeeChatId}/status` 그대로. 알람의 `refId` 를 `coffeeChatId` 로 사용 |
| 6 | requester 의 `department` / `nickname` 이 null 이어도 그대로 노출 (회원가입 시 모두 채워진다는 전제). 추후 필요시 별도 처리 |
| 7 | 새 에러코드 없음. 기존 흐름 그대로 200 OK |

**구현 단계**

#### Step 1: `CoffeeChatServiceImpl.create` 의 알람 content 동적화
파일: `src/main/java/mju/capstone/ddingconnect/domain/coffeechat/service/CoffeeChatServiceImpl.java`

기존 (L54–60):
```java
coffeeChatAlarmRepository.save(CoffeeChatAlarm.builder()
        .coffeeChat(saved)
        .member(receiver)
        .content("새로운 커피챗 요청이 도착했습니다.")
        .isRead(false)
        .build());
```

변경 후:
```java
String content = String.format("%s %s님이 커피챗을 요청했어요!",
        member.getDepartment(),
        member.getNickname());
coffeeChatAlarmRepository.save(CoffeeChatAlarm.builder()
        .coffeeChat(saved)
        .member(receiver)
        .content(content)
        .isRead(false)
        .build());
```

#### Step 2: 테스트 보강
파일: `src/test/java/mju/capstone/ddingconnect/domain/coffeechat/service/CoffeeChatServiceImplTest.java` (없으면 신설)

추가 시나리오:
- `create_PENDING_알람_content_요청자정보포함` — requester 가 `department="응용소프트웨어학과"`, `nickname="김후배"` 일 때 발행된 알람의 `content` 가 `"응용소프트웨어학과 김후배님이 커피챗을 요청했어요!"` 인지 `ArgumentCaptor<CoffeeChatAlarm>` 로 검증
- 기존 테스트 중 `content` 비교 어서션이 있다면 새 메시지로 갱신

#### Step 3: CLAUDE.md 갱신
`### 커피챗 (coffeechat)` 섹션의 알람 발행 규칙 bullet 수정:

기존:
> - 생성(PENDING): 수신자에게 1건

변경 후:
> - 생성(PENDING): 수신자에게 1건. content = `String.format("%s %s님이 커피챗을 요청했어요!", requester.department, requester.nickname)` 으로 동적 생성 (요청자 정보 노출). 헤더 텍스트는 프론트가 type 별 상수로 표시.

또 알람 발행 위치 섹션의 `CoffeeChatAlarm` 줄에 "PENDING content 는 요청자 학과·닉네임 포함" 보강.

#### Step 4: 커밋, 푸시, PR
- 커밋 메시지: `feat(coffeechat): PENDING 알람 content 에 요청자 학과·닉네임 포함`
- PR 제목: 동일
- PR 본문: 결정표 + 테스트 시나리오 + Swagger 검증 가이드

#### Step 5: Swagger 수동 검증
- 학생 S(department="응용소프트웨어학과", nickname="김후배") 가 졸업생 G 에게 `POST /api/v1/coffeechat` 요청
- G 토큰으로 `GET /api/v1/alarms` → 첫 항목 `content == "응용소프트웨어학과 김후배님이 커피챗을 요청했어요!"` 확인
- `GET /api/v1/alarms/COFFEE_CHAT/{알람PK}` 상세 호출도 동일 content
- `PATCH /api/v1/coffeechat/{refId}/status` 로 ACCEPTED → 기존 정적 메시지(`"커피챗 요청이 수락되었습니다. ..."`) 그대로 발행

#### 작업자 노트
- ACCEPTED / REJECTED 알람은 본 TODO 범위 외. 필요해지면 후속 TODO 로 분리
- 동적 content 는 i18n 도입 시점에 다국어 처리 어렵게 만들 수 있음. 그때는 방향 B (AlarmResponse 메타 필드 추가) 로 진화 검토
- TODO #7 (커피챗 자기 자신/동일 role 거부) 과 같은 도메인 — 둘 다 머지 시점 충돌 가능성. 가능하면 #7 머지 후 #8 진행

---

---

### TODO #9: Q&A 좋아요·답변 카운트·내 좋아요 상태 응답 노출 + Swagger 명세 갱신

**배경**
화면(Figma)이 요구하는 "답변 N", "👍 N", 좋아요 버튼 활성 상태가 백엔드 응답에 없음. 좋아요는 `QuestionLike` / `AnswerLike` row 로만 저장되고 카운트 캐시 컬럼 없음. 또한 5개 메서드가 레포에 **선언만 돼 있고 호출 0건**:

| # | 메서드 | 보여줄 정보 |
|---|---|---|
| 1 | `AnswerRepository.countByQuestionId(Long)` | 질문당 답변 개수 ("답변 12") |
| 2 | `QuestionLikeRepository.countByQuestionId(Long)` | 질문 좋아요 개수 |
| 3 | `AnswerLikeRepository.countByAnswerId(Long)` | 답변 좋아요 개수 |
| 4 | `QuestionLikeRepository.existsByMemberIdAndQuestionId(Long, Long)` | 본인이 이 질문에 좋아요 눌렀는지 |
| 5 | `AnswerLikeRepository.existsByMemberIdAndAnswerId(Long, Long)` | 본인이 이 답변에 좋아요 눌렀는지 |

**확정된 결정 사항 (재논의 금지)**

| # | 결정 |
|---|---|
| 1 | **DTO 확장 방식 채택** — 별도 카운트 endpoint 안 만듦. `QuestionResponse` / `AnswerResponse` 에 nullable 필드 추가 |
| 2 | `QuestionResponse` 추가 필드: `likeCount: Long`, `answerCount: Long`, `likedByMe: Boolean` |
| 3 | `AnswerResponse` 추가 필드: `likeCount: Long`, `likedByMe: Boolean` |
| 4 | 카운트는 매 GET 시 `countBy*` 쿼리 호출. **캐시 컬럼 미도입** (정규화 유지) |
| 5 | `likedByMe` 는 로그인된 `member.id` 기준 `existsByMemberIdAnd*` 호출 |
| 6 | **좋아요 토글 응답 개선**: `ApiResponse<String>` → `ApiResponse<LikeToggleResponse>`. 토글 후 새 `liked` / `likeCount` 즉시 반환 (프론트 추가 GET 불필요) |
| 7 | Q&A 모든 GET API (`getList`, `getOne`, `Answer.getList`) 가 `Member` 를 받아 `likedByMe` 채움. 시그니처 변경 |
| 8 | **N+1 위험 인지**: `getList` 에서 N건 질문마다 카운트 쿼리 1회 + exists 1회씩 호출. 작은 트래픽 허용, 큰 트래픽 시 batch/JOIN 최적화는 별도 TODO |
| 9 | **Swagger 명세 갱신**: `QuestionSwagger`, `AnswerSwagger` 의 `@Operation`/응답 타입 표기 갱신, 새 필드 의미 description 보강 |
| 10 | 비로그인 사용자도 GET 가능하게 정책 변경되면 별도 검토 (현재는 모든 GET 이 인증 필수라 `member` 항상 존재) |

**구현 단계**

#### Step 1: 토글 응답 DTO 신설
파일: `src/main/java/.../qna/dto/response/LikeToggleResponse.java` (또는 question/answer 도메인 양쪽 공용으로 두기)
```java
public record LikeToggleResponse(Boolean liked, Long likeCount) {}
```

#### Step 2: `QuestionResponse` 확장
```java
public record QuestionResponse(
        Long id, Long memberId, QuestionCategory category, String title,
        String content, Integer viewCount,
        Long likeCount, Long answerCount, Boolean likedByMe
) {
    public static QuestionResponse from(Question q, long likeCount,
                                        long answerCount, boolean likedByMe) {
        return new QuestionResponse(
                q.getId(), q.getMember().getId(), q.getCategory(),
                q.getTitle(), q.getContent(), q.getViewCount(),
                likeCount, answerCount, likedByMe);
    }
}
```
기존 `from(Question)` 시그니처는 호출처 모두 새 시그니처로 마이그레이션 (deprecated 처리 X — 컴파일 깨지는 곳 다 갱신).

#### Step 3: `AnswerResponse` 확장
동일 패턴, 필드 = `likeCount`, `likedByMe`.

#### Step 4: `QuestionServiceImpl` 변경
- `getList(Member member)` — Member 받음. 각 질문마다 `questionLikeRepository.countByQuestionId(q.id)`, `answerRepository.countByQuestionId(q.id)`, `questionLikeRepository.existsByMemberIdAndQuestionId(member.id, q.id)` 호출해 응답 채움.
- `getOne(Member member, Long questionId)` — viewCount+1 처리 후 동일하게 카운트·상태 채움.
- `toggleLike(Member, Long)` 반환 타입을 `LikeToggleResponse` 로 변경. toggle 후 `existsByMemberIdAndQuestionId` 로 새 `liked`, `countByQuestionId` 로 새 `likeCount` 반환.

#### Step 5: `AnswerServiceImpl` 변경
- `getList(Member member, Long questionId)` — 동일 패턴, 각 답변에 `likeCount`/`likedByMe` 채움.
- `toggleLike(Member, Long)` 반환 타입 변경 (Step 4 와 동일 패턴).
- `create()`, `update()` 의 응답도 새 시그니처로 채움 (자기 답변이라 `likedByMe=false`, `likeCount=0` 기본).

#### Step 6: 컨트롤러 시그니처 갱신
`QuestionController`:
- `getQuestions()` → `@LoginMember Member member` 추가, `questionService.getList(member)` 호출
- `getQuestion(questionId)` → 동일하게 member 받아 전달
- `likeQuestion(...)` 반환 타입 `ApiResponse<LikeToggleResponse>` 로 변경

`AnswerController`:
- `getAnswers(...)` → member 받아 전달
- `likeAnswer(...)` 반환 타입 변경

#### Step 7: Swagger 명세 갱신
`QuestionSwagger`, `AnswerSwagger` 인터페이스:
- 응답 반환 타입 표기 갱신 (`ApiResponse<LikeToggleResponse>` 등)
- `@Operation` description 보강 — 예: 질문 상세 → "응답에 좋아요 개수(`likeCount`), 답변 개수(`answerCount`), 본인 좋아요 여부(`likedByMe`)가 포함됩니다."
- 좋아요 토글 description — "토글 후 새로운 상태와 카운트가 응답에 포함됩니다."
- 필요시 `QuestionResponse`/`AnswerResponse` DTO 의 필드에 `@Schema(description = "...")` 추가 (선택)

#### Step 8: 테스트
기존 `QuestionServiceImplTest`, `AnswerServiceImplTest`:
- `QuestionResponse.from(question)` 호출하는 단정문이 깨지므로 새 시그니처로 마이그레이션
- 새 테스트:
  - `getOne_좋아요갯수_답변갯수_likedByMe_정상반환` — 좋아요 2건 + 답변 3건 + 본인 좋아요 누른 상태 → likeCount=2, answerCount=3, likedByMe=true
  - `getList_각질문별_카운트누락없음` — 질문 N건 각각의 카운트 정상
  - `toggleLike_누른후_liked_true_likeCount_증가` — 응답 검증
  - `toggleLike_재호출_liked_false_likeCount_감소`
- AnswerServiceImplTest 동일 패턴
- `QuestionLikeRepository`/`AnswerLikeRepository` 가 mock 이라면 `countByQuestionId`/`existsByMemberIdAndQuestionId` 등 stub 추가

#### Step 9: CLAUDE.md 갱신
`### Q&A` 섹션 좋아요 bullet 보강:
> - 좋아요: `QuestionLike`, `AnswerLike` (복합키 `AnswerLikeId`), toggle 방식. **카운트·내 좋아요 여부**는 응답 DTO 에 포함 (`QuestionResponse.likeCount`/`likedByMe`/`answerCount`, `AnswerResponse.likeCount`/`likedByMe`). 매 GET 시 `countBy*` / `existsByMemberIdAnd*` 쿼리 호출 (캐시 컬럼 미도입).
> - 좋아요 토글 응답: `LikeToggleResponse(liked, likeCount)` — 토글 후 새 상태·카운트 즉시 반환. 프론트 추가 GET 불필요.
> - 답변 개수: `QuestionResponse.answerCount` 로 노출 (`AnswerRepository.countByQuestionId`).

#### Step 10: Swagger 수동 검증
- `GET /api/v1/questions` → 각 항목에 `likeCount`, `answerCount`, `likedByMe` 보임
- `GET /api/v1/questions/{id}` → 동일
- `GET /api/v1/questions/{qid}/answers` → 각 답변에 `likeCount`, `likedByMe` 보임
- `POST /api/v1/questions/{id}/like` → 응답 `{"liked": true, "likeCount": 1}`. 한 번 더 호출 → `{"liked": false, "likeCount": 0}`
- `POST /api/v1/questions/{qid}/answers/{aid}/like` → 동일 패턴

#### Step 11: 커밋, 푸시, PR
- 커밋 메시지: `feat(qna): 좋아요·답변 카운트·내 좋아요 상태 응답 노출 + Swagger 명세 갱신`
- PR 본문: 결정표 + 테스트 시나리오 + Swagger 검증 가이드

#### 작업자 노트
- **시그니처 변경 영향**: `QuestionResponse.from(question)` 단일 인자 호출처가 모두 깨짐. 컴파일러가 잡아주므로 일괄 마이그레이션.
- **N+1 최적화**: 본 TODO 범위 외. `getList` 가 느려지면 별도 TODO 로 분리 (단일 GROUP BY 쿼리 또는 projection DTO).
- **응답 DTO 가 커짐**: 다른 화면 (예: 알람 → 질문 미리보기) 에서 카운트 불필요 시 가벼운 별도 DTO 검토. 일단은 단일 응답 유지.
- **TODO #1 의 회원 hard delete 와의 관계**: 회원 탈퇴 시 본인이 누른 좋아요 row 도 정리 (`QuestionLikeRepository.deleteByMemberId`). TODO #1 의 step 2 에 이미 포함된 작업.

---

---

### TODO #10: Q&A 본인이 쓴 글에 본인이 좋아요 토글 거부

**배경**
`QuestionServiceImpl.toggleLike` (L114-128), `AnswerServiceImpl.toggleLike` (L104-117) 둘 다 본인 글 체크 없음. 본인이 자기 질문/답변에 좋아요를 누르면 `QuestionLike`/`AnswerLike` row 가 그대로 INSERT 됨. 카운트 부풀리기 가능.

비교: 알람 도메인의 "본인이 본인 질문에 답변 시 알람 미발행" (`AnswerServiceImpl.create` L49) 같은 자기 자신 방어 패턴은 이미 코드 곳곳에 존재. 좋아요에도 동일 패턴 필요.

**확정된 결정 사항 (재논의 금지)**

| # | 결정 |
|---|---|
| 1 | Question/Answer 좋아요 토글 모두 `member.id == 작성자.id` 인 경우 거부 — 한 TODO 로 묶어 처리 |
| 2 | 응답 코드: **400 Bad Request** (TODO #7 의 `COFFEE_CHAT_SELF_REQUEST` 와 동일 패턴) |
| 3 | 새 에러 2종:<br>- `QUESTION_SELF_LIKE(BAD_REQUEST, "QUESTION400", "본인이 작성한 질문에는 좋아요를 누를 수 없습니다.")`<br>- `ANSWER_SELF_LIKE(BAD_REQUEST, "ANSWER400", "본인이 작성한 답변에는 좋아요를 누를 수 없습니다.")` |
| 4 | 검증 위치: `toggleLike` 진입부 (질문/답변 조회 직후, like row 조회/save 전) |
| 5 | 화면 UI 에서 버튼이 비활성화돼 있더라도 백엔드는 항상 거부 (악성 사용자 대비) |
| 6 | TODO #9 (좋아요 카운트 응답 노출) 머지 후 진행 권장 — `toggleLike` 시그니처 변경 충돌 회피 |

**구현 단계**

#### Step 1: `ErrorStatus` 에 두 에러 추가
- `QUESTION_SELF_LIKE`
- `ANSWER_SELF_LIKE`

#### Step 2: `QuestionServiceImpl.toggleLike` 진입부 검증
```java
Question question = questionRepository.findById(questionId)
        .orElseThrow(() -> new QuestionHandler(ErrorStatus.QUESTION_NOT_FOUND));

if (question.getMember().getId().equals(member.getId())) {
    throw new QuestionHandler(ErrorStatus.QUESTION_SELF_LIKE);
}
// 기존 toggle 로직 그대로
```

#### Step 3: `AnswerServiceImpl.toggleLike` 진입부 검증
```java
Answer answer = answerRepository.findById(answerId)
        .orElseThrow(() -> new AnswerHandler(ErrorStatus.ANSWER_NOT_FOUND));

if (answer.getMember().getId().equals(member.getId())) {
    throw new AnswerHandler(ErrorStatus.ANSWER_SELF_LIKE);
}
// 기존 toggle 로직 그대로
```

#### Step 4: 테스트
- `toggleLike_본인질문_400`
- `toggleLike_타인질문_정상`
- `toggleLike_본인답변_400`
- `toggleLike_타인답변_정상`
- (TODO #9 머지 후라면) 응답이 `LikeToggleResponse` 인지도 같이 검증

#### Step 5: CLAUDE.md 갱신
`### Q&A` 섹션 좋아요 bullet 보강:
> - 좋아요: `QuestionLike`, `AnswerLike` (복합키 `AnswerLikeId`), toggle 방식. **본인이 작성한 글에는 좋아요 불가** (`QuestionServiceImpl.toggleLike` / `AnswerServiceImpl.toggleLike` 진입부에서 작성자 == 호출자 체크. 위반 시 400 `QUESTION_SELF_LIKE` / `ANSWER_SELF_LIKE`).

#### 작업자 노트
- `QuestionServiceImpl.toggleLike` 의 기존 코드는 `questionLikeRepository.findAll().stream().filter(...)` 로 **모든 좋아요 row 를 메모리에 로드 후 필터** 하는 비효율적 패턴. 본 TODO 범위 외지만 같이 개선 가능 — `existsByMemberIdAndQuestionId` 활용해서 O(1) 쿼리로 단순화. 다만 본 TODO 핵심은 self-like 거부.

---

### TODO #11: 답변 등록은 졸업생만 가능

**배경**
`AnswerServiceImpl.create` (L34-58) 에 role 체크 없음. 학생도 답변 가능. 도메인 의도 = Q&A 에서 답변은 졸업생(멘토) 만 작성. 비교: `JobPostServiceImpl.create` (L42-44) 는 `member.getRole() != MemberRole.GRADUATE` 일 때 `POST_CONTENTS_NOT_GRADUATE` throw — 동일 패턴 답변에도 적용.

**확정된 결정 사항 (재논의 금지)**

| # | 결정 |
|---|---|
| 1 | `AnswerServiceImpl.create` 진입부에서 `member.getRole() != MemberRole.GRADUATE` 시 즉시 거부 |
| 2 | 응답 코드: **403 Forbidden** (`POST_CONTENTS_NOT_GRADUATE` 와 동일 패턴) |
| 3 | 새 에러: `ANSWER_NOT_GRADUATE(FORBIDDEN, "ANSWER403", "졸업생만 답변을 등록할 수 있습니다.")` |
| 4 | 기존 학생이 이미 등록한 답변 row 는 **그대로 둠** (마이그레이션 없음, 신규 호출만 차단) |
| 5 | 답변 수정/삭제는 기존 작성자 권한 검증 그대로 유지 (역할 제한 X — 한 번 등록된 답변은 본인이 관리) |
| 6 | UNKNOWN role 도 비-GRADUATE 라 자동 거부 |

**구현 단계**

#### Step 1: `ErrorStatus` 에 `ANSWER_NOT_GRADUATE` 추가

#### Step 2: `AnswerServiceImpl.create` 진입부에 가드
```java
@Override
@Transactional
public AnswerResponse create(Member member, Long questionId, CreateAnswerRequest request) {
    if (member.getRole() != MemberRole.GRADUATE) {
        throw new AnswerHandler(ErrorStatus.ANSWER_NOT_GRADUATE);
    }

    Question question = questionRepository.findById(questionId)
            .orElseThrow(() -> new QuestionHandler(ErrorStatus.QUESTION_NOT_FOUND));
    // 기존 흐름 그대로
    ...
}
```

#### Step 3: 테스트
- `create_GRADUATE_정상_등록`
- `create_STUDENT_403_답변불가`
- `create_UNKNOWN_403_답변불가`
- 기존 `AnswerAlarm` 발행 테스트가 STUDENT 회원으로 작성됐다면 GRADUATE 로 변경 필요 (마이그레이션)

#### Step 4: CLAUDE.md 갱신
`### Q&A` 섹션에 bullet 추가:
> - **답변 등록은 졸업생만** (`AnswerServiceImpl.create`): STUDENT/UNKNOWN 이 호출 시 403 (`ANSWER_NOT_GRADUATE`). 도메인 의도 = Q&A 답변은 멘토(졸업생) 가 학생 질문에 답하는 흐름. 수정/삭제는 기존 작성자 권한만 검증 (역할 제한 X).

#### 작업자 노트
- 기존 코드의 "본인이 본인 질문에 답변한 경우 알람 미발행" (L48-55) 분기는 그대로 유지 — 단 GRADUATE 가 본인 질문에 답하는 경우만 해당 (학생은 진입 자체 차단). 로직 그대로 두면 정상 동작.
- 화면이 STUDENT 에게 답변 버튼을 노출하지 않는다는 전제. 백엔드는 악성 호출 대비 가드만 추가.

---

### 11개 TODO 공통 작업자 노트

- **브랜치 정책**: 각 TODO 를 **개별 브랜치 + 개별 PR** 로 처리하는 것을 기본으로 한다. TODO #1 은 영향 범위가 커서 단독 PR 필수. TODO #2/#3/#4 는 회원 도메인의 작은 검증 변경이라 묶어서 1개 PR 도 허용 (작업자 판단). TODO #5/#6/#7/#8/#9/#10/#11 도 각각 다른 변경 지점이라 단독 PR 권장. 사용자가 "TODO #N 작업" 으로 단일 항목 지목 시 그 항목만 단독 PR.
- **공통 커밋 메시지 컨벤션**: `feat(member): ...` / `fix(member): ...` / `feat(techstack): ...` / `feat(coffeechat): ...` / `feat(qna): ...` / `docs(CLAUDE.md): ...` prefix. 마지막 줄에 항상 `https://claude.ai/code/session_...` 포함.
- **Swagger 검증**: 가능하면 PR 본문 Test plan 에 Swagger 시나리오 체크박스 포함.
- **테스트 실패 시**: `--no-verify` 등으로 우회 금지. 원인 분석 후 수정.
