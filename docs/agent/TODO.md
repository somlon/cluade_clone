# 진행 예정 작업 (To-do)

> 이 파일은 루트 `CLAUDE.md` 가 `@import` 하지 않는다(자주 바뀌어 자동 로드 제외). 사용자가 TODO 관련 명령을 내리면 이 파일을 직접 읽고 진행한다. 완료 후 항목은 이 파일에서 삭제하고 `docs/agent/backend.md` 의 해당 도메인 섹션에 정식 규칙으로 통합한다.

> 사용자가 "to-do 리스트 수행" / "to-do 진행" / "TODO N 작업" 등 유사 명령을 내리면 아래 항목을 **즉시 코드 작성 → 테스트 보강 → CLAUDE.md 갱신 → 커밋/푸시 → PR 생성까지 일사천리로** 수행한다. 결정 사항은 이미 확정돼 있으니 다시 묻지 말고 명시된 디폴트로 진행할 것.
>
> 각 항목은 완료(머지) 후 이 섹션에서 삭제하고, 본문 도메인 섹션에 정식 규칙으로 통합 기록한다.

> **관찰 (의도 확인 필요)**: `ErrorStatus.getReason()`/`getReasonHttpStatus()` 가 실패 코드 enum 인데 `ErrorReasonDTO` 를 `.isSuccess(true)` 로 고정 생성한다(`SuccessStatus` 와 대비). 현재 `ApiResponse.onFailure` 가 `isSuccess=false` 를 따로 지정하므로 실제 응답엔 영향 없으나, 오해를 부르는 죽은 설정값이다 — 의도 확인 후 정리 여부 결정.


### 11개 작업자 노트 (TODO #1~#11 머지 완료 후 보존되는 일반 가이드)

- **브랜치 정책**: 각 TODO 를 **개별 브랜치 + 개별 PR** 로 처리하는 것을 기본으로 한다. 영향 범위가 큰 TODO 는 단독 PR 필수. 같은 도메인 내 작은 변경은 묶어서 1개 PR 도 허용 (작업자 판단). 사용자가 "TODO N 작업" 으로 단일 항목 지목 시 그 항목만 단독 PR.
- **공통 커밋 메시지 컨벤션**: `feat(<도메인>): ...` / `fix(<도메인>): ...` / `refactor(<도메인>): ...` / `docs(CLAUDE.md): ...` prefix. 마지막 줄에 항상 `https://claude.ai/code/session_...` 포함.
- **Swagger 검증**: 가능하면 PR 본문 Test plan 에 Swagger 시나리오 체크박스 포함.
- **테스트 실패 시**: `--no-verify` 등으로 우회 금지. 원인 분석 후 수정.


### TODO L~R 통합 머지 전략 (ddingconnect-backend 동기화용)

> `cluade_clone` 에서는 L~R 7개가 PR #73~#80 으로 **개별** 머지됐다. 이를 다음 동기화 대상(`mju-capstone-4/ddingconnect-backend`)으로 옮길 때는 같은 파일을 건드리는 항목을 묶어 **4개 PR 로 압축**한다. 머지 순서는 파일 충돌·서비스 의존성 기준.

#### 충돌 분석

| 파일 | L | M | N | O | P | Q | R |
|------|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| `MemberController` | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |   |
| `MyPageServiceImpl` (or 위임 서비스) | ✓ |   |   |   | ✓ |   |   |
| `MemberServiceImpl` |   | ✓ | ✓ | ✓ |   | ✓ |   |
| `MyPageResponse` | ✓ |   |   |   | ✓ |   |   |
| `UpdateXxxMyPageRequest` (DTO 신규) | ✓ |   |   |   |   |   |   |
| `S3Service` |   |   | ✓ | ✓ |   | ✓ |   |
| `RoadmapController` |   |   |   |   | ✓ |   |   |
| `JobPostController` / `JobPostServiceImpl` |   |   |   |   |   |   | ✓ |
| `ErrorStatus` |   |   | ✓ | ✓ |   | ✓ | ✓ |

핵심 충돌 구간:
- `MyPageResponse` + `MyPageServiceImpl.buildResponse` — **L + P** (activity 분기 / 역할별 응답)
- `S3Service` — **N + O + Q** (O 가 foundation, N/Q 가 사용)
- `MemberController` — 다수 PR 이 새 endpoint 메서드 추가 → 메서드 단위 분리되어 있어 순차 머지로 회피

