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

