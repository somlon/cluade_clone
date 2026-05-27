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


### TODO L~T 통합 머지 전략 (ddingconnect-backend 동기화용)

> `cluade_clone` 에서는 L~R 7개가 PR #73~#80 으로, S+T 가 PR #84 로 **개별** 머지됐다(총 9개 TODO). 이를 다음 동기화 대상(`mju-capstone-4/ddingconnect-backend`)으로 옮길 때는 같은 파일을 건드리는 항목을 묶어 **4개 PR 로 압축**한다. 머지 순서는 파일 충돌·서비스 의존성 기준.

#### 충돌 분석

| 파일 | L | M | N | O | P | Q | R | S | T |
|------|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| `MemberController` | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |   |   |   |
| `MyPageServiceImpl` (or 위임 서비스) | ✓ |   |   |   | ✓ |   |   | ✓ |   |
| `MemberServiceImpl` |   | ✓ | ✓ | ✓ |   | ✓ |   |   |   |
| `MyPageResponse` | ✓ |   |   |   | ✓ |   |   | ✓ |   |
| `MemberResponse` |   |   |   |   |   |   |   | ✓ |   |
| `UpdateXxxMyPageRequest` (DTO 신규) | ✓ |   |   |   |   |   |   |   |   |
| `S3Service` |   |   | ✓ | ✓ |   | ✓ |   |   |   |
| `RoadmapController` |   |   |   |   | ✓ |   |   |   |   |
| `RoadmapSwagger` |   |   |   |   |   |   |   |   | ✓ |
| `QuestionSwagger` |   | ✓ |   |   |   |   |   |   | ✓ |
| `JobPostController` / `JobPostServiceImpl` |   |   |   |   |   |   | ✓ |   |   |
| `ErrorStatus` |   |   | ✓ | ✓ |   | ✓ | ✓ |   |   |

핵심 충돌 구간:
- `MyPageResponse` + `MyPageServiceImpl.buildResponse` — **L + P + S** (activity 분기 / 역할별 응답 / 역할별 필드 정리·`@JsonInclude(NON_NULL)`)
- `S3Service` — **N + O + Q** (O 가 foundation, N/Q 가 사용)
- `MemberController` — 다수 PR 이 새 endpoint 메서드 추가 → 메서드 단위 분리되어 있어 순차 머지로 회피
- `QuestionSwagger` — **M + T** (M 이 `getMyQuestions` 신설, T 가 같은 메서드에 `@Tag` 부여)

#### 의존성

- **L → R**: L 의 `UpdateGraduateMyPageRequest.jobPostsToAdd` 가 R 의 `JobPostService.createFromLink` 호출
- **N, Q → O**: N (profileImage), Q (businessCard) 가 O 의 `S3Service.uploadFile(MultipartFile, Set<String>, long)` 호출
- **S → L + P**: S 가 L 추가의 `MyPageServiceImpl` 와 P 추가의 `buildResponse` roadmapCount STUDENT 분기 위에서 동작 (`long 0L → Long null` 로 교체)
- **T → M**: T 의 `QuestionSwagger.getMyQuestions` `@Tag` 부여는 M 이 신설한 메서드 위에 얹힘 (RoadmapSwagger.getRoadmaps 측 변경은 pre-existing 메서드라 L~R 의존 없음)

#### PR 분할 (4개)

##### PR1 — `feat(job-post): 졸업생 링크 공고 + 선배/일반 분리 조회` (TODO R)

- 엔드포인트:
  - `POST /api/v1/job-post/link` (GRADUATE 전용, `detailUrl` 만 입력)
  - `GET /api/v1/job-post/graduates` (선배 매핑 있음)
  - `GET /api/v1/job-post/crawled` (선배 매핑 없음, 빈 IN 폴백 포함)
- 주요 파일: `JobPostController`, `JobPostServiceImpl` (`createFromLink`, `getGraduatePosts`, `getCrawledPosts`, `create` 의 `jobType=null` 가드), `CreateJobPostLinkRequest` DTO, `GraduatePostResponse` DTO, `GraduateJobPostRepository.findDistinctPostContentsIds`, `PostContentsRepository.findByIdNotIn`, `ErrorStatus.POST403`
- 의존: 없음 (다른 PR 영향 없음)
- **머지 순서 1순위** — PR2 의 `createFromLink` 호출 컴파일 의존

##### PR2 — `feat(member,roadmap): 마이페이지 역할별 분리 + 로드맵 가드 + 응답 필드 정리` (TODO L + P + S)

- 엔드포인트:
  - `PATCH /api/v1/members/mypage/student` (L 신규)
  - `PATCH /api/v1/members/mypage/graduate` (L 신규)
  - `GET /api/v1/members/mypage` (P, `activity.roadmapCount` STUDENT 분기 + S 가 GRADUATE 시 null 로 교체해 응답 키 제외)
  - `GET /api/v1/roadmaps` (P, GRADUATE 차단)
