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

- ~~신규 DTO `CreateJobPostLinkRequest(String detailUrl)`~~ — **TODO L 작업 시 선행 신설 완료** (`job_post/dto/request/CreateJobPostLinkRequest.java`, `@NotBlank` + `^https?://.+` `@Pattern`).
- ~~서비스 동작~~ — **TODO L 작업 시 `JobPostService.createFromLink` 선행 신설 완료** (`PostContents.builder().detailUrl(...).build()` 저장 → `GraduateJobPost` 매핑 + 알람 분기 스킵). `UpdateGraduateMyPageRequest.jobPostsToAdd` 가 이미 이 메서드에 위임 중.
- 신규 엔드포인트 `POST /api/v1/job-posts/link` — 본 TODO R 작업의 잔여 항목. `JobPostController`/`JobPostSwagger` 에 메서드 추가 + 기존 서비스 메서드(`createFromLink`)에 위임.

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
