# 진행 예정 작업 (To-do)

> 이 파일은 루트 `CLAUDE.md` 가 `@import` 하지 않는다(자주 바뀌어 자동 로드 제외). 사용자가 TODO 관련 명령을 내리면 이 파일을 직접 읽고 진행한다. 완료 후 항목은 이 파일에서 삭제하고 `docs/agent/backend.md` 의 해당 도메인 섹션에 정식 규칙으로 통합한다.

> 사용자가 "to-do 리스트 수행" / "to-do 진행" / "TODO N 작업" 등 유사 명령을 내리면 아래 항목을 **즉시 코드 작성 → 테스트 보강 → CLAUDE.md 갱신 → 커밋/푸시 → PR 생성까지 일사천리로** 수행한다. 결정 사항은 이미 확정돼 있으니 다시 묻지 말고 명시된 디폴트로 진행할 것.
>
> 각 항목은 완료(머지) 후 이 섹션에서 삭제하고, 본문 도메인 섹션에 정식 규칙으로 통합 기록한다.

> **관찰 (의도 확인 필요)**: `ErrorStatus.getReason()`/`getReasonHttpStatus()` 가 실패 코드 enum 인데 `ErrorReasonDTO` 를 `.isSuccess(true)` 로 고정 생성한다(`SuccessStatus` 와 대비). 현재 `ApiResponse.onFailure` 가 `isSuccess=false` 를 따로 지정하므로 실제 응답엔 영향 없으나, 오해를 부르는 죽은 설정값이다 — 의도 확인 후 정리 여부 결정.


### TODO Y — 나의 활동 커피챗 응답 카드 경량 DTO + `jobType` 노출 + `region` 필드 전면 제거

**배경**
- 프론트 '나의 활동 > 커피챗' 카드 UI 는 회원당 다음 7+1 필드만 표시한다: `nickname`("이선배"), `department`+`enrollmentYear`("컴퓨터공학과 '18"), `company`+`jobType`("네이버 • 백엔드 개발자"), `careerYear`("경력 3년"), `techStacks`(칩), 상세 진입용 `memberId`(선배 페이지 둘러보기 버튼).
- 그러나 `GET /api/v1/coffeechat/my-activity` 는 매칭 상세 화면용 `MatchedCandidateDetailResponse` 를 그대로 반환해 화면이 안 쓰는 필드(`portfolio`/`githubLink`/`linkedinLink`/`businessCardImage`/`jobPosts`/`jobCategories`/`grade`)를 통째로 내려 보낸다. 페이로드 비대화 + 의도 모호.
- 또한 화면에 필수인 **`jobType`(예: "백엔드 개발자")** 이 응답 어디에도 없다 — `MatchedCandidateResponse`/`MatchedCandidateDetailResponse` 둘 다 `Graduate.jobType` 을 노출하지 않는다(현재 `enrollmentYear`/`company`/`careerYear` 만 채움).
- `region` 은 `CandidateProfileAssembler` 가 항상 `null` 로 채우는 "소스 미확정" 더미 필드 — UI 가 지역을 표시하지 않기로 확정됐으므로 DTO 에서 **전면 제거**(키 자체 삭제) 한다.

**변경 (요지)**
- **신규 경량 DTO `MyActivityCoffeeChatResponse`** — `(memberId, nickname, department, enrollmentYear, company, jobType, careerYear, techStacks)` 8필드. `/my-activity` 전용. 매칭 카드/상세 DTO 와는 분리해 책임 명확화.
- `CoffeeChatMatchingService.getMyActivity` 리턴 타입 `List<MatchedCandidateDetailResponse>` → `List<MyActivityCoffeeChatResponse>`. `CoffeeChatMatchingServiceImpl.getMyActivity` 는 `assembleDetail` 대신 신규 조립 헬퍼 사용.
- `CandidateProfileAssembler` 에 `MyActivityCoffeeChatResponse assembleMyActivityCard(Long memberId)` 신설 — GRADUATE 가드 후 `Graduate.jobType` 포함해 8필드 채움. (현재 `getMyActivity` 로직상 receiver 는 항상 졸업생이지만 안전 가드는 명시한다.)
- **`region` 필드 전면 제거** — `MatchedCandidateResponse.region` · `MatchedCandidateDetailResponse.region` 필드와 `CandidateProfileAssembler.assembleCard`/`assembleDetail` 의 `null` 채움 자리 모두 삭제. 어차피 항상 null 이라 매칭 카드/상세 화면도 시각적 영향 없음.

**파일 (수정 6, 신규 1)**
- 신규: `src/main/java/mju/capstone/ddingconnect/domain/coffeechat/dto/response/MyActivityCoffeeChatResponse.java`
- 수정: `src/main/java/mju/capstone/ddingconnect/domain/coffeechat/dto/response/MatchedCandidateResponse.java` — `region` 필드 제거
- 수정: `src/main/java/mju/capstone/ddingconnect/domain/coffeechat/dto/response/MatchedCandidateDetailResponse.java` — `region` 필드 제거
- 수정: `src/main/java/mju/capstone/ddingconnect/domain/coffeechat/service/CandidateProfileAssembler.java` — `assembleMyActivityCard` 신설, 기존 두 메서드의 `region` 인자 제거
- 수정: `src/main/java/mju/capstone/ddingconnect/domain/coffeechat/service/CoffeeChatMatchingService.java` — `getMyActivity` 시그니처 갱신
- 수정: `src/main/java/mju/capstone/ddingconnect/domain/coffeechat/service/CoffeeChatMatchingServiceImpl.java` — `assembleMyActivityCard` 위임
- 수정: `src/main/java/mju/capstone/ddingconnect/domain/coffeechat/controller/CoffeeChatMatchingController.java` + `CoffeeChatMatchingSwagger.java` — 엔드포인트 리턴 타입 갱신

**테스트**
- 신규/갱신: `CoffeeChatMatchingServiceImplTest` — `getMyActivity` 가 새 DTO 로 반환되고, `jobType` 이 정상 채워지며, `region`/상세 전용 필드는 노출되지 않음 검증
- 갱신: `CoffeeChatMatchingControllerTest` — `my-activity` jsonPath `$.result[0].jobType` 노출 검증 + `region`/`portfolio`/`githubLink`/`linkedinLink`/`businessCardImage`/`jobPosts` 미노출 검증
- 갱신: 매칭 카드/상세 테스트 — `region` 필드 expectation 제거

**Breaking change**
- 프론트 응답 스키마 변경: `/my-activity` 응답이 8필드 슬림 카드로 축소 + `jobType` 신규 노출, `region` 제거. 매칭 카드/상세는 `region` 만 제거(항상 null 이라 화면 영향 없음).

**문서 갱신**
- `docs/agent/backend.md` 의 "후보 프로필 조립 (`CandidateProfileAssembler`)" 섹션 — `region` 관련 문장 삭제, `jobType` 노출 명시, `getMyActivity` 가 카드 경량 DTO 반환으로 변경됐음 추가
- `docs/agent/backend.md` 의 마이페이지/나의 활동 섹션 — `/my-activity` 응답 contract 변경 사실 명시


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

