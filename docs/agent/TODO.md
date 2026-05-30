# 진행 예정 작업 (To-do)

> 이 파일은 루트 `CLAUDE.md` 가 `@import` 하지 않는다(자주 바뀌어 자동 로드 제외). 사용자가 TODO 관련 명령을 내리면 이 파일을 직접 읽고 진행한다. 완료 후 항목은 이 파일에서 삭제하고 `docs/agent/backend.md` 의 해당 도메인 섹션에 정식 규칙으로 통합한다.

> 사용자가 "to-do 리스트 수행" / "to-do 진행" / "TODO N 작업" 등 유사 명령을 내리면 아래 항목을 **즉시 코드 작성 → 테스트 보강 → CLAUDE.md 갱신 → 커밋/푸시 → PR 생성까지 일사천리로** 수행한다. 결정 사항은 이미 확정돼 있으니 다시 묻지 말고 명시된 디폴트로 진행할 것.
>
> 각 항목은 완료(머지) 후 이 섹션에서 삭제하고, 본문 도메인 섹션에 정식 규칙으로 통합 기록한다.

> **관찰 (의도 확인 필요)**: `ErrorStatus.getReason()`/`getReasonHttpStatus()` 가 실패 코드 enum 인데 `ErrorReasonDTO` 를 `.isSuccess(true)` 로 고정 생성한다(`SuccessStatus` 와 대비). 현재 `ApiResponse.onFailure` 가 `isSuccess=false` 를 따로 지정하므로 실제 응답엔 영향 없으나, 오해를 부르는 죽은 설정값이다 — 의도 확인 후 정리 여부 결정.


### TODO Z — 범용 파일 업로드 (presigned PUT URL) — 명함·포트폴리오·이미지 공통

**배경**
- 졸업생 마이페이지 '명함' 영역에서 파일을 선택하면 프론트는 브라우저 임시 객체 URL(`blob:http://…`)만 얻는다. 이 값은 해당 탭에서만 유효해 서버엔 무의미하다. 그런데 `PATCH /api/v1/members/mypage/graduate`(및 `/me`)의 `businessCardImage` 가 **string(URL)** 필드라 파일 자체를 실어 보낼 수 없다. 프론트는 blob URL 이면 전송을 막고 있어 **명함이 서버에 저장되지 않고**, view 모드에서 `businessCardImage=null` 로 보인다. 포트폴리오(`portfolio`)도 동일 구조의 문제.
- 기존에 `PATCH /me/business-card`·`/me/portfolio`·`/me/profile-image` 멀티파트 업로드 엔드포인트가 있으나, (1) 프론트 generated client 에 노출돼 있지 않고, (2) 업로드와 엔티티 갱신이 한 번에 묶여 있어, 프론트가 원하는 "**파일 업로드 → 실제 URL 수령 → 그 URL 을 마이페이지 PATCH 의 string 필드에 포함**" 범용 플로우와 결이 다르다. 프론트(유진)가 범용 업로드 API 를 요청했고 백엔드(세창)가 presigned URL 방식으로 합의.
- **(참고) 공고 링크 NPE 는 이미 코드상 해결됨**: 함께 제보된 `Cannot invoke "JobType.name()" because "jobType" is null`(마이페이지 500)은 `JobPostServiceImpl.create` 의 `jobType==null` 가드(이미 `main` 반영·`backend.md` 문서화)로 차단된다. 링크 전용 등록(`createFromLink`)은 알람 분기를 타지 않아 jobType=null 이어도 NPE 가 없다. 제보 환경에서 여전히 500 이면 **배포된 백엔드가 가드 머지 이전 빌드**이므로 동기화·재배포로 해소 — 본 TODO 범위 밖(별도 코드 변경 불필요).

**결정 (확정 — 다시 묻지 않음)**
- 업로드 방식 = **presigned PUT URL**(브라우저가 S3 로 직접 PUT). 백엔드 경유 직접 업로드 대신 백엔드는 presigned PUT URL + 최종 public URL 만 발급한다.
- 신규 엔드포인트 `POST /api/v1/files/presigned-url` 1개로 이미지·PDF 공통 처리. 용도 구분은 요청 `uploadType`(`IMAGE`|`PORTFOLIO`)으로 받아 content-type 화이트리스트를 분기.
- 키 생성·최종 URL 규약은 기존 `S3Service.convertToSaveName`/`getUrl` 재사용(동일 버킷·네이밍). presigner 빈은 기존 `S3Config.s3Presigner()` 재사용 — 신규 의존성 0.