#### 의존성

- **L → R**: L 의 `UpdateGraduateMyPageRequest.jobPostsToAdd` 가 R 의 `JobPostService.createFromLink` 호출
- **N, Q → O**: N (profileImage), Q (businessCard) 가 O 의 `S3Service.uploadFile(MultipartFile, Set<String>, long)` 호출

#### PR 분할 (4개)

##### PR1 — `feat(job-post): 졸업생 링크 공고 + 선배/일반 분리 조회` (TODO R)

- 엔드포인트:
  - `POST /api/v1/job-post/link` (GRADUATE 전용, `detailUrl` 만 입력)
  - `GET /api/v1/job-post/graduates` (선배 매핑 있음)
  - `GET /api/v1/job-post/crawled` (선배 매핑 없음, 빈 IN 폴백 포함)
- 주요 파일: `JobPostController`, `JobPostServiceImpl` (`createFromLink`, `getGraduatePosts`, `getCrawledPosts`, `create` 의 `jobType=null` 가드), `CreateJobPostLinkRequest` DTO, `GraduatePostResponse` DTO, `GraduateJobPostRepository.findDistinctPostContentsIds`, `PostContentsRepository.findByIdNotIn`, `ErrorStatus.POST403`
- 의존: 없음 (다른 PR 영향 없음)
- **머지 순서 1순위** — PR2 의 `createFromLink` 호출 컴파일 의존

##### PR2 — `feat(member,roadmap): 마이페이지 역할별 분리 + 로드맵 가드` (TODO L + P)

- 엔드포인트:
  - `PATCH /api/v1/members/mypage/student` (L 신규)
  - `PATCH /api/v1/members/mypage/graduate` (L 신규)
  - `GET /api/v1/members/mypage` (P, `activity.roadmapCount` STUDENT 분기)
  - `GET /api/v1/roadmaps` (P, GRADUATE 차단)
- 주요 파일: `MemberController` (mypage PATCH 2개), `MyPageService(Impl)` (`updateStudentMyPage`/`updateGraduateMyPage`, `buildResponse` roadmapCount STUDENT 분기), `MyPageResponse`, `UpdateStudentMyPageRequest`/`UpdateGraduateMyPageRequest`, `UpdateStudentProfileRequest`/`UpdateGraduateProfileRequest`, `RoadmapController` (`getRoadmaps` GRADUATE 가드)
- 통합 이유: L + P 둘 다 `MyPageResponse` + `MyPageServiceImpl.buildResponse` 를 수정 → 분리 시 거의 100% 충돌
- 의존: PR1 (`JobPostService.createFromLink`)
- **머지 순서 2순위**

##### PR3 — `feat(member,coffeechat,qna): 나의 활동 페이지 API` (TODO M)

- 엔드포인트:
  - `GET /api/v1/questions/me` (Q&A 본인 글)
  - `GET /api/v1/members/me/activity/coffeechats` (커피챗 카드, 상대방 정보 포함)
  - `GET /api/v1/roadmaps` (기존 활용, 변경 없음)
- 주요 파일: `QuestionController` (`getMyQuestions`), `QuestionService(Impl)` (`getMyQuestions`, `questionRepository.findByMemberId`), `CoffeeChatService(Impl)` (`getMyActivities`), `MyActivityController` (신규), `CoffeeChatPartnerResponse` DTO
- 의존: 없음 (독립 도메인)
- **머지 순서 3순위** — `MemberController` 충돌 회피 위해 PR2 후 진행

##### PR4 — `feat(member,aws): S3Service 일반화 + 파일 업로드 3종` (TODO N + O + Q)

- 엔드포인트:
  - `PATCH /api/v1/members/me/profile-image` (N)
  - `PATCH /api/v1/members/me/portfolio` (O)
  - `PATCH /api/v1/members/me/business-card` (Q, GRADUATE 가드)
