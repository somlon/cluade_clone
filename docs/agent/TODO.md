# 진행 예정 작업 (To-do)

> 이 파일은 루트 `CLAUDE.md` 가 `@import` 하지 않는다(자주 바뀌어 자동 로드 제외). 사용자가 TODO 관련 명령을 내리면 이 파일을 직접 읽고 진행한다. 완료 후 항목은 이 파일에서 삭제하고 `docs/agent/backend.md` 의 해당 도메인 섹션에 정식 규칙으로 통합한다.

> 사용자가 "to-do 리스트 수행" / "to-do 진행" / "TODO N 작업" 등 유사 명령을 내리면 아래 항목을 **즉시 코드 작성 → 테스트 보강 → CLAUDE.md 갱신 → 커밋/푸시 → PR 생성까지 일사천리로** 수행한다. 결정 사항은 이미 확정돼 있으니 다시 묻지 말고 명시된 디폴트로 진행할 것.
>
> 각 항목은 완료(머지) 후 이 섹션에서 삭제하고, 본문 도메인 섹션에 정식 규칙으로 통합 기록한다.

> **관찰 (의도 확인 필요)**: `ErrorStatus.getReason()`/`getReasonHttpStatus()` 가 실패 코드 enum 인데 `ErrorReasonDTO` 를 `.isSuccess(true)` 로 고정 생성한다(`SuccessStatus` 와 대비). 현재 `ApiResponse.onFailure` 가 `isSuccess=false` 를 따로 지정하므로 실제 응답엔 영향 없으나, 오해를 부르는 죽은 설정값이다 — 의도 확인 후 정리 여부 결정.

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

### 11개 작업자 노트 (TODO #1~#11 머지 완료 후 보존되는 일반 가이드)

- **브랜치 정책**: 각 TODO 를 **개별 브랜치 + 개별 PR** 로 처리하는 것을 기본으로 한다. 영향 범위가 큰 TODO 는 단독 PR 필수. 같은 도메인 내 작은 변경은 묶어서 1개 PR 도 허용 (작업자 판단). 사용자가 "TODO N 작업" 으로 단일 항목 지목 시 그 항목만 단독 PR.
- **공통 커밋 메시지 컨벤션**: `feat(<도메인>): ...` / `fix(<도메인>): ...` / `refactor(<도메인>): ...` / `docs(CLAUDE.md): ...` prefix. 마지막 줄에 항상 `https://claude.ai/code/session_...` 포함.
- **Swagger 검증**: 가능하면 PR 본문 Test plan 에 Swagger 시나리오 체크박스 포함.
- **테스트 실패 시**: `--no-verify` 등으로 우회 금지. 원인 분석 후 수정.