- 주요 파일: `MemberController` (mypage PATCH 2개), `MyPageService(Impl)` (`updateStudentMyPage`/`updateGraduateMyPage`, `buildResponse` 가 S 의 `withoutPoint` + 역할별 null 분기로 최종 형태), `MyPageResponse`(+`ActivityStats`, S 가 `@JsonInclude(NON_NULL)` + `roadmapCount: Long`), `MemberResponse` (S 가 `@JsonInclude(NON_NULL)` + `withoutPoint()` 헬퍼 신설), `UpdateStudentMyPageRequest`/`UpdateGraduateMyPageRequest`, `UpdateStudentProfileRequest`/`UpdateGraduateProfileRequest`, `RoadmapController` (`getRoadmaps` GRADUATE 가드)
- 통합 이유: L + P + S 셋 다 `MyPageResponse` + `MyPageServiceImpl.buildResponse` 를 수정 → 분리 시 3-way 충돌. S 는 P 의 STUDENT 분기 위에 얹혀 의미가 완성되므로 한 PR 로 합치는 게 자연스러움.
- 의존: PR1 (`JobPostService.createFromLink`)
- **머지 순서 2순위**

##### PR3 — `feat(member,coffeechat,qna,swagger): 나의 활동 페이지 API + Swagger 그룹 통합` (TODO M + T)

- 엔드포인트:
  - `GET /api/v1/questions/me` (Q&A 본인 글, M 신설 / T 가 `@Tag(name = "나의 활동")` 부여)
  - `GET /api/v1/members/me/activity/coffeechats` (커피챗 카드, 상대방 정보 포함)
  - `GET /api/v1/roadmaps` (기존 활용, M 은 변경 없음 / T 가 `RoadmapSwagger.getRoadmaps` 에 `@Tag(name = "나의 활동")` 부여)
- 주요 파일: `QuestionController` (`getMyQuestions`), `QuestionService(Impl)` (`getMyQuestions`, `questionRepository.findByMemberId`), `QuestionSwagger` (M 이 `getMyQuestions` 신설 + T 가 `@Tag` 부여), `CoffeeChatService(Impl)` (`getMyActivities`), `MyActivityController` (신규), `CoffeeChatPartnerResponse` DTO, `RoadmapSwagger` (T 가 `getRoadmaps` 에 `@Tag` 부여)
- 통합 이유: T 의 `QuestionSwagger.getMyQuestions` `@Tag` 가 M 의 신설 메서드 위에 얹혀 분리 시 직렬 의존. RoadmapSwagger 변경은 단독이지만 같은 "나의 활동" 그룹 통합 의도라 함께 묶음.
- 의존: 없음 (독립 도메인, PR1·PR2 와 무관)
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
PR2 (L+P+S, 마이페이지 + 로드맵 가드 + 응답 필드 정리)
  ↓
PR3 (M+T, 활동 페이지 + Swagger 그룹 통합)
  ↓