- 주요 파일: `S3Service` (`uploadFile(MultipartFile, Set<String> allowedContentTypes, long maxBytes)` 신규, `uploadImage` 시그니처 유지), `MemberController` (3개 PATCH), `MemberServiceImpl` (`updateProfileImage`/`updatePortfolio`/`updateBusinessCard` + Q role guard), `ErrorStatus._FILE_TYPE_NOT_ALLOWED`/`_FILE_TOO_LARGE`
- 통합 이유: 셋 다 `S3Service` 수정 + O 가 N/Q 의 foundation. 분리 시 S3Service 3회 머지 충돌 잦음.
- **회귀 보장**: `uploadImage(MultipartFile)` 시그니처 유지 → `AuthServiceImpl.signup()` 증명서 흐름(PDF 도 통과) 영향 없음
- 의존: 없음 (독립)
- **머지 순서 4순위** — `MemberController` 충돌 회피 위해 PR3 후 진행. PR3 와 병렬도 가능하나 직렬 권장.

#### 머지 순서 요약

```
PR1 (R, JobPost)
  ↓
PR2 (L+P, 마이페이지 + 로드맵 가드)
  ↓
PR3 (M, 활동 페이지)
  ↓
PR4 (N+O+Q, 파일 업로드 3종)
```

`MemberController` 가 PR2/PR3/PR4 에 공통 등장 — 머지 순서 직렬화로 충돌 방지.

#### 로컬 검증 상태 (2026-05-23, ddingconnect-backend)

위 4개 PR 분할 안에 해당하는 작업을 `ddingconnect-backend` 로컬에 미리 적용해 7개 TODO 전 항목 end-to-end 검증 완료. **30/31 PASS**. 1건은 환경 설정 차이로 백엔드 검증 로직 미트리거(기능적으로는 차단됨):

| TODO | PASS/전체 | 핵심 검증 |
|------|----------|-----------|
| L | 4/4 | role guard 400 + Bean Validation 400 |
| M | 3/3 | `/questions/me` 본인 스코프 + partner 정보 매핑 |
| N | 4/5 | PNG 200 / GIF·5MB+ 400. 5MB+ 는 Tomcat 413 차단 (앱 `_FILE_TOO_LARGE` 미트리거) |
| O | 4/4 | PDF 200 / PNG 400. `uploadImage` 회귀 없음 |
| P | 4/4 | GRADUATE `/roadmaps` 400 + mypage.activity.roadmapCount=0 + 단건 조회 200 |
| Q | 4/4 | STUDENT 400 MEMBER400 (S3 호출 전 차단) + GRADUATE 다른 필드 보존 |
| R | 7/7 | 링크 등록 200 + STUDENT 403 + 선배/일반 분리 + 합계 일치 |

별도 환경 메모 (TODO 결함 아님):
- MySQL utf8mb4 미설정 → 한글 필드 `?????` 저장 (테스트는 영문 입력으로 회피)
- `spring.servlet.multipart.max-file-size: 5MB` 설정으로 N-(d) 가 Tomcat 413 으로 차단 — 앱 `_FILE_TOO_LARGE` 검증을 직접 보고 싶으면 max-file-size 를 10MB 로 늘리고 컨트롤러/서비스에서 5MB 자체 체크


### TODO S — 마이페이지 응답 역할별 필드 전면 정리

> 발견 맥락: 재학생/졸업생 마이페이지 응답에 **상대 역할 필드 + dead field** 가 항상 포함되어 화면에 표시되지도 않는 잡음이 응답 곳곳에 섞임. Figma 화면과 실제 응답 contract 가 8개 필드에서 불일치.

#### 화면 ↔ 응답 불일치 매핑

| 응답 위치 | 필드 | STUDENT 화면 | STUDENT 응답 | GRADUATE 화면 | GRADUATE 응답 | 처리 방향 |
|----------|------|-------------|-------------|--------------|-------------|----------|
| `activity` | `coffeeChatCount` | ✓ | 값 | ✓ | 값 | 유지 |
| `activity` | `roadmapCount` | ✓ | 값 | ❌ 없음 | **0 고정** | GRADUATE 는 응답 제외 |
| `activity` | `questionCount` | ✓ | 값 | ✓ | 값 | 유지 |
| top-level | `techStacks` | ✓ | 값 | ✓ | 값 | 유지 |
| top-level | `targetJobs` | ✓ | 값 | ❌ 없음 | **`[]`** | GRADUATE 는 응답 제외 |
| top-level | `jobPosts` | ❌ 없음 | **`[]`** | ✓ | 값 | STUDENT 는 응답 제외 |
| `profile` | `grade` | ✓ | 값 | ❌ 없음 | **null** | GRADUATE 는 응답 제외 |
| `profile` | `businessCardImage` | ❌ 없음 | **null** | ✓ | 값 | STUDENT 는 응답 제외 |
| `profile` | `jobType` | ❌ 없음 | **null** | ✓ | 값 | STUDENT 는 응답 제외 |
| `profile` | `company` | ❌ 없음 | **null** | ✓ | 값 | STUDENT 는 응답 제외 |
| `profile` | `careerYear` | ❌ 없음 | **null** | ✓ | 값 | STUDENT 는 응답 제외 |
| `profile` | `point` | ❌ 없음 | **null** | ❌ 없음 | **null** | **마이페이지에선 항상 제외** (향후 값이 채워져도 mypage 응답엔 미포함) |

