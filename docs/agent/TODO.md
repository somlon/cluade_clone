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

> **결정 (2026-05-27 사용자 확정)**: 다운로드 파일 포맷은 **PDF**. (JSON 텍스트·기타 포맷은 채택하지 않음 — 재검토 불필요.)

- 라이브러리: `backend/build.gradle` 에 OpenPDF 의존성 추가 — `com.github.librepdf:openpdf:1.3.x` (LGPL/MPL, 상업 사용 자유). iText 7 는 AGPL 부담으로 미채택.
- 신규 컴포넌트: `RoadmapPdfRenderer` (`backend/src/main/java/mju/capstone/ddingconnect/domain/roadmap/service/RoadmapPdfRenderer.java`) — 메서드 `byte[] render(String content)` 가 `Roadmap.content`(JSON 문자열)를 PDF 바이트 배열로 변환.
- JSON 파싱: 데이터 파트 `RoadmapResponse` 스키마와 정합 — `roadmap_title` / `steps[].phase_badge` / `steps[].title` / `steps[].details[].category` / `steps[].details[].content` / `recommended_certifications` / `recommended_activities` / `summary_advice`. `ObjectMapper` 로 내부 record 매핑.
- 레이아웃: Figma 결과 화면 카드 형식을 PDF 페이지로 옮긴다 — (1) 표지 제목 = `roadmap_title`, (2) 3단계 step 카드 = `phase_badge` 칩 + `title` + `details` 항목 리스트, (3) 추천 자격증·활동 칩 영역, (4) 마지막 페이지에 `summary_advice` 한마디.
- **한글 폰트 (2026-05-27 사용자 확정 — A안 레포 vendoring)**: `backend/src/main/resources/fonts/NotoSansKR-Regular.ttf` 를 레포에 커밋 (1.5MB+). [Google Fonts > Noto Sans KR](https://fonts.google.com/noto/specimen/Noto+Sans+KR) 의 static Regular ttf 사용 (라이선스 **SIL OFL 1.1**, 상업 사용 자유, `OFL.txt` 동봉 선택). 코드는 `BaseFont.createFont("classpath:fonts/NotoSansKR-Regular.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED)` 로 등록 — 미설정 시 한글 깨짐. B안(빌드 시 다운로드)·C안(후속 PR 보강)은 미채택 — 외부 URL 의존 회피 + 1차 PR 안에서 동작 보장.
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



