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

### 통합 알람 (alarm)
- 4종(`AnswerAlarm`, `JobAlarm`, `RoadmapAlarm`, `CoffeeChatAlarm`)을 합쳐 단일 `AlarmResponse` 리스트로 반환
- 정렬: `createdAt DESC`, null은 뒤로
- 읽음 처리는 Builder 패턴으로 새 인스턴스 만들어 저장 (불변 스타일)
- 각 타입별 소유자 검증 헬퍼 분리 (`verify*AlarmOwner`)

### Q&A
- `Question` 1:N `Answer`
- 좋아요: `QuestionLike`, `AnswerLike` (복합키 `AnswerLikeId`), toggle 방식
- 작성자만 수정/삭제

### 로드맵 (roadmap)
- 흐름: 데이터 파트가 화면 폼(학년/학점/전공/관심 직무/보유 역량/목표 기업) 입력값으로 AI 호출 → 결과 JSON만 백엔드가 저장. **AI 호출 자체는 백엔드 미포함** (Spring 측에 OpenAI/HTTP 클라이언트 의존성 없음).
- `Roadmap.content`: MySQL JSON 컬럼 (`@JdbcTypeCode(SqlTypes.JSON)`) — INSERT 시 `CAST(? AS JSON)` 수행되므로 valid JSON이 아니면 DB가 SQLException을 던짐.
- 등록(`POST /api/v1/roadmaps`) 시 `RoadmapServiceImpl.validateJsonContent()`로 사전 검증:
  - null / blank / JSON 파싱 실패 / JSON primitive(string·number·boolean·null) → `ROADMAP_INVALID_CONTENT` (HTTP 400)
  - **JSON object 또는 array만 통과** (구조만 검증; content 내부 키 스키마는 미강제 — 데이터 파트와 별도 합의)
  - DB SQLException으로 500이 떨어지지 않도록 컨트롤러→서비스 경계에서 막는 게 목적
- update API 미지원 (재생성 = 새 create + 기존 delete)
- 삭제는 소유자(member.id 일치)만 가능 (`ROADMAP_UNAUTHORIZED`)
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