**변경 (요지)**
- `S3Service` 에 **PUT presigned 발급** 추가 — `generateUploadPresignedUrl(String key, String contentType, Duration ttl)` (AWS SDK v2 `S3Presigner.presignPutObject` + `PutObjectPresignRequest` / `PutObjectRequest(bucket,key,contentType)`). 기존 `generatePresignedUrl`(GET 다운로드용)은 그대로 두고 PUT 발급만 신설.
- 발급 진입 헬퍼 — `createUploadPresign(originalFilename, contentType, allowedContentTypes, ttl)`: content-type 화이트리스트 검증(불일치 `_FILE_TYPE_NOT_ALLOWED`) → `convertToSaveName` 키 생성 → presigned PUT URL + `getUrl(key)` 최종 URL 을 담은 record 반환.
- 신규 컨트롤러 `FileController`(`POST /api/v1/files/presigned-url`) — `@LoginMember` 인증 필수. 요청 `PresignedUploadRequest(uploadType, fileName, contentType)`, 응답 `ApiResponse<PresignedUploadResponse(uploadUrl, fileUrl, key, expiresAt)>`. `uploadType` 별 화이트리스트: `IMAGE`={image/png, image/jpeg, image/webp}, `PORTFOLIO`={application/pdf}. (기존 `MemberServiceImpl.*_CONTENT_TYPES` 상수와 동일 값 — 공용 위치로 추출해 단일 정의 참조, 하드코딩·중복 금지.)
- 설정 `aws.s3.upload-presign-ttl`(기본 `PT5M`)을 `application-s3.yml` 에 외부화(기존 `download-presign-ttl` 선례와 동일), `S3Service` 에 `@Value` 주입.
- (선택) `ErrorStatus._FILE_PRESIGN_FAILED`(S3500) 추가 — presign 발급 자체 실패 시. content-type 위반은 기존 `_FILE_TYPE_NOT_ALLOWED`(S3400) 재사용.

**프론트 연동 계약 (2-step)**
1. `POST /api/v1/files/presigned-url` `{uploadType, fileName, contentType}` → `{uploadUrl, fileUrl, key, expiresAt}`.
2. 받은 `uploadUrl` 로 파일 바이트를 **`PUT`** (요청 헤더 `Content-Type` 은 1번에서 보낸 `contentType` 과 **동일해야** S3 가 수락 — presign 에 content-type 이 서명됨).
3. 업로드 성공 후 `fileUrl` 을 `PATCH /api/v1/members/mypage/graduate` 의 `businessCardImage`(명함)·`portfolio`(포폴) string 필드에 넣어 저장.

**S3 버킷 CORS (인프라 선행 필수)**
- 브라우저 직접 PUT 을 위해 public 버킷 CORS 허용 필요: `AllowedMethods:[PUT]`, `AllowedOrigins:[http://localhost:5173, <운영 도메인>]`, `AllowedHeaders:["*"]`, `ExposeHeaders:[ETag]`. 미설정 시 브라우저 PUT 이 CORS 로 차단된다. (코드 외 인프라 작업 — 구현 PR 전/동시 처리.)

**제약 / 주의**
- **크기 제한은 presigned PUT 으로 서버 강제가 불가** — 직접 업로드(`uploadFile`)의 `maxBytes` 검증이 이 경로엔 없다. 1차 대응: 프론트 클라이언트단 크기 검증 + (선택) S3 버킷 정책/라이프사이클 상한. 엄격 강제가 필요하면 presigned POST(`content-length-range` 조건) 또는 업로드 후 HEAD 검증 잡으로 후속 보강(범위 밖). 본 TODO 는 content-type 화이트리스트 + presign content-type 핀까지만 강제.
- 기존 `PATCH /me/business-card`·`/me/portfolio`·`/me/profile-image` 멀티파트 엔드포인트는 **유지**(비파괴). 신규 범용 업로드와 병존하며, 명함/포폴 저장 플로우만 마이페이지 string 필드 방식으로 일원화.