#### 목표

- `null` = **비해당 필드** (or dead/미구현 필드) → JSON 응답에서 키 자체 제외
- `[]` = **해당 역할 + 0개** → 응답에 그대로 포함

→ 프론트가 "공고 없음" 같은 빈 상태 UI 를 그릴지 말지 명확히 구분 + 응답 잡음 제거.

#### 변경 파일 (4개)

1. **`MyPageResponse.java`** (record) — `@JsonInclude(NON_NULL)` 추가, `targetJobs`/`jobPosts` 는 nullable:
   ```java
   @JsonInclude(JsonInclude.Include.NON_NULL)
   public record MyPageResponse(
       MemberResponse profile,
       ActivityStats activity,
       List<TechStackResponse> techStacks,
       List<TargetJobResponse> targetJobs,   // STUDENT 전용, GRADUATE 는 null
       List<JobPostResponse> jobPosts        // GRADUATE 전용, STUDENT 는 null
   ) {}
   ```

2. **`MyPageResponse.ActivityStats`** (nested record) — `roadmapCount` 를 `long` → `Long` 변환:
   ```java
   @JsonInclude(JsonInclude.Include.NON_NULL)
   public record ActivityStats(
       long coffeeChatCount,
       Long roadmapCount,    // long → Long: GRADUATE 는 null → 응답 제외
       long questionCount
   ) {}
   ```

3. **`MemberResponse.java`** (record) — `@JsonInclude(NON_NULL)` 추가 + 마이페이지용 헬퍼 메서드:
   ```java
   @JsonInclude(JsonInclude.Include.NON_NULL)
   public record MemberResponse(
       Long id, String email, ..., Long point,  // 다른 엔드포인트에선 노출 가능, null 이면 제외
       MemberRole role,
       Integer grade,                            // STUDENT 전용
       String businessCardImage, JobType jobType, String company, Integer careerYear  // GRADUATE 전용
   ) {
       /** 마이페이지 응답용 — point 를 항상 제외 (마이페이지에선 노출하지 않는 정책) */
       public MemberResponse withoutPoint() {
           return new MemberResponse(id, email, name, nickname, studentNumber, department,
               githubLink, linkedinLink, portfolio, profileImage, null, role,
               grade, businessCardImage, jobType, company, careerYear);
       }
   }
   ```
   → 기존 정적 팩토리 (`from(Member, Student)`, `from(Member, Graduate)`) 는 비해당 역할 필드를 `null` 로 채우고 있어 어노테이션만으로 자동 동작. `point` 만 별도 처리 (`withoutPoint()` 헬퍼). **`GET /me` 등 다른 엔드포인트는 영향 없음** — 그쪽은 정적 팩토리 결과 그대로 사용해 향후 `point` 값이 들어가면 노출.

4. **`MyPageServiceImpl.buildResponse`** — 역할별 null 설정 + `point` 제외:
   ```java
   private MyPageResponse buildResponse(Member member, MemberResponse profile) {
       // 마이페이지에선 point 를 노출하지 않음 (다른 엔드포인트는 영향 없음)
       MemberResponse profileForMyPage = profile.withoutPoint();

       Long roadmapCount = member.getRole() == MemberRole.STUDENT
           ? roadmapService.countMyRoadmaps(member)
           : null;   // long 0L 대신 Long null

       List<TargetJobResponse> targetJobs = member.getRole() == MemberRole.STUDENT
           ? targetJobService.getMyTargetJobs(member)
           : null;
       List<JobPostResponse> jobPosts = member.getRole() == MemberRole.GRADUATE
           ? jobPostService.getMyJobPosts(member)
           : null;

       MyPageResponse.ActivityStats activity = new MyPageResponse.ActivityStats(
           coffeeChatService.countMyAcceptedCoffeeChats(member),
           roadmapCount,
           questionService.countMyQuestions(member)
       );

       return new MyPageResponse(profileForMyPage, activity,
           techStackService.getMyTechStacks(member), targetJobs, jobPosts);
   }
   ```

