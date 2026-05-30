# 진행 예정 작업 (To-do)

> 이 파일은 루트 `CLAUDE.md` 가 `@import` 하지 않는다(자주 바뀌어 자동 로드 제외). 사용자가 TODO 관련 명령을 내리면 이 파일을 직접 읽고 진행한다. 완료 후 항목은 이 파일에서 삭제하고 `docs/agent/backend.md` 의 해당 도메인 섹션에 정식 규칙으로 통합한다.

> 사용자가 "to-do 리스트 수행" / "to-do 진행" / "TODO N 작업" 등 유사 명령을 내리면 아래 항목을 **즉시 코드 작성 → 테스트 보강 → CLAUDE.md 갱신 → 커밋/푸시 → PR 생성까지 일사천리로** 수행한다. 결정 사항은 이미 확정돼 있으니 다시 묻지 말고 명시된 디폴트로 진행할 것.
>
> 각 항목은 완료(머지) 후 이 섹션에서 삭제하고, 본문 도메인 섹션에 정식 규칙으로 통합 기록한다.

> **관찰 (의도 확인 필요)**: `ErrorStatus.getReason()`/`getReasonHttpStatus()` 가 실패 코드 enum 인데 `ErrorReasonDTO` 를 `.isSuccess(true)` 로 고정 생성한다(`SuccessStatus` 와 대비). 현재 `ApiResponse.onFailure` 가 `isSuccess=false` 를 따로 지정하므로 실제 응답엔 영향 없으나, 오해를 부르는 죽은 설정값이다 — 의도 확인 후 정리 여부 결정.


### TODO AA — 회원가입 시 증명서 OCR 추출 정보(이름·학과·학년) 자동 저장

**배경**
- 데이터 파트(`ddingconnect-data`)가 PR #13(`main` 반영, 2026-05-30)으로 OCR 증명서에서 **이름·학과·학년**까지 파싱하도록 확장됐다. `POST /api/data/verify` 응답이 다음 형태로 바뀌었다(읽기 확인 완료):
  ```jsonc
  { "status": "success", "user_id": 1, "is_approved": true,
    "message": "...",
    "student_info": {
      "type": "재학생" | "졸업생",   // 재학증명서/졸업증명서 종류
      "name": "홍길동",
      "department": "데이터사이언스전공",
      "grade": "4"                  // 재학생일 때만 포함(문자열). 졸업생은 키 없음
    } }
  // 미승인: { "status":"fail", "is_approved":false, "details":{ is_myongji, is_certificate } }
  ```
- 그러나 백엔드 회원가입(`AuthServiceImpl.signup`)은 **OCR 을 호출하지 않고** 증명서를 S3 에 올려 `Member.certificate`(URL)만 저장한다. `Member.name`·`Member.department` 는 비고, `Student.grade` 도 비어 있다(`SignupRequest`·`AuthServiceImpl` 에 `//TODO 재학증명서` 주석만 존재). 마이페이지·커피챗 카드·로드맵 등에서 이름/학과/학년이 빠져 보이는 근본 원인.
- 목표: 회원가입 시 데이터 파트 OCR 추출값을 받아 **`Member.name`·`Member.department`(공통) + `Student.grade`(재학생만)** 에 저장한다. 졸업생은 학년 개념이 없으므로 `Graduate` 가 아닌 **멤버 공통 2필드만** 채운다.

**설계 제약 (먼저 확정 필요 — 구현 전 사용자 확인)**
- ⚠️ **`/api/data/verify` 는 `X-User-Id` 헤더(= 이미 존재하는 회원) 인증을 요구**한다(`get_current_user`). 즉 "회원가입 *도중*" 호출하려면 **member 를 먼저 저장해 PK 를 얻은 뒤** 그 id 로 `X-User-Id` 를 붙여 호출해야 한다. 단일 `signup` 트랜잭션 안에서 (1) member/role 레코드 저장 → (2) 외부 OCR 호출 → (3) 추출값으로 member/student 갱신 순서가 된다(로드맵 `create` 가 트랜잭션 안에서 외부 HTTP 를 호출하는 선례와 동일).
- ⚠️ **`/verify` 는 PDF 멀티파트(`file`) 를 받는다.** 백엔드 `signup` 도 이미 `certificate` 멀티파트를 받으므로 같은 파일 바이트를 데이터 파트로 전달(멀티파트 relay)해야 한다. (현재 `signup` 은 그 파일을 S3 업로드에만 쓰고 OCR 엔 안 보냄.)
- ⚠️ **`/verify` rate limit = 5/day(IP 기준)**. 백엔드가 단일 IP 로 호출하면 전체 가입자가 한도를 공유 → 가입 실패 위험. 운영 영향 큼.
- ⚠️ **데이터 파트 의존성 추가**: 회원가입이 데이터 파트(8000) 가용성에 묶인다. 데이터 파트 다운 시 가입 전체가 막히면 안 되므로 **실패 정책(승인 거부 vs name/department 만 비우고 가입 허용)** 을 정해야 한다.