**파일 (신규 5, 수정 2~3)**
- 신규: `src/main/java/mju/capstone/ddingconnect/global/aws/controller/FileController.java`
- 신규: `src/main/java/mju/capstone/ddingconnect/global/aws/controller/FileSwagger.java`
- 신규: `src/main/java/mju/capstone/ddingconnect/global/aws/dto/PresignedUploadRequest.java` (`uploadType`(enum)·`fileName`·`contentType`, `@NotNull`/`@NotBlank`)
- 신규: `src/main/java/mju/capstone/ddingconnect/global/aws/dto/PresignedUploadResponse.java` (`uploadUrl`·`fileUrl`·`key`·`expiresAt`)
- 신규: `src/main/java/mju/capstone/ddingconnect/global/aws/UploadType.java` (IMAGE/PORTFOLIO + 허용 content-type 집합 매핑)
- 수정: `src/main/java/mju/capstone/ddingconnect/global/aws/S3Service.java` — `generateUploadPresignedUrl`/`createUploadPresign` + `upload-presign-ttl` `@Value`
- 수정: `src/main/resources/application-s3.yml` — `aws.s3.upload-presign-ttl: PT5M`
- 수정(선택): `src/main/java/mju/capstone/ddingconnect/global/response/code/status/ErrorStatus.java` — `_FILE_PRESIGN_FAILED`

**테스트**
- 신규/확장 `S3ServiceTest` — content-type 위반 시 `_FILE_TYPE_NOT_ALLOWED`, presigned PUT URL 발급 + 최종 `fileUrl` 규약(`getUrl(key)` 일치), ttl 반영 검증.
- 신규 `FileControllerTest` — `POST /files/presigned-url` 인증 가드(`WithMockLoginMember`), `uploadType` 별 화이트리스트 분기, 응답 jsonPath(`$.result.uploadUrl`/`$.result.fileUrl`/`$.result.expiresAt`), 비허용 content-type 400.
- 하드코딩 금지 — 화이트리스트/ttl/경로 prefix 는 상수·설정값으로 참조(테스트도 동일 심볼 참조).

**문서 갱신 (구현 PR 에서 동반)**
- `docs/agent/backend.md` `### 회원 (Member)` 의 S3/업로드 단락 — 범용 presigned PUT 업로드 엔드포인트 + 2-step 계약 + CORS 선행 + 크기 강제 한계 명시.
- `docs/agent/backend.md` `### 마이페이지 (member)` — 명함/포폴 저장이 "파일 업로드(presigned) → string URL 필드 PATCH" 플로우임을 명시.
- 완료(머지) 후 본 TODO 항목은 이 파일에서 삭제.

**Breaking change**
- 신규 엔드포인트 추가(기존 API 시그니처 무변경). 프론트는 명함/포폴 저장을 2-step(presigned 업로드 → URL PATCH)로 전환 필요 — 백엔드/프론트 동시 반영.


### 11개 작업자 노트 (TODO #1~#11 머지 완료 후 보존되는 일반 가이드)

- **브랜치 정책**: 각 TODO 를 **개별 브랜치 + 개별 PR** 로 처리하는 것을 기본으로 한다. 영향 범위가 큰 TODO 는 단독 PR 필수. 같은 도메인 내 작은 변경은 묶어서 1개 PR 도 허용 (작업자 판단). 사용자가 "TODO N 작업" 으로 단일 항목 지목 시 그 항목만 단독 PR.
- **공통 커밋 메시지 컨벤션**: `feat(<도메인>): ...` / `fix(<도메인>): ...` / `refactor(<도메인>): ...` / `docs(CLAUDE.md): ...` prefix. 마지막 줄에 항상 `https://claude.ai/code/session_...` 포함.
- **Swagger 검증**: 가능하면 PR 본문 Test plan 에 Swagger 시나리오 체크박스 포함.
- **테스트 실패 시**: `--no-verify` 등으로 우회 금지. 원인 분석 후 수정.