#### 테스트 보강

- `MyPageServiceImplTest`/`MemberControllerTest` 에 JSON 키 존재 검증:
  - STUDENT 응답에 `jobPosts` / `businessCardImage` / `jobType` / `company` / `careerYear` 키 **없음**
  - GRADUATE 응답에 `targetJobs` / `grade` / `activity.roadmapCount` 키 **없음**
  - STUDENT 의 `targetJobs` 가 0개일 때 `[]` 로 **유지** (null 아님)
  - GRADUATE 의 `jobPosts` 가 0개일 때 `[]` 로 **유지**
  - **마이페이지 응답에선 `point` 키가 항상 없음** (값이 채워져도 미포함, `withoutPoint()` 적용)
  - 다른 엔드포인트(`GET /me` 등)에선 `point` 가 null 이면 응답에서 제외, 값 있으면 포함 (정책 분리 검증)
  - UNKNOWN 역할은 양쪽 필드 모두 키 없음

#### 문서 갱신 (`backend.md`)

마이페이지 섹션의 다음 줄:
> `targetJobs` 는 `STUDENT`, `jobPosts` 는 `GRADUATE` 역할에서만 채워지고 그 외 역할에선 빈 리스트.

→ 다음으로 교체:
> **응답 contract**: 역할별 비해당 필드는 `null` 로 두고 `@JsonInclude(NON_NULL)` 가 응답 JSON 에서 키 자체를 제외한다. `targetJobs` 는 STUDENT 전용, `jobPosts` 는 GRADUATE 전용. `activity.roadmapCount` 는 STUDENT 전용 (GRADUATE 는 응답에서 제외). `MemberResponse` 의 `grade` (STUDENT 전용) / `businessCardImage`·`jobType`·`company`·`careerYear` (GRADUATE 전용) 도 동일 패턴. **`point` 는 마이페이지에선 항상 제외** (`MemberResponse.withoutPoint()` 헬퍼로 명시 null 처리, 향후 값이 채워져도 mypage 응답엔 미포함). 다른 엔드포인트(`GET /me` 등)에선 `point` 가 null 이면 제외, 값 있으면 포함. **빈 배열 `[]` 는 "역할 맞지만 0개" 의미로 보존**.

#### 프론트 호환성

- `?.length === 0` / `arr.length === 0` 체크: 필드 자체가 없어지면 `undefined` 도 falsy → 동일 동작
- `arr.map(...)` 직접 호출: 옵셔널 체이닝(`arr?.map(...)`) 필요. 프론트가 이미 사용 중이면 영향 없음.
- TypeScript 타입: 8개 필드 모두 optional(`?`) 로 변경 권장 (`targetJobs?`, `jobPosts?`, `activity.roadmapCount?`, `profile.grade?` 등)

#### 의존성·머지 우선순위

- **독립 TODO** — L~R 통합 머지 전략(4개 PR)과 충돌 없음
- 단, `MyPageResponse` + `MyPageServiceImpl.buildResponse` 가 PR2 (L+P) 에서 마지막으로 수정되는 파일이므로 **PR2 머지 후 진행이 안전**
- 추정 작업 범위: 코드 ~15줄 (어노테이션 + null 분기) + 테스트 8~10 케이스 + 문서 1줄 — **단독 PR 권장**

#### 커밋 메시지 예시

```
refactor(member): 마이페이지 응답 역할별 필드 전면 정리 (TODO S)

- MyPageResponse + ActivityStats + MemberResponse 에 @JsonInclude NON_NULL
- ActivityStats.roadmapCount: long → Long (GRADUATE 는 null)
- targetJobs(GRADUATE) / jobPosts(STUDENT) / grade(GRADUATE) /
  businessCardImage·jobType·company·careerYear(STUDENT) 모두 응답에서 키 자체 제외
- MemberResponse.withoutPoint() 헬퍼 신설 — 마이페이지에선 point 항상 제외
  (다른 엔드포인트는 정책 유지: null 일 때만 제외)
- "비해당 필드(null)" vs "있지만 0개([])" 의미 분리
- backend.md 마이페이지 섹션 갱신
```


