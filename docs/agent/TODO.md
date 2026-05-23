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