### 백엔드 레포 PR 전략 — TODO V/W/U/X 의 `ddingconnect-backend` 이전

`cluade_clone` 의 PR #88/#89/#90/#91/#92/#93 으로 `main` 에 머지된 V/W/U/X 변경을 백엔드 레포(`mju-capstone-4/ddingconnect-backend`) 로 옮기기 위한 작업 분할표. **4개 이슈로 나눠 순차 PR 권장** — 일부 파일이 여러 이슈에서 동시 변경돼 병렬 시 충돌 가능. 권장 머지 순서: **1 → 2 → 3 → 4**.

파일 경로는 백엔드 레포 루트 기준 (`backend/` 접두사 없음).

#### 이슈 1 — `{N}-feat-roadmap-pdf-download-sync` (V+W+U + RFC 5987 + Swagger 라벨)

**신규 (4 파일)**
- `src/main/resources/fonts/NotoSansKR-Regular.ttf` (Noto Sans KR Regular, SIL OFL 1.1)
- `src/main/java/mju/capstone/ddingconnect/domain/roadmap/service/RoadmapPdfRenderer.java`
- `src/main/java/mju/capstone/ddingconnect/domain/roadmap/dto/response/RoadmapDownloadResponse.java`
- `src/test/java/mju/capstone/ddingconnect/domain/roadmap/service/RoadmapPdfRendererTest.java`

**수정 (10 파일)**
- `build.gradle` — `com.github.librepdf:openpdf:1.3.34` 의존성 추가
- `src/main/resources/application-s3.yml` — `aws.s3.download-presign-ttl` (기본 `PT5M`) 추가
- `src/main/java/mju/capstone/ddingconnect/global/config/S3Config.java` — `S3Presigner` 빈
- `src/main/java/mju/capstone/ddingconnect/global/aws/S3Service.java` — `uploadBytes(byte[], key, contentType)` / `generatePresignedUrl(key, ttl, fileName)` 추가 (RFC 5987 한글 파일명 처리 포함)
- `src/main/java/mju/capstone/ddingconnect/domain/roadmap/service/RoadmapService.java` — `getDownloadUrl(Member, Long)` 메서드 추가
- `src/main/java/mju/capstone/ddingconnect/domain/roadmap/service/RoadmapServiceImpl.java` — `create` 동일 트랜잭션 안 PDF 렌더+S3 업로드 통합, `delete` 시 S3 best-effort cleanup, `getDownloadUrl` 구현, title 추출/sanitize/폴백 헬퍼
- `src/main/java/mju/capstone/ddingconnect/domain/roadmap/controller/RoadmapController.java` — `GET /{roadmapId}/download` 엔드포인트 추가, javadoc `"로드맵 생성"`
- `src/main/java/mju/capstone/ddingconnect/domain/roadmap/controller/RoadmapSwagger.java` — 다운로드 swagger 어노테이션, `summary = "로드맵 생성"` (구 `"로드맵 등록"`)
- `src/test/java/mju/capstone/ddingconnect/domain/roadmap/service/RoadmapServiceImplTest.java` — 신규 6 테스트 (PDF 업로드 위임, S3 삭제 swallow, presigned URL 발급, 권한, 미존재, sanitize/폴백)
- `src/test/java/mju/capstone/ddingconnect/domain/roadmap/controller/RoadmapControllerTest.java` — `GET /{id}/download` 1 테스트 + DisplayName 정합

#### 이슈 2 — `{N}-feat-roadmap-detail-owner-check-sync` (상세 조회 본인 검증)