PR4 (N+O+Q, 파일 업로드 3종)
```

`MemberController` 가 PR2/PR3/PR4 에 공통 등장 — 머지 순서 직렬화로 충돌 방지. `MyPageResponse`/`MyPageServiceImpl.buildResponse` 는 PR2 에서 L+P+S 의 최종 형태로 한 번에 머지되어 추후 충돌 없음.

#### 로컬 검증 상태 (2026-05-23, ddingconnect-backend)

위 4개 PR 분할 안에 해당하는 작업을 `ddingconnect-backend` 로컬에 미리 적용해 7개 TODO(L~R) end-to-end 검증 완료. **30/31 PASS**. 1건은 환경 설정 차이로 백엔드 검증 로직 미트리거(기능적으로는 차단됨):

| TODO | PASS/전체 | 핵심 검증 |
|------|----------|-----------|
| L | 4/4 | role guard 400 + Bean Validation 400 |
| M | 3/3 | `/questions/me` 본인 스코프 + partner 정보 매핑 |
| N | 4/5 | PNG 200 / GIF·5MB+ 400. 5MB+ 는 Tomcat 413 차단 (앱 `_FILE_TOO_LARGE` 미트리거) |
| O | 4/4 | PDF 200 / PNG 400. `uploadImage` 회귀 없음 |
| P | 4/4 | GRADUATE `/roadmaps` 400 + mypage.activity.roadmapCount=0 + 단건 조회 200 |
| Q | 4/4 | STUDENT 400 MEMBER400 (S3 호출 전 차단) + GRADUATE 다른 필드 보존 |
| R | 7/7 | 링크 등록 200 + STUDENT 403 + 선배/일반 분리 + 합계 일치 |

**S, T 는 `cluade_clone` PR #84 머지 후 추가됨 — `ddingconnect-backend` 동기화 시 함께 검증해야 한다.** 권장 검증 시나리오:

| TODO | 핵심 검증 |
|------|-----------|
| S | STUDENT 응답에 `jobPosts`/`businessCardImage`/`jobType`/`company`/`careerYear`/`point` 키 부재 + `targetJobs: []` 보존 / GRADUATE 응답에 `targetJobs`/`grade`/`activity.roadmapCount`/`point` 키 부재 + `jobPosts: []` 보존 / `GET /me` 등 다른 엔드포인트는 `point` 값 있으면 노출 (정책 분리) |
| T | Swagger UI "나의 활동" 그룹에 3개 엔드포인트(coffeechats, roadmaps, questions/me) 모두 표시 + 원본 그룹("로드맵", "질문")에도 그대로 표시 |

별도 환경 메모 (TODO 결함 아님):
- MySQL utf8mb4 미설정 → 한글 필드 `?????` 저장 (테스트는 영문 입력으로 회피)
- `spring.servlet.multipart.max-file-size: 5MB` 설정으로 N-(d) 가 Tomcat 413 으로 차단 — 앱 `_FILE_TOO_LARGE` 검증을 직접 보고 싶으면 max-file-size 를 10MB 로 늘리고 컨트롤러/서비스에서 5MB 자체 체크


### TODO U — 로드맵 다운로드 엔드포인트 신설 (파일 URL 반환)

> **결정**: 로드맵 결과 페이지에서 다운로드 버튼을 누르면 백엔드는 **파일 URL(JSON)** 을 반환한다. 파일 바이너리 직접 반환(`ResponseEntity<byte[]>`) 이나 리다이렉트 방식은 채택하지 않는다. 파일 자체는 S3 에 저장되고, 응답은 만료가 짧은 presigned URL.

- 엔드포인트: `GET /api/v1/roadmaps/{roadmapId}/download` (`RoadmapController` 메서드 추가)
- 응답: `ApiResponse<RoadmapDownloadResponse>` — `{ fileUrl: String, fileName: String, expiresAt: LocalDateTime }`
- 권한 검증: `delete()` 와 동일한 본인 소유 검증 — `!roadmap.getMember().getId().equals(member.getId())` → `ROADMAP_UNAUTHORIZED`(403). 미존재는 `ROADMAP_NOT_FOUND`(404).
- 서비스: `RoadmapService.getDownloadUrl(Member, Long roadmapId)` → `RoadmapDownloadResponse` 반환. 내부적으로 S3 키 규약(TODO W)으로 presigned URL 발급.
- 데이터 흐름: 프론트 다운로드 버튼 클릭 → 이 엔드포인트 호출 → JSON 의 `fileUrl` 을 받아 프론트가 `<a href=fileUrl download>` 또는 `window.location.href = fileUrl` 로 실제 파일 가져오기.
- Swagger: `RoadmapSwagger.downloadRoadmap` 신설 — 응답 예시에 presigned URL 형태 포함, 404/403 에러 코드 명시.
- 의존: TODO V (PDF 변환), TODO W (S3 업로드 + presigned URL 발급)
- 머지 순서: V → W → U 직렬 의존 (U 의 서비스 호출이 W 의 메서드를 컴파일 의존)


### TODO V — 로드맵 PDF 변환 로직

> **결정**: 다운로드 파일 포맷은 **PDF**. JSON 텍스트 다운로드는 채택하지 않는다(사용자 가독성·표준 다운로드 경험 우선).

- 라이브러리: `backend/build.gradle` 에 OpenPDF 의존성 추가 — `com.github.librepdf:openpdf:1.3.x` (LGPL/MPL, 상업 사용 자유). iText 7 는 AGPL 부담으로 미채택.
- 신규 컴포넌트: `RoadmapPdfRenderer` (`backend/src/main/java/mju/capstone/ddingconnect/domain/roadmap/service/RoadmapPdfRenderer.java`) — 메서드 `byte[] render(String content)` 가 `Roadmap.content`(JSON 문자열)를 PDF 바이트 배열로 변환.
- JSON 파싱: 데이터 파트 `RoadmapResponse` 스키마와 정합 — `roadmap_title` / `steps[].phase_badge` / `steps[].title` / `steps[].details[].category` / `steps[].details[].content` / `recommended_certifications` / `recommended_activities` / `summary_advice`. `ObjectMapper` 로 내부 record 매핑.
- 레이아웃: Figma 결과 화면 카드 형식을 PDF 페이지로 옮긴다 — (1) 표지 제목 = `roadmap_title`, (2) 3단계 step 카드 = `phase_badge` 칩 + `title` + `details` 항목 리스트, (3) 추천 자격증·활동 칩 영역, (4) 마지막 페이지에 `summary_advice` 한마디.
- 한글 폰트 필수: `backend/src/main/resources/fonts/NotoSansKR-Regular.ttf` 추가 후 `BaseFont.createFont(...)` 로 등록 — 미설정 시 한글이 깨진다.
- 호출 시점: 로드맵 생성 직후(`RoadmapServiceImpl.create`)에 **1회만** 렌더링해 S3 업로드(TODO W). 이후 다운로드 요청은 S3 객체 재사용 → 매 요청마다 PDF 렌더링하지 않음(생성 시 dependency, 다운로드 응답 지연 최소화).
- 의존: 없음 (단독 렌더링 모듈)
- 머지 순서: 1순위


### TODO W — S3 업로드 + presigned URL 발급 (로드맵 다운로드용)

> **결정**: 파일 자체는 **S3 에 저장**, 응답은 **presigned URL(GET, 만료 5분)** 형태. 회원 포트폴리오용 기존 `S3Service` 인프라를 재사용한다.

- `S3Service` 확장 (`backend/src/main/java/mju/capstone/ddingconnect/global/aws/S3Service.java`):
  - `String uploadBytes(byte[] bytes, String key, String contentType)` 신규 — `RoadmapPdfRenderer.render(...)` 결과 업로드용 (`MultipartFile` 외 `byte[]` 경로). 기존 `uploadFile`/`uploadImage` 시그니처는 그대로 유지.
  - `String generatePresignedUrl(String key, Duration ttl)` 신규 — `S3Presigner.presignGetObject(...)` 로 GET presigned URL 발급. AWS SDK v2 `s3-presigner` 의존성 `backend/build.gradle` 추가 필요.
- TTL: 5분 (`Duration.ofMinutes(5)`) — `application-s3.yml` 의 `aws.s3.download-presign-ttl` 설정값으로 외부화. 짧게 둬서 URL 유출 시 노출 시간 최소화.
- S3 키 규약: `roadmaps/{roadmapId}.pdf` — 결정론적 키라 별도 `Roadmap.contentUrl` 컬럼 신설 불필요. `RoadmapServiceImpl` 의 `private static final String S3_KEY_PREFIX = "roadmaps/";` 상수로 추출해 변경 여지 확보.
- 저장 시점: `RoadmapServiceImpl.create()` 의 `roadmapRepository.save(...)` 직후, 같은 `@Transactional` 안에서 (1) `byte[] pdf = roadmapPdfRenderer.render(content)` (2) `s3Service.uploadBytes(pdf, "roadmaps/" + saved.getId() + ".pdf", "application/pdf")`. S3 업로드 실패 → 본체 저장 롤백 → dangling DB row 방지. 외부 HTTP 호출이 트랜잭션 안에 포함되는 점은 데이터 파트 AI 호출 패턴(`RoadmapAiClient.generate`)과 동일.
- 다운로드 응답 파일명: `RoadmapDownloadResponse.fileName` = `"{roadmap_title}.pdf"` (특수문자 sanitize). presigned URL 의 `response-content-disposition` 쿼리 파라미터로 `attachment; filename="..."` 지정해 브라우저 다운로드 강제.
- 삭제 캐스케이드: `RoadmapServiceImpl.delete()` 에서 `RoadmapAlarm` 삭제 후 S3 객체도 함께 삭제(`s3Service.deleteImage(key)` 재사용 — 메서드명이 image 지만 임의 key 삭제 가능, backend.md `S3Service 일반화` 항목 정합). DB 삭제와 S3 삭제 간 정합성은 best-effort (S3 삭제 실패 시 로그만, 트랜잭션 영향 없음 — dangling S3 객체는 추후 cleanup job 도입 검토).
- 의존: TODO V (`RoadmapPdfRenderer.render` 호출 컴파일 의존)
- 머지 순서: 2순위
- 회귀 보장: 기존 `S3Service.uploadFile(MultipartFile, Set<String>, long)` / `uploadImage(MultipartFile)` / `deleteImage(String)` 시그니처 모두 유지. 기존 호출처(포트폴리오 PDF · 프로필 이미지 · 명함 이미지 · 회원가입 증명서) 영향 없음.


#### TODO U+V+W PR 분할

세 항목 모두 같은 도메인(`roadmap`)이고 V→W→U 직렬 의존이라 분리 시 중간 단계가 사용자 노출 변경 0(V/W 단독 머지는 의미 없음). 단일 PR `feat(roadmap): 로드맵 PDF 다운로드 (S3 presigned URL 반환)` 로 V+W+U 를 함께 머지 권장. 머지 후 본문은 `docs/agent/backend.md` 의 `### 로드맵 (roadmap)` 섹션에 정식 규칙으로 통합한다 (다운로드 흐름·S3 키 규약·presigned TTL·PDF 렌더링 규칙).