### TODO T — Swagger UI "나의 활동" 그룹 통합 표시

> 발견 맥락: '나의 활동' 페이지(`MyActivityController` + 도메인별 분산)의 3개 엔드포인트가 Swagger UI 에서 각자 다른 컨트롤러 그룹("나의 활동", "로드맵", "질문")에 흩어져 있어, '나의 활동' 한 그룹에서 모아 보거나 통합 테스트하기 불편.

#### 현재 상태

| Swagger 그룹 | 표시되는 엔드포인트 |
|--------------|---------------------|
| "나의 활동" | `GET /api/v1/members/me/activity/coffeechats` 만 (1개) |
| "로드맵" | `GET /api/v1/roadmaps` + 다른 로드맵 API |
| "질문" | `GET /api/v1/questions/me` + 다른 Q&A API |

#### 목표

"나의 활동" 그룹에 3개 엔드포인트가 **모두** 표시되도록 변경. 원본 도메인 그룹("로드맵", "질문")에서도 계속 표시 — **양쪽 동시 표시는 springdoc-openapi 의도된 동작** (같은 엔드포인트를 두 가지 맥락에서 찾을 수 있게 함).

#### 변경 파일 (2개, 각 1줄)

1. **`RoadmapSwagger.getRoadmaps`** 메서드 위에 method-level `@Tag` 추가:
   ```java
   @Tag(name = "나의 활동")
   @Operation(...)
   ApiResponse<List<RoadmapResponse>> getRoadmaps(@LoginMember Member member);
   ```

2. **`QuestionSwagger.getMyQuestions`** 메서드 위에 method-level `@Tag` 추가:
   ```java
   @Tag(name = "나의 활동")
   @Operation(...)
   ApiResponse<List<QuestionResponse>> getMyQuestions(@LoginMember Member member);
   ```

> `MyActivitySwagger` 는 이미 클래스 레벨 `@Tag(name = "나의 활동")` 가 있어 그대로 두면 됨. 추가 작업 없음.

#### 변경 후 Swagger UI

```
[나의 활동]                          ← 통합 그룹
  GET /api/v1/members/me/activity/coffeechats
  GET /api/v1/roadmaps                 ← 추가됨
  GET /api/v1/questions/me             ← 추가됨

[로드맵]                              ← 원본 그룹 유지
  POST /api/v1/roadmaps
  GET  /api/v1/roadmaps                ← 양쪽 동시 표시
  GET  /api/v1/roadmaps/{id}
  DELETE /api/v1/roadmaps/{id}

[질문]                                ← 원본 그룹 유지
  POST /api/v1/questions
  GET  /api/v1/questions
  GET  /api/v1/questions/me            ← 양쪽 동시 표시
  GET  /api/v1/questions/{id}
  PATCH/DELETE/likes ...
```

#### 문서 갱신 (`backend.md`)

마이페이지 섹션의 "나의 활동 페이지" 항목 끝에 1줄 추가:
> **Swagger 그룹**: "로드맵"/"질문" 도메인 그룹 + "나의 활동" 보조 그룹에 동시 표시 (`RoadmapSwagger.getRoadmaps`·`QuestionSwagger.getMyQuestions` 에 method-level `@Tag(name = "나의 활동")` 부여). '나의 활동' 화면 기준으로 한 그룹에서 3개 엔드포인트 통합 테스트 가능.

#### 의존성·머지 우선순위

- **독립 TODO** — 다른 어떤 작업과도 충돌 없음 (L~R, S 모두 무관)
- 코드 변경: 2줄 (Swagger 메타데이터만), 테스트 영향 없음
- **단독 PR 권장** (극소규모)
- 언제든 진행 가능

#### 커밋 메시지 예시

```
docs(swagger): "나의 활동" 그룹에 로드맵·질문 me 엔드포인트 통합 표시 (TODO T)

- RoadmapSwagger.getRoadmaps + QuestionSwagger.getMyQuestions 에
  method-level @Tag(name = "나의 활동") 추가
- Swagger UI 에서 "나의 활동" 한 그룹으로 3개 엔드포인트 모아 테스트 가능
- 원본 도메인 그룹에서도 그대로 표시 (springdoc-openapi 의도된 중복)
- backend.md 마이페이지 섹션 갱신
```