**수정 (6 파일)**
- `src/main/java/mju/capstone/ddingconnect/domain/roadmap/service/RoadmapService.java` — `getOne(Long)` → `getOne(Member, Long)`
- `src/main/java/mju/capstone/ddingconnect/domain/roadmap/service/RoadmapServiceImpl.java` — 본인 소유 검증 (`ROADMAP_UNAUTHORIZED`)
- `src/main/java/mju/capstone/ddingconnect/domain/roadmap/controller/RoadmapController.java` — `@LoginMember Member` 주입
- `src/main/java/mju/capstone/ddingconnect/domain/roadmap/controller/RoadmapSwagger.java` — `responses` 200/403/404 명시
- `src/test/java/mju/capstone/ddingconnect/domain/roadmap/service/RoadmapServiceImplTest.java` — `getOne` 3 테스트 (성공·UNAUTHORIZED·NOT_FOUND)
- `src/test/java/mju/capstone/ddingconnect/domain/roadmap/controller/RoadmapControllerTest.java` — mock 시그니처 `(any(), eq(...))` 갱신

#### 이슈 3 — `{N}-feat-roadmap-list-lightweight-sync` (목록 응답 카드 경량 DTO)

**신규 (1 파일)**
- `src/main/java/mju/capstone/ddingconnect/domain/roadmap/dto/response/RoadmapListResponse.java` — `(id, title, createdAt)` + `roadmap_title` 추출 헬퍼 + `"로드맵"` 폴백

**수정 (6 파일)**
- `src/main/java/mju/capstone/ddingconnect/domain/roadmap/service/RoadmapService.java` — `getList` 리턴 타입 `List<RoadmapListResponse>`
- `src/main/java/mju/capstone/ddingconnect/domain/roadmap/service/RoadmapServiceImpl.java` — 매핑 `RoadmapListResponse::from` 으로 교체
- `src/main/java/mju/capstone/ddingconnect/domain/roadmap/controller/RoadmapController.java` — 리턴 타입
- `src/main/java/mju/capstone/ddingconnect/domain/roadmap/controller/RoadmapSwagger.java` — 리턴 타입 + description (`content` 미포함 명시)
- `src/test/java/mju/capstone/ddingconnect/domain/roadmap/service/RoadmapServiceImplTest.java` — `getList` title 검증 + 폴백 1 테스트
- `src/test/java/mju/capstone/ddingconnect/domain/roadmap/controller/RoadmapControllerTest.java` — `jsonPath` title 노출 + `content`/`memberId` 미노출 검증

#### 이슈 4 — `{N}-feat-roadmap-create-loginmember-sync` (POST 회원 식별 결함 정합, **breaking**)

**수정 (3 파일)**
- `src/main/java/mju/capstone/ddingconnect/domain/roadmap/controller/RoadmapController.java` — `@RequestParam Long memberId` 제거, `@LoginMember Member member` 주입, `roadmapService.create(member.getId(), request)` 위임
- `src/main/java/mju/capstone/ddingconnect/domain/roadmap/controller/RoadmapSwagger.java` — `@RequestParam` Swagger 어노테이션 제거
- `src/test/java/mju/capstone/ddingconnect/domain/roadmap/controller/RoadmapControllerTest.java` — 쿼리 파라미터 제거, `WithMockLoginMember` 적용

**Breaking change**: 프론트가 `?memberId=` 쿼리 미부착으로 호출하도록 동시 변경 필요.

#### 충돌 가능 파일 (이슈 간 동시 변경)

병렬 작업 시 다음 파일들이 같은 영역을 건드려 충돌 가능 — 한 이슈씩 직렬 머지 권장:

- `RoadmapService.java` — 이슈 1(`getDownloadUrl` 추가) + 이슈 2(`getOne` 시그니처) + 이슈 3(`getList` 리턴 타입)
- `RoadmapServiceImpl.java` — 이슈 1(`create`/`delete`/`getDownloadUrl` 구현) + 이슈 2(`getOne` 본인 검증) + 이슈 3(`getList` 매핑)
- `RoadmapController.java` — 이슈 1(`/download`) + 이슈 2(`@LoginMember`) + 이슈 3(리턴 타입) + 이슈 4(`@LoginMember`, `@RequestParam` 제거)
- `RoadmapSwagger.java` — 이슈 1(`/download` + 라벨) + 이슈 2(`responses`) + 이슈 3(리턴 타입) + 이슈 4(`@RequestParam` 제거)
- `RoadmapServiceImplTest.java` — 이슈 1·2·3 모두 신규 테스트 추가
- `RoadmapControllerTest.java` — 이슈 1·2·3·4 모두 갱신