**결정 (디폴트 — 사용자가 바꾸지 않으면 이대로)**
- 신규 클라이언트 `CertificateOcrClient`(`global/auth` 또는 `global` 하위) — 커피챗 `MatchingAlgorithmClient`/로드맵 `RoadmapAiClient` 와 동일한 얇은 `RestClient` 패턴. base-url 은 기존 `data.base-url`(env `DATA_BASE_URL`, 기본 `http://localhost:8000`) 재사용. `POST /api/data/verify` 멀티파트 전송 + `X-User-Id` 헤더(저장된 member.id).
- 응답 DTO(record): `CertificateVerifyResponse(status, isApproved, StudentInfo studentInfo)` / `StudentInfo(type, name, department, grade)`. snake_case 매핑(`@JsonProperty("student_info")` 등). `grade` 는 문자열로 받아 `Integer.parseInt` 변환(파싱 실패 시 null).
- `AuthServiceImpl.signup` 흐름: role 검증 → 이메일 중복 → **member + role 레코드 저장(기존)** → 증명서 S3 업로드(기존) → **OCR `verify` 호출** → `is_approved=true` 면 `Member.name`/`department` 갱신, role=STUDENT 면 `Student.grade` 도 갱신(`MemberServiceImpl.sanitizeGrade` 와 동일한 1~4 클램프/검증 재사용). role=GRADUATE 면 학년 무시.
- **실패 정책(디폴트)**: OCR 호출 실패·`is_approved=false` 여도 **가입 자체는 성공** 처리하고 name/department/grade 만 비워 둔다(추후 마이페이지 수정 가능). 즉 OCR 은 best-effort 자동 채움이며 가입의 차단 조건이 아니다. (증명서 미승인 시 가입을 막아야 한다면 이 결정을 뒤집어야 하므로 **사용자 확인 항목**.)
- 졸업생은 `Graduate` 에 학년 필드가 없으므로 멤버 공통(name/department)만 채운다.

**변경 (요지)**
- 신규: `CertificateOcrClient`(인터페이스) + `CertificateOcrClientImpl`(RestClient, 멀티파트+`X-User-Id`) + 응답 record DTO.
- 수정: `AuthServiceImpl.signup` — member 저장 후 OCR 호출·추출값 반영. `Student.grade` 갱신을 위해 `studentRepository` 재조회/저장 또는 `createRoleRecord` 시점에 grade 주입하도록 순서 조정.
- 수정(가능): `Student` 생성 시 grade 를 받도록 `createRoleRecord` 시그니처 조정. `Member.name`/`department` 는 빌더에 추가(엔티티 컬럼은 이미 존재 — 스키마 변경 없음).
- (선택) `application.yml`/`data.yml` 에 OCR 호출용 타임아웃 등 설정 — 기존 `data.base-url` 재사용이라 신규 설정 최소.

**파일 (신규 ~3, 수정 ~2)**
- 신규: `global/auth/service/CertificateOcrClient.java` · `CertificateOcrClientImpl.java`
- 신규: `global/auth/dto/response/CertificateVerifyResponse.java`(+ 내부 `StudentInfo`)
- 수정: `global/auth/service/AuthServiceImpl.java` — OCR 호출·추출값 저장 로직
- 수정(가능): `AuthServiceImpl.createRoleRecord` 또는 `Student` 저장부 — grade 주입

**테스트**
- `AuthServiceImplTest`(신규/확장) — OCR mock(`CertificateOcrClient` stub)으로: (1) 재학생 가입 시 name/department/grade 저장, (2) 졸업생 가입 시 name/department 만 저장(grade 미적용), (3) OCR 실패/미승인 시 가입은 성공하고 name/department/grade 는 비어 있음(실패 정책), (4) grade 문자열 파싱·1~4 클램프.
- `CertificateOcrClientImplTest` — `MockRestServiceServer` 로 `/api/data/verify` 멀티파트 요청·`X-User-Id` 헤더·응답 파싱(`student_info` snake_case, 재학생 grade 유무) 검증. (매칭/로드맵 클라이언트 테스트 선례 동일.)
- 하드코딩 금지 — 경로(`/api/data/verify`)·헤더명(`X-User-Id`)·grade 범위는 상수/기존 심볼 참조.

**문서 갱신 (구현 PR 에서 동반)**
- `docs/agent/backend.md` `### 회원 (Member)` 또는 `### 인증/JWT` — 회원가입이 OCR 추출값(name/department/grade)을 자동 저장하는 플로우, 데이터 파트 의존·실패 정책 명시.
- `docs/agent/data.md` `### POST /api/data/verify` — 현재 문서의 응답 스키마(구: `extracted_name`/`raw_text`)를 PR #13 의 신 스키마(`student_info{type,name,department,grade}`)로 갱신. 백엔드 연동 계약(멀티파트 relay + `X-User-Id` + rate limit 한계) 추가.
- 완료(머지) 후 본 TODO 항목은 이 파일에서 삭제.

**Breaking change / 주의**
- 회원가입이 **데이터 파트(8000) 가용성에 의존**하게 된다(실패 정책으로 가입 차단은 회피하되, 데이터 파트 다운 시 자동 채움 미동작). `verify` 5/day rate limit 이 가입 흐름에 영향을 줄 수 있어 **운영 전 limit 정책 재협의 필요**.
- 데이터 파트 `/verify` 가 `X-User-Id` 인증·멀티파트라 "가입 전" 호출이 불가 → member 선저장 후 호출하는 순서가 강제됨(위 설계 제약).


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

