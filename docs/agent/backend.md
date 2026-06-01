# ddingconnect 백엔드

명지대 캡스톤 — 졸업생/재학생 커뮤니티 플랫폼 백엔드(`somlon/cluade_clone`).
원본: `ddingconnect-backend` PR #15 스냅샷 (Merge PR #1, 2026-05-11).

> 루트 `CLAUDE.md` 가 `@import` 로 불러오는 백엔드 상세 문서. 백엔드 구조·도메인 규칙·패턴이 바뀌면 이 파일을 갱신한다 (루트 `CLAUDE.md` 의 '문서 자동 유지관리' 규칙 참조).

> **디렉터리 위치**: 백엔드 프로젝트(코드·빌드 파일)는 레포 루트가 아니라 `backend/` 하위에 있다 — `backend/src/`, `backend/build.gradle`, `backend/gradlew`, `backend/gradle/`, `backend/settings.gradle`. 빌드·실행은 `cd backend` 후 진행한다 (예: `cd backend && ./gradlew build`). 이 문서에서 `src/`·`build.gradle`·`gradle/` 등으로 표기한 경로는 모두 `backend/` 기준이다. 루트에는 `CLAUDE.md`·`docs/`·`.github/`·`.gitignore` 만 남는다.

## 기술 스택

- Spring Boot 3.5.13 / Java 17 (toolchain)
- Spring Data JPA + MySQL (`mysql-connector-j`)
- Spring Security + JWT (`jjwt 0.12.6`, HS512)
- Swagger (`springdoc-openapi-starter-webmvc-ui 2.7.0`)
- AWS S3 (`software.amazon.awssdk:s3 2.25.61` — `S3Client` + `S3Presigner` 포함)
- PDF 렌더 (`com.github.librepdf:openpdf 1.3.34` — LGPL/MPL, iText 7 AGPL 미채택; 로드맵 다운로드용)
- Lombok, Validation, `spring-dotenv` (`.env`)
- 테스트: JUnit 5, `spring-security-test`

## 빌드 특이사항

- `build.gradle` — build 디렉터리는 `-PbuildDir=...` 또는 `gradle.properties` 의 `buildDir` 로 외부화한다. 미지정 시 Gradle 기본값(`build/`)을 사용하므로 리눅스/맥/CI 에서 그대로 빌드된다. OneDrive 동기화 잠금 회피가 필요한 Windows 환경만 `buildDir` 를 외부 경로(예: `C:/gradle-builds/...`)로 지정한다.
- Spring profiles: `db, s3, oauth, jwt` (기본 활성)
- 설정 분리: `application.yml`, `application-db.yml`, `application-jwt.yml`

## 패키지 구조

```
mju.capstone.ddingconnect
├── DdingconnectApplication.java
├── domain/
│   ├── coffeechat/      # 커피챗 매칭
│   ├── interested_job/  # TargetJob (관심 직군)
│   ├── job_post/        # PostContents + GraduateJobPost
│   ├── member/          # Member + Student/Graduate
│   ├── qna/answer/
│   ├── qna/question/
│   ├── roadmap/
│   └── techstack/
└── global/
    ├── alarm/           # 통합 알람 조회(4종 목록/상세/읽음) + 상대시간 포맷
    ├── auth/            # 회원가입/로그인, JWT 필터, @LoginMember
    ├── aws/             # S3 업로드/삭제·presigned URL (S3Service.createUploadPresign 발급, UploadType 용도별 화이트리스트)
    ├── common/          # BaseEntity (createdAt/updatedAt), SuccessMessage (성공 메시지 상수)
    ├── config/          # Security, Swagger, JpaAuditing, WebMvc, Mail, Sse
    ├── jwt/             # JwtUtil(Impl)
    ├── redis/           # RedisUtil (StringRedisTemplate 래퍼)
    ├── response/        # ApiResponse, ErrorStatus, ExceptionAdvice
    └── sse/             # SSE 실시간 푸시 + 알람 이벤트 리스너
```

각 도메인은 `controller / service / domain(entity+repository) / dto` 4계층으로 일관됨.

## 핵심 도메인 규칙

### 회원 (Member)
- `MemberRole`: `UNKNOWN | STUDENT | GRADUATE`
- 회원가입 시 role에 따라 `Student` 또는 `Graduate` 레코드를 함께 생성 (`AuthServiceImpl.createRoleRecord`)
- **`Member.point` 기본값 = 0L (가입 시 자동 초기화)**: `Member` 엔티티가 `@Builder.Default private Long point = 0L` 로 정의되어, `Member.builder()...build()` 호출 시 `point` 를 명시하지 않으면 0 으로 초기화된다(`AuthServiceImpl.signup` 의 회원가입 경로 포함). 도메인 invariant 로 `point` 는 절대 null 이 아니다 — 홈 응답 우측 상단 P 뱃지가 신규 가입 직후에도 비지 않도록 보장. 운영 가입자 데이터에 이미 `point: null` row 가 있다면 별도 마이그레이션 백필이 필요하지만 현재(개발 단계)는 적용 대상 없음.
- **탈퇴 = hard delete (`MemberServiceImpl.withdraw`)**: 회원이 작성/소유/수신한 모든 자식 행을 단일 `@Transactional` 안에서 정리한 뒤 `Member` row 자체를 삭제. 위임 순서 = (1) CoffeeChat 양방향(requester/receiver) 모두 정리(서비스 우회, 알람도 함께 삭제) → (2) 본인 작성 부모 row 들을 각 도메인 `delete()` 로 캐스케이드 위임 (Question/Answer/Roadmap, GRADUATE 면 JobPost) → (3) 본인 수신 leaf 알람·좋아요 `deleteByMemberId` (AnswerAlarm/JobAlarm/RoadmapAlarm/CoffeeChatAlarm/QuestionLike/AnswerLike) → (4) TechStack/TargetJob 본인 row → (5) Student/Graduate 매핑 → (6) Member. `isDeleted` 컬럼은 스키마에 남기되 기본 `false` 고정 (다른 용도 보존). `JwtAuthenticationFilter` 가 `findById` 실패 시 자동으로 인증 실패 처리하므로 토큰 무효화 별도 작업 불필요.
- **소셜 링크 검증 (`UpdateMemberRequest`)**: `githubLink` 는 `^https?://(www\.)?github\.com/.+`, `linkedinLink` 는 `^https?://(www\.)?linkedin\.com/.+` 패턴 매칭 (Bean Validation `@Pattern`). `null` 통과 (미입력 = 변경 없음), 빈 문자열·도메인 불일치는 400. 컨트롤러는 `@Valid` 필수. 검증 실패 시 `MethodArgumentNotValidException` 핸들러에서 `_BAD_REQUEST` 로 매핑.
- **`Student.grade` 검증 (`MemberServiceImpl.sanitizeGrade`)**: `grade < 1` → 400 (`MEMBER_INVALID_GRADE`). `grade > 4` → 4 로 클램프 (`Math.min(raw, 4)`)해서 저장. `null` 은 기존값 유지.
- **역할 외 필드 거부 (`MemberServiceImpl.validateRoleFields`)**: 단일 `UpdateMemberRequest` 에 STUDENT/GRADUATE 양쪽 필드가 다 있어, 본인 역할 외 필드를 non-null 로 보내면 즉시 400 (`MEMBER_FIELD_ROLE_MISMATCH`). STUDENT → graduate 필드 거부, GRADUATE → grade 거부, UNKNOWN → 양쪽 다 거부. 공통 필드만 보내는 건 항상 통과.
- **이름·이메일 수정 + 졸업생 직무**: 기본 정보의 '이름'(`Member.name`)·'이메일'(`Member.email`)은 STUDENT/GRADUATE 공통 수정 항목 — `UpdateMemberRequest`·`MemberResponse` 에 포함. `email` 변경은 `@mju.ac.kr` 패턴(`ValidationPattern.MJU_EMAIL_REGEX`) 검증 + 본인 제외 중복 검사(`DUPLICATE_EMAIL`)를 거친다(변경 시 재인증 요구는 아직 없음 — 가입 이메일 인증 방식 확정 후 별도 도입 예정). 졸업생 '직무'(`Graduate.jobType`)는 자유 텍스트가 아니라 구직 공고 직무 필터용 `JobType` enum 을 재사용한다.
- **프로필 사진·명함·포트폴리오 업로드 = 용도별 presigned PUT URL 발급 (2-step)**: 사용자 파일 업로드(프로필 사진·명함·포트폴리오)는 백엔드가 파일 바이트를 받지 않고 **용도별 presigned PUT URL 만 발급**한다. 프론트는 발급받은 `uploadUrl` 로 S3 에 직접 PUT 한 뒤, 받은 `fileUrl`(최종 public URL) 을 string URL 필드에 저장하는 별도 PATCH 를 호출한다. 용도별 발급 엔드포인트 3개(모두 `MemberController`, `@LoginMember` 인증 필수, 요청 `PresignedUploadRequest(fileName, contentType)` → 응답 `ApiResponse<PresignedUploadResponse(uploadUrl, fileUrl, key, expiresAt)>`):
  - **프로필 사진 (`POST /api/v1/members/me/profile-image/presigned-url`)** — `MemberServiceImpl.createProfileImageUploadUrl`. content-type 화이트리스트 `UploadType.IMAGE`(`image/png`·`image/jpeg`·`image/webp`). 발급받은 `fileUrl` 은 `PATCH /me` 의 `profileImage`(String) 에 저장.
  - **명함 사진 (`POST /api/v1/members/me/business-card/presigned-url`, GRADUATE 전용)** — `MemberServiceImpl.createBusinessCardUploadUrl`. `member.getRole() == GRADUATE` 진입 가드(STUDENT/UNKNOWN 모두 `MEMBER_FIELD_ROLE_MISMATCH`) 후 `UploadType.IMAGE` 화이트리스트로 발급. `fileUrl` 은 `PATCH /mypage/graduate` 의 `businessCardImage` 에 저장.
  - **포트폴리오 (`POST /api/v1/members/me/portfolio/presigned-url`)** — `MemberServiceImpl.createPortfolioUploadUrl`. content-type 화이트리스트 `UploadType.PORTFOLIO`(`application/pdf`). `fileUrl` 은 `PATCH /me` 의 `portfolio` 에 저장.
  - **발급 로직 = `S3Service.createUploadPresign(fileName, contentType, allowedContentTypes)`**: content-type 화이트리스트 검증(불일치 `_FILE_TYPE_NOT_ALLOWED`) → `convertToSaveName` 키 생성 → `generateUploadPresignedUrl` 발급 → `getUrl(key)` 최종 URL + `expiresAt(now+ttl)` 담은 `PresignedUploadResponse` 반환. `generateUploadPresignedUrl(key, contentType, ttl)` 은 AWS SDK v2 `S3Presigner.presignPutObject`(`PutObjectPresignRequest`/`PutObjectRequest(bucket,key,contentType)`) 사용 — content-type 이 presign 에 서명되므로 실제 PUT 요청 `Content-Type` 헤더가 동일해야 S3 가 수락. 발급 자체 실패는 `_FILE_PRESIGN_FAILED`(S3500).
  - **용도별 content-type 화이트리스트 = `global/aws/UploadType`**: `IMAGE`={image/png, image/jpeg, image/webp} / `PORTFOLIO`={application/pdf} 를 **단일 정의로 보유**하고 위 3개 발급 메서드가 참조(중복·하드코딩 제거). 업로드 용도는 enum 파라미터가 아니라 **엔드포인트 경로로 구분**하므로 `PresignedUploadRequest` 에는 `uploadType` 필드가 없다.
  - **2-step 계약**: ① `POST /me/{용도}/presigned-url {fileName, contentType}` → `{uploadUrl, fileUrl, key, expiresAt}` ② 받은 `uploadUrl` 로 파일 바이트 **PUT**(헤더 `Content-Type` = 1번 contentType) ③ 업로드 성공 후 `fileUrl` 을 해당 string 필드(profileImage·businessCardImage·portfolio)에 `PATCH` 로 저장.
  - **삭제 엔드포인트 (DELETE, 자산별 1개씩)**: 업로드된 자산을 다시 비우는 동작은 부분 수정 규약(`null` = 미변경)으로 처리할 수 없어 전용 DELETE 엔드포인트로 노출한다(빈 문자열 우회·"clear" 플래그 모두 미채택). 응답은 `ApiResponse<MemberResponse>` 로 갱신된 프로필을 반환해 프론트의 후속 GET 호출을 제거한다 — `PATCH /me` 와 동일 DTO. 세 엔드포인트 모두 `MemberServiceImpl` 에서 단일 `@Transactional` + `s3Service.deleteImage(url)` best-effort(`safeDeleteFromS3` 헬퍼: try/catch + `log.warn`, 실패해도 트랜잭션 영향 없음, dangling S3 객체보다 사용자 자산 DB 클리어 우선 — `RoadmapServiceImpl.delete` 동일 패턴) → 빌더 reconstruction 으로 해당 string 필드만 `null` 로 교체해 저장. **이미 비어 있으면 no-op** 으로 현재 프로필을 그대로 반환(idempotent, 200) — S3·DB 호출 없음.
    - **프로필 사진 (`DELETE /api/v1/members/me/profile-image`)** — `MemberServiceImpl.deleteProfileImage`. `Member.profileImage` 만 `null` 로 클리어, 다른 Member 필드는 보존.
    - **명함 사진 (`DELETE /api/v1/members/me/business-card`, GRADUATE 전용)** — `MemberServiceImpl.deleteBusinessCard`. 진입부에서 `member.getRole() != GRADUATE` 면 `MEMBER_FIELD_ROLE_MISMATCH`(STUDENT/UNKNOWN 모두 거부, presigned 발급 가드와 동일). `Graduate.businessCardImage` 만 `null` 로 클리어, `jobType`·`company`·`careerYear` 는 보존. `Graduate` row 가 없으면 `MEMBER_NOT_FOUND`.
    - **포트폴리오 (`DELETE /api/v1/members/me/portfolio`)** — `MemberServiceImpl.deletePortfolio`. `Member.portfolio` 만 `null` 로 클리어.
  - **설정**: `aws.s3.upload-presign-ttl`(ISO-8601 `Duration`, 기본 `PT5M`) 을 `application-s3.yml` 에 외부화(`download-presign-ttl` 선례와 동일), `S3Service` 에 `@Value` 주입. URL 유출 시 노출 시간 최소화 목적.
  - **S3 버킷 CORS (인프라 선행 필수)**: 브라우저 직접 PUT 을 위해 public 버킷 CORS 허용 필요 — `AllowedMethods:[PUT]`, `AllowedOrigins:[http://localhost:5173, <운영 도메인>]`, `AllowedHeaders:["*"]`, `ExposeHeaders:[ETag]`. 미설정 시 브라우저 PUT 이 CORS 로 차단(코드 외 작업).
  - **크기 강제 한계**: presigned PUT 은 서버측 크기 강제가 불가 — 크기 제한은 프론트 클라이언트단 검증에 맡긴다(엄격 강제 필요 시 presigned POST `content-length-range` 또는 업로드 후 HEAD 검증으로 후속 보강, 범위 밖). content-type 화이트리스트 + presign content-type 핀까지만 강제. 멀티파트 업로드가 하던 기존 파일 S3 cleanup(교체 시 old 삭제)은 발급 시점에 불가하므로 string URL 저장 PATCH 단계의 후속 과제로 둔다.
  - **이력 (대체·제거)**: 과거 멀티파트 업로드(`PATCH /me/profile-image`·`/me/business-card`·`/me/portfolio` + `MemberServiceImpl.updateProfileImage`/`updateBusinessCard`/`updatePortfolio`, `S3Service.uploadFile`)와 범용 단일 발급 엔드포인트(`POST /api/v1/files/presigned-url` + `FileController`/`FileSwagger`, `PresignedUploadRequest.uploadType`)는 본 용도별 presigned 발급으로 통합·대체되어 제거됨. `S3Service.uploadImage` 는 회원가입 증명서 흐름(`AuthServiceImpl.signup`)이 계속 사용하므로 유지.
- **`S3Service` 서버 생성 파일 + presigned URL 확장 (로드맵 PDF 다운로드용)**: `MultipartFile` 외 서버가 직접 만든 바이트 배열 업로드용으로 `uploadBytes(byte[], key, contentType)` 추가, GET 다운로드 URL 발급용으로 `generatePresignedUrl(key, Duration ttl, fileName)` 추가. 후자는 응답 헤더 오버라이드(`response-content-disposition: attachment; filename="..."`) 를 함께 지정해 브라우저 다운로드를 강제한다. presigner 빈은 `S3Config.s3Presigner()` 가 `S3Client` 와 동일 region/credentials 로 생성(`software.amazon.awssdk:s3` 에 `S3Presigner` 포함 — 별도 `s3-presigner` artifact 추가 없음). 업로드 PUT 발급(`createUploadPresign`/`generateUploadPresignedUrl`)·GET 다운로드 발급(`generatePresignedUrl`)·`uploadImage`·`uploadBytes`·`deleteImage` 시그니처 유지 — 로드맵 다운로드·증명서 흐름 영향 없음.

### 홈 화면 (member)
- **`GET /api/v1/members/me/home`** — 홈 화면을 1회 호출로 렌더링하기 위한 통합 조회. 컨트롤러는 `MemberController`, 서비스는 `HomeService(Impl)`.
- **응답 `HomeResponse`** = `point`(보유 P) + `nickname` + 학과 자리(STUDENT `department` / GRADUATE `company`) + `role` + 학년 자리(STUDENT `grade` / GRADUATE `careerYear`) + `activity(coffeeChatCount, roadmapCount, questionCount)`. **STUDENT 와 GRADUATE 가 같은 UI 슬롯(학과·학년 자리)을 서로 다른 키로 채운다** — 평면 필드 `department`/`company`/`grade`/`careerYear` 4개를 두고, 비해당 역할 필드는 모두 null 로 채워 `@JsonInclude(NON_NULL)` 가 응답에서 키 자체를 제외한다. 결과적으로 STUDENT 응답엔 `{department, grade}` 만, GRADUATE 응답엔 `{company, careerYear}` 만 노출. `activity.roadmapCount` 도 GRADUATE 응답에선 키 자체 제외.
- **활동 카운트는 마이페이지와 동일 집계 기준 재사용** — `HomeServiceImpl` 은 `CoffeeChatService.countMyAcceptedCoffeeChats` / `RoadmapService.countMyRoadmaps`(STUDENT 만 호출, GRADUATE/UNKNOWN 은 null) / `QuestionService.countMyQuestions` 를 그대로 호출한다. 새 집계 로직을 만들지 않고 `MyPageServiceImpl.buildResponse` 와 같은 메서드를 공유 (집계 기준 단일화).
- **애그리게이터 패턴** (`MyPageServiceImpl` 과 동일) — `HomeServiceImpl` 은 항목별 도메인 서비스(`CoffeeChatService`/`RoadmapService`/`QuestionService`) + 역할별 `StudentRepository.findByMemberId`/`GraduateRepository.findByMemberId` 를 in-process 로 조합. 별도 엔티티/집계 컬럼 없음.
- **`Member.point` 노출 정책 분리**: 홈 응답은 `point` 를 항상 노출(우측 상단 P 뱃지용), 마이페이지 응답은 `MemberResponse.withoutPoint()` 로 항상 제외(기존 정책). 두 응답이 서로 영향 없음.
- **GRADUATE 의 학과·학년 자리 = 회사·연차 평면 필드**: STUDENT 가 `{department, grade}` 평면 필드로 표현되는 학과·학년 슬롯에, GRADUATE 는 같은 슬롯을 `{company, careerYear}` 평면 필드로 채운다(프론트가 키 존재 여부로 렌더, role 분기 불필요). 직무(`jobType`)·명함(`businessCardImage`) 같은 졸업생 부가 정보는 홈 응답에 노출하지 않으며 필요하면 마이페이지에서 조회한다.
- **추가 포인트/충전 엔드포인트 — 홈 모달·충전 페이지용**:
  - `GET /api/v1/members/me/point` — 보유 P만 반환(`PointBalanceResponse(point)`). 홈 화면 포인트 모달이 열릴 때마다 호출해 최신 P 갱신 용도. 컨트롤러에서 `new PointBalanceResponse(member.getPoint())` 로 직접 응답(서비스 없음).
  - `GET /api/v1/members/me/point/products` — 보유 P + 충전 상품 카드 리스트(`PointChargeResponse(point, products: [{id, points, price, recommended}])`). 컨트롤러에서 `PointChargeResponse.of(member.getPoint())` 로 직접 응답(서비스 없음). 상품 매핑은 `PointChargeResponse.of` 정적 팩토리가 `PointProduct.values()` 를 변환.
- **포인트 상품 정적 관리 (`PointProduct` enum)**: 충전 상품 6건(10/30/50추천/100/300/500 P)을 enum 상수로 보유 (id/points/price/recommended). 운영 중 가격·추천 뱃지 위치 변경 시 enum 만 수정. 별도 `PointProduct` 테이블/엔티티 없음(현 정책 — 변경 빈도 낮음 가정).
- **단위 통일 = P**: 응답 필드는 `point`/`points` (Long/Integer) 로 통일. 프론트의 "토큰" 라벨은 동일 값을 다른 표기로만 렌더링 (1 P = 1 토큰). 백엔드 단에서 토큰 단위 별도 보유하지 않는다.
- **(미구현) 결제·충전 처리·포인트 이력**: 충전 상품을 선택해 실제 결제하는 API, 결제 PG 연동, `PointHistory` (충전/사용/환불 추적) 엔티티는 아직 없다. 현재는 `Member.point` 단일 컬럼만 증감 가능 — 누가 언제 얼마를 어떻게 변동했는지 추적 불가. 결제 흐름·이력 도입은 별도 작업으로 분리.

### 마이페이지 (member)
- **`GET /api/v1/members/mypage`** — 마이페이지 화면을 1회 호출로 렌더링하기 위한 통합 조회. 컨트롤러는 `MemberController`, 서비스는 `MyPageService(Impl)`.
- **나의 활동 페이지 (`MyActivityController` + 도메인별 분산)**: 마이페이지 활동 통계 카드를 클릭해 진입하는 '나의 활동' 페이지는 도메인별 본인 스코프 목록을 별도 엔드포인트로 노출한다. 도메인별 배치:
  - **로드맵** — `GET /api/v1/roadmaps` 재사용(이미 본인 글 최신순 반환, 백엔드 변경 0). 프론트가 같은 엔드포인트를 호출.
  - **Q&A** — `GET /api/v1/questions/me` (`QuestionController.getMyQuestions` → `QuestionService.getMyQuestions`). 내부적으로 `questionRepository.findByMemberId(memberId)` 로 본인 글만 조회하고 기존 `toResponse(q, member)` 로 좋아요/답변 카운트·`likedByMe` 까지 일관 매핑. 전체 목록(`GET /api/v1/questions`, `getList(member)`)은 절대 수정하지 않는다 — Q&A 게시판은 전체 질문을 보여줘야 하므로 별도 엔드포인트로 분리.
  - **커피챗** — `GET /api/v1/members/me/activity/coffeechats` (`MyActivityController.getMyCoffeeChats` → `CoffeeChatService.getMyActivities`). 응답 DTO 가 상대방(파트너) 정보 위주(`CoffeeChatPartnerResponse`)라 기존 `CoffeeChatController` 의 본인 시점 응답(`CoffeeChatResponse`)과 분리하기 위해 별도 컨트롤러 신설. 신규 컨트롤러는 base `/api/v1/members` 로 회원 도메인 패키지에 둔다(나의 활동 = 마이페이지 후속 화면).
  - **Swagger 그룹**: "로드맵"/"질문" 도메인 그룹 + "나의 활동" 보조 그룹에 동시 표시 (`RoadmapSwagger.getRoadmaps`·`QuestionSwagger.getMyQuestions` 에 method-level `@Tag(name = "나의 활동")` 부여 — `MyActivitySwagger` 는 클래스 레벨 `@Tag` 가 이미 있어 그대로). '나의 활동' 화면 기준으로 한 그룹에서 3개 엔드포인트 통합 테스트 가능. 원본 도메인 그룹에서도 그대로 표시 (springdoc-openapi 의도된 중복).
- **애그리게이터 패턴**: `MyPageServiceImpl` 은 레포지토리를 직접 주입하지 않고, 항목별 도메인 서비스(`MemberService`/`TechStackService`/`TargetJobService`/`CoffeeChatService`/`RoadmapService`/`QuestionService`/`JobPostService`)를 in-process 로 호출해 한 응답으로 조합한다. 모놀리식 내 HTTP 자기호출이 아니라 서비스 계층 직접 호출.
- 응답 `MyPageResponse` = `profile`(`MemberResponse` 재사용) + `activity`(활동 통계) + `techStacks` + `targetJobs`(재학생 항목) + `jobPosts`(졸업생 항목).
- **활동 통계 `ActivityStats`**: 커피챗 수 = 본인이 요청자/수신자로 참여한 `ACCEPTED` 상태 합산(`CoffeeChatService.countMyAcceptedCoffeeChats`), 로드맵 수 = 본인 생성(`RoadmapService.countMyRoadmaps`), 질문 수 = 본인 작성(`QuestionService.countMyQuestions`).
- **`roadmapCount` 는 STUDENT 전용** (`MyPageServiceImpl.buildResponse`): 졸업생 마이페이지 카드에는 로드맵 항목 자체가 없으므로 `member.getRole() == MemberRole.STUDENT` 일 때만 `roadmapService.countMyRoadmaps(member)` 를 호출하고, GRADUATE/UNKNOWN 은 호출 없이 `null` 고정. `ActivityStats.roadmapCount` 의 타입은 `Long`(`@JsonInclude(NON_NULL)` 와 결합해 GRADUATE/UNKNOWN 응답에선 키 자체 제외).
- **응답 contract — 역할별 비해당 필드는 키 자체 제외** (TODO S): `MyPageResponse`·`MyPageResponse.ActivityStats`·`MemberResponse` 셋 모두 `@JsonInclude(NON_NULL)`. `targetJobs` 는 STUDENT 전용, `jobPosts` 는 GRADUATE 전용. `activity.roadmapCount` 는 STUDENT 전용 (GRADUATE 는 응답에서 제외). `MemberResponse` 의 `grade` (STUDENT 전용) / `businessCardImage`·`jobType`·`company`·`careerYear` (GRADUATE 전용) 도 동일 패턴 — 정적 팩토리 `from(Member, Student/Graduate)` 가 비해당 역할 필드를 `null` 로 채우므로 어노테이션만으로 자동 동작. **`point` 는 마이페이지에선 항상 제외** (`MemberResponse.withoutPoint()` 헬퍼로 명시 null 처리, 향후 값이 채워져도 mypage 응답엔 미포함). 다른 엔드포인트(`GET /me` 등)는 정적 팩토리 결과를 그대로 사용해 `point` 가 null 이면 제외, 값 있으면 포함(정책 분리). **빈 배열 `[]` 는 "역할 맞지만 0개" 의미로 보존** — 프론트의 "공고 없음" 같은 빈 상태 UI 분기에 사용.
- 마이페이지용 항목별 조회 메서드(`countMy*`, `getMyJobPosts`)는 각 도메인 서비스에 추가돼 향후 단건 화면에서도 재사용 가능. 별도 공개 엔드포인트는 아직 두지 않고 마이페이지 1개만 노출.
- **역할별 수정 엔드포인트 2개** — 마이페이지 편집은 역할별로 분리돼 있다. 컨트롤러는 `MemberController`, 서비스는 `MyPageService.updateStudentMyPage` / `updateGraduateMyPage`.
  - **`PATCH /api/v1/members/mypage/student`** — STUDENT 전용. 프로필(공통 9 + `grade`) + `techStacks` + `targetJobs` 일괄 수정.
  - **`PATCH /api/v1/members/mypage/graduate`** — GRADUATE 전용. 프로필(공통 9 + `businessCardImage`·`jobType`·`company`·`careerYear`) + `techStacks` + `jobPostsToAdd`(링크 전용) + `jobPostIdsToDelete` 일괄 수정.
  - 진입부 가드 — 컨트롤러는 `@LoginMember` 만 받고, 역할 검증은 서비스 진입부(`MyPageServiceImpl.update*MyPage`)에서 수행. `member.getRole()` 이 해당 역할이 아니면 즉시 `MEMBER_FIELD_ROLE_MISMATCH`(UNKNOWN 도 거부).
- **역할별 DTO 4개**: `UpdateStudentMyPageRequest(@Valid UpdateStudentProfileRequest profile, List<TechStackName> techStacks, List<TargetJobCategory> targetJobs)`, `UpdateGraduateMyPageRequest(@Valid UpdateGraduateProfileRequest profile, List<TechStackName> techStacks, @Valid List<CreateJobPostLinkRequest> jobPostsToAdd, List<Long> jobPostIdsToDelete)`. 프로필은 `UpdateStudentProfileRequest`(공통 9필드 + `grade`) / `UpdateGraduateProfileRequest`(공통 9필드 + GRADUATE 4필드)로 분리해 각 역할 필드만 노출 — 역할 외 필드는 record 정의 자체에 없어 클라이언트가 보낼 수 없다. **부분 수정 규약** — 필드가 `null` 이면 그 항목은 미변경(`profile` 이 null 이면 수정 대신 조회로 최신 프로필을 채움), 리스트가 빈 값이면 그 항목 전부 삭제(replace 규약).
- **수정도 애그리게이터**: 각 도메인의 **기존 수정 API** 에 위임한다 — 프로필 `MemberService.updateMyProfile`(역할별 프로필 DTO 의 `toUpdateMemberRequest()` 어댑터를 거쳐 호출), 기술 스택 `TechStackService.replace`, 관심 직군 `TargetJobService.replace`(STUDENT 경로), 졸업생 구직 공고 `JobPostService.createFromLink`/`delete`(GRADUATE 경로). 위임 후 `getMyPage` 와 공유하는 `buildResponse` 헬퍼로 최신 `MyPageResponse` 를 반환한다.
- **명함/포트폴리오 저장 = presigned 업로드 → string URL PATCH**: 졸업생 마이페이지의 명함(`businessCardImage`)·포트폴리오(`portfolio`)는 `PATCH /api/v1/members/mypage/graduate`(및 `/me`)의 **string(URL) 필드**다. 프론트는 파일을 직접 싣지 않고 용도별 발급 엔드포인트(`POST /me/business-card/presigned-url`·`/me/portfolio/presigned-url`)로 받은 `fileUrl` 을 이 string 필드에 넣어 저장한다(2-step, 위 회원 도메인 '용도별 presigned PUT URL 발급' 단락 참조). blob URL 직접 전송 문제를 이 플로우로 해소.
- **졸업생 구직 공고는 링크 전용 추가·삭제만** 위임 — `jobPostIdsToDelete`(삭제)를 먼저, `jobPostsToAdd`(`CreateJobPostLinkRequest` 리스트, 추가)를 나중에 처리. 추가는 `JobPostService.createFromLink`(`detailUrl` 만 채운 `PostContents` 저장 + `GraduateJobPost` 매핑, `jobType=null` 이므로 알람 분기 스킵)로 진행. 기존 11필드 공고 본문 편집은 범위 밖이며 개별 편집은 `PATCH /api/v1/job-post/{id}` 사용. 졸업생 권한·소유자 검증은 위임 메서드가 그대로 수행한다.
- **'수정 완료' = 원자성**: `updateStudentMyPage`/`updateGraduateMyPage` 는 단일 `@Transactional`. 위임 도메인 수정 메서드가 모두 `@Transactional`(전파 REQUIRED)이라 애그리게이터 트랜잭션에 참여 → 일부라도 실패하면 전체 롤백, 부분 저장 없음.
- **'취소' = 클라이언트 임시저장**: 편집 임시상태는 프론트가 보관, 취소 시 서버 호출 없이 폼을 버리고 조회 데이터로 복원. 별도 취소 엔드포인트 없음(서버 무상태).

### 인증/JWT
- `/api/v1/auth/signup`, `/api/v1/auth/login` 만 화이트리스트
- 화이트리스트: `swagger-ui/**`, `v3/api-docs/**`, `api/v1/auth/**`, `actuator/health`
- 토큰: HS512, `Authorization: Bearer <token>`, 클레임 키 `id` (회원 PK)
- `JwtAuthenticationFilter` → `CustomUserDetails` → `@LoginMember`로 컨트롤러에 주입
- 비밀번호: `BCryptPasswordEncoder`
- **회원가입 증명서 OCR 자동 채움 (`AuthServiceImpl.signup` + `CertificateOcrClient`)**: 회원가입 시 업로드된 재학/졸업증명서(PDF)를 데이터 파트(`POST /api/data/verify`)로 멀티파트 relay 해 OCR 추출값(이름·학과·학년)을 `Member.name`·`Member.department`(공통) + `Student.grade`(재학생만)에 자동 저장한다. 졸업생은 학년 개념이 없어 멤버 공통 2필드만 채운다(`Graduate` 미수정).
  - **클라이언트 (`CertificateOcrClient`/`CertificateOcrClientImpl`)**: 커피챗 `MatchingAlgorithmClient`/로드맵 `RoadmapAiClient` 와 동일한 얇은 `RestClient` 패턴. base-url 은 로드맵과 동일하게 `data.base-url`(env `DATA_BASE_URL`, 기본 `http://localhost:8000`) 재사용. `POST /api/data/verify` 멀티파트(`file` 파트, 업로드 바이트 그대로 relay) + 저장된 `member.id` 를 `X-User-Id` 헤더로 전달(데이터 파트 `get_current_user` 인증). 데이터 파트가 파일명 `.pdf` 확장자를 검사하므로 원본 파일명이 `.pdf` 가 아니면 `certificate.pdf` 로 대체. 응답 record `CertificateVerifyResponse(status, is_approved, student_info{type,name,department,grade})` — snake_case `@JsonProperty` 매핑 + `@JsonIgnoreProperties(ignoreUnknown=true)`(미사용 `user_id`/`message`/`details` 흘림). `grade` 는 문자열("1"~"4")이며 `StudentInfo.gradeAsInt()` 가 `Integer` 로 파싱(null/blank/형식오류 → null). 호출 실패·타임아웃·HTTP 오류·파일 읽기 실패는 모두 `ErrorStatus.CERTIFICATE_OCR_FAILED`(AUTH502).
  - **흐름 (단일 `@Transactional`)**: role 검증 → 이메일 중복 → 증명서 S3 업로드(기존 `uploadImage`) → **member + role 레코드 저장**(OCR 인증에 쓸 PK 확보) → **OCR `verify` 호출**(저장된 막 영속화된 엔티티이므로 `Member.applyVerifiedProfile`/`Student.assignGrade` 는 dirty checking 으로 반영, 재저장 없음). 외부 HTTP 를 트랜잭션 안에서 호출하는 점은 로드맵 `create` 선례와 동일.
  - **실패 정책 = best-effort (가입 차단 안 함)**: OCR 호출 실패(`CERTIFICATE_OCR_FAILED`)·미승인(`is_approved=false`)·`student_info` 누락이어도 **가입 자체는 성공**시키고 이름/학과/학년만 비워 둔다(추후 마이페이지 수정 가능). `applyCertificateOcr` 가 `RuntimeException` 을 흡수(`log.warn`)하므로 트랜잭션은 rollback-only 로 마킹되지 않는다. 데이터 파트(8000) 다운 시에도 가입이 막히지 않는다.
  - **학년 정규화 = `Student.clampGrade`(단일 정의)**: `Student.MIN_GRADE(1)`/`MAX_GRADE(4)` 상수와 `clampGrade`(상한 초과 → 4, 하한 미만·null → null, 예외 없음)를 엔티티가 보유하고 `MemberServiceImpl.sanitizeGrade`(사용자 입력 검증 — 하한 미만을 400 으로 거부)가 같은 상수를 재참조한다(매직 넘버 단일화). 가입 OCR 경로는 잘못된 값이 가입을 막으면 안 되므로 throw 대신 null 정규화(정책 분리).
  - **⚠️ 운영 주의 (배포 전 재협의)**: (1) `/verify` rate limit = **5/day(IP 기준)** — 백엔드가 단일 IP 로 호출하면 전체 가입자가 한도를 공유해 한도 초과 시 자동 채움이 동작하지 않는다(가입은 best-effort 로 통과). (2) **트랜잭션 가시성** — `/verify` 의 `get_current_user` 는 (공용 DB 전제 시) 별도 커넥션으로 member 를 조회하는데, 가입 트랜잭션이 아직 커밋 전이라 막 INSERT 한 member 가 안 보여 404 가 날 수 있다 → 자동 채움이 best-effort 로 생략된다. 로드맵 `/generate` 는 `member_id` 만 받고 회원 존재를 검증하지 않아 이 문제가 없지만 `/verify` 는 인증 조회를 하므로 영향을 받는다. 엄격히 채우려면 member 선커밋(트랜잭션 분리) 또는 데이터 파트 인증 방식 재협의 필요(범위 밖).

### 구직 공고 (job_post)
- **졸업생만 등록 가능** (`MemberRole.GRADUATE` 검증, `POST_CONTENTS_NOT_GRADUATE`)
- `PostContents` (공고 본문) + `GraduateJobPost` (졸업생-공고 매핑)으로 분리
- **등록 경로 2가지**:
  - **11필드 본문 등록** (`POST /api/v1/job-post`): 회사명/직무/마감일 등 본문을 모두 받는 기존 경로. `CreateJobPostRequest` 사용.
  - **링크 전용 등록** (`POST /api/v1/job-post/link`, GRADUATE 전용): '나의 공고 올리기' 모달에서 `detailUrl` 만 입력해 등록. `CreateJobPostLinkRequest` (`@NotBlank` + `^https?://.+`) → `JobPostService.createFromLink` 가 `PostContents.builder().detailUrl(...).build()` 저장 + `GraduateJobPost` 매핑. `jobType=null` 이라 알람 분기 스킵. 마이페이지 통합 수정(`UpdateGraduateMyPageRequest.jobPostsToAdd`)도 같은 서비스 메서드를 위임.
- **선배/일반 공고 분리 조회**: 구직 정보 화면이 선배 공고와 일반 공고를 시각적으로 분리 표시하므로 두 엔드포인트로 분리. 분리 기준은 `GraduateJobPost` **매핑 존재 여부** (데이터 모델 변경 없이 가능).
  - `GET /api/v1/job-post/graduates` — 매핑 존재(선배 등록). `JobPostService.getGraduatePosts` → `graduateJobPostRepository.findAll()` → `GraduatePostResponse.from(mapping)` 매핑. 응답 DTO `GraduatePostResponse` 는 본문 10필드 + 등록자(`Graduate.member.nickname`·`department` + `Graduate.jobType`·`careerYear`) 카드용 정보 포함.
  - `GET /api/v1/job-post/crawled` — 매핑 없음(크롤링 등). `JobPostService.getCrawledPosts` → `graduateJobPostRepository.findDistinctPostContentsIds()` 로 선배 매핑된 id 추출 → `postContentsRepository.findByIdNotIn(graduatePostIds)` 로 제외. 매핑이 0건이면 `findAll()` 로 폴백(IN 빈 컬렉션 SQL 오류 회피). 응답은 기존 `JobPostResponse` 재사용.
  - `GET /api/v1/job-post` (전체) 는 호환성 유지(deprecate 후보).
- 수정/삭제는 매핑 테이블 통해 소유자 확인
- `PostContents.jobType` enum 은 공고의 직무 속성. 구직 정보 화면의 **직무 필터** 용도 (`JobType` enum)
- **선호 언어는 다중 입력** (`PostContents.preferredLanguages`, `List<String>`): `@ElementCollection` + `@CollectionTable(name = "post_contents_preferred_language", joinColumns = post_contents_id)` 로 별도 컬렉션 테이블에 매핑 — 공고 카드에서 언어별 칩으로 표시. `Create/UpdateJobPostRequest`·`JobPostResponse` 모두 `List<String>`. `update` 는 null-coalescing(요청 리스트가 null 이면 기존값 유지). 컬렉션 테이블 행은 `PostContents` 삭제 시 Hibernate 가 자동 정리하므로 서비스 레벨 수동 캐스케이드 대상 아님.
- **삭제 캐스케이드 (서비스 레벨)**: `PostContents` 삭제 시 자식 행을 **서비스 코드에서 명시적으로 먼저 삭제** (`JobPostServiceImpl.delete`). 순서 = `JobAlarm` → `GraduateJobPost` → `PostContents`. 이유: `GraduateJobPost.postContents`, `JobAlarm.postContents` 가 `nullable = false` FK 인데 JPA cascade 설정이 없어, 부모만 지우면 영속성 컨텍스트의 자식이 transient 부모를 참조하게 되어 `TransientObjectException` 으로 터짐. 알람도 함께 hard delete 정책 (사용자의 알람 이력은 손실되지만, 공고가 사라진 상태의 dangling 알람을 막음).
- **update 시 jobType 변경 알람 디스패치 (`JobPostServiceImpl.update`)**: `oldJobType != newJobType` 일 때, 이 공고의 기존 `JobAlarm` 수신자 (= prev) 와 새 `jobType` 매칭 학생 (= curr) 의 차집합을 계산해 두 종류 알람을 발행. **Removed = prev − curr** → `"관심 직군에서 벗어난 공고로 변경되었습니다."`, **Added = curr − prev** → `"관심 직무에 새로운 공고가 등록되었습니다."`. 기존 알람 row 는 보존(삭제·갱신 X). 등록 졸업생 본인 항상 제외. prev 집합 정의는 이 공고(`postContentsId`)에 연결된 `JobAlarm` row 만 본다 (다른 알람 타입/다른 공고는 무관).
- **`create` 의 `jobType` null 가드 (`JobPostServiceImpl.create`)**: 알람 분기 진입 직전에 `if (saved.getJobType() == null) return JobPostResponse.from(saved);` 단락. 링크 전용 등록 경로(`createFromLink`)는 이 분기를 거치지 않지만, 향후 update 등 다른 경로에서 `jobType=null` 인 row 가 알람 분기로 들어올 경우의 NPE(`TargetJobCategory.valueOf(null)`) 를 차단한다.

### 관심 직군 (interested_job)
- 학생 마이페이지의 **관심 직군 칩** — 회원 ↔ `TargetJobCategory` enum 단순 매핑. `PostContents`(구직 공고)와는 FK 관계 **없음**.
- 입력은 카테고리(`TargetJobCategory`) 값뿐. 공고 ID 같은 FK는 받지 않음.
- `TargetJob` 엔티티는 `member_id`(NOT NULL FK) + `Interested_Job`(enum) 으로 구성.
- `TargetJobCategory` 와 `JobType` 은 같은 원티드 직군 태그 소스에서 11개 동일 값을 받지만 **타입 분리**. 두 enum 의 값 정렬은 사람이 직접 유지해야 함.
- 공고 ↔ 관심 직군 매칭(예: "내 관심 카테고리에 해당하는 새 공고 알람")이 필요해지면, FK 가 아닌 `PostContents.jobType` 과 `TargetJob.interestedJob` 의 **enum 값 비교**로 처리. 이 매칭이 두 enum 이 같은 값을 갖는 본질적 이유.
- **REPLACE 단일 endpoint (`TargetJobServiceImpl.replace`)**: `PATCH /api/v1/target-jobs` 로 본인 관심 직군 리스트 전체를 요청 리스트로 교체. 단일 `@Transactional` 안에서 `deleteByMemberId` 후 `save` N회. 입력 리스트 내부 중복은 서버에서 `Stream.distinct()` 로 제거. 빈 리스트 = 본인 row 전부 삭제(정상 200), `categories` 가 null = 400 (`_BAD_REQUEST`, `TargetJobHandler`). 응답은 교체 후 `List<TargetJobResponse>`. 마이페이지 수정 화면이 유일 진입점이라 단건 add/update/delete endpoint 는 두지 않는다. REPLACE 자체는 `JobAlarm` 을 발행하지 않으며, 공고↔관심직군 알람 매칭은 새 공고 등록 시점의 `JobPostServiceImpl.create` 가 담당.
- **(보류) 두 enum 통합 검토**: 같은 원티드 taxonomy 에서 받는 값이므로 `TargetJobCategory` + `JobType` → 단일 `JobCategory` 로 통합하면 `==` 매칭이 가능해지고 sync 부담이 사라짐. 현재는 의도적으로 보류 — 원티드 API 의 런타임 동적 연동 구조(static enum vs `String` + 화이트리스트 vs 참조 테이블)까지 함께 결정한 뒤 진행. 보류 동안 두 enum 의 값 정렬은 사람이 수동으로 유지.

### 기술 스택 (techstack)
- 회원 ↔ `TechStackName` enum 단순 매핑. 마이페이지 수정 화면이 유일 진입점이라 단건 add/delete endpoint 는 두지 않는다.
- **REPLACE 단일 endpoint (`TechStackServiceImpl.replace`)**: `PATCH /api/v1/tech-stacks` 로 본인 기술 스택 리스트 전체를 요청 리스트로 교체. 단일 `@Transactional` 안에서 `deleteByMemberId` 후 `save` N회. 입력 리스트 내부 중복은 서버에서 `Stream.distinct()` 로 제거. 빈 리스트 = 본인 row 전부 삭제(정상 200), `names` 가 null = 400 (`_BAD_REQUEST`, `TechStackHandler`). 응답은 교체 후 `List<TechStackResponse>`.

### 커피챗 (coffeechat)
- 흐름: 요청자가 카카오 오픈채팅 링크 포함해 생성 (`PENDING`) → 수신자만 `ACCEPTED`/`REJECTED`로 상태 변경
- **생성 검증 (`CoffeeChatServiceImpl.create`)**: receiver 조회 직후 → (1) `requester.id == receiver.id` 면 400 (`COFFEE_CHAT_SELF_REQUEST`); (2) role 쌍이 (STUDENT, GRADUATE) 또는 (GRADUATE, STUDENT) 가 아니면 400 (`COFFEE_CHAT_ROLE_MISMATCH`) — UNKNOWN 양쪽 모두 거부; (3) **중복 신청 방지** — requester→receiver 로 `PENDING`/`ACCEPTED` 상태 커피챗이 있으면 400 (`COFFEE_CHAT_ALREADY_REQUESTED`), `REJECTED` 만 있으면 재신청 허용; (4) **재요청 쿨다운** — (3) 통과 후에도 requester→receiver 의 가장 최근 `CoffeeChat.createdAt` 이 24시간(`RE_REQUEST_COOLDOWN` = `Duration.ofHours(24)` 상수) 이내면 429 (`COFFEE_CHAT_REQUEST_TOO_SOON`). 도메인 의도 = 학생 ↔ 졸업생 멘토링 매칭 + 거절 직후 빠른 반복 재신청 차단. (3)·(4) 는 각각 `CoffeeChatRepository.existsByRequesterIdAndReceiverIdAndStatusIn` · `existsByRequesterIdAndReceiverIdAndCreatedAtAfter` 파생 쿼리로 검사.
- 알람 발행 규칙:
  - 생성(PENDING): **수신자·요청자 양쪽에 1건씩**. 수신자 content = `String.format("%s %s님이 커피챗을 요청했어요!", requester.department, requester.nickname)` (요청 도착 — 요청자 정보 노출), 요청자 content = `String.format("%s %s님에게 커피챗을 신청했어요!", receiver.department, receiver.nickname)` (신청 접수 확인 — 수신자 정보 노출). 헤더 텍스트는 프론트가 type 별 상수로 표시.
  - ACCEPTED: **요청자/수신자 양쪽**에 카카오 링크 포함 알람 2건 (content 정적)
  - REJECTED: 요청자에게만 1건 (content 정적)
- 삭제(취소)는 요청자만 가능
- **삭제 캐스케이드 (서비스 레벨)**: `CoffeeChatServiceImpl.delete` 에서 `CoffeeChatAlarm` 먼저 `deleteByCoffeeChatId` 로 정리 → `CoffeeChat` 삭제. `CoffeeChatAlarm.coffeeChat` 가 `nullable = false` FK 라 정리 없이는 MySQL FK constraint 위반.
- **나의 활동 커피챗 목록 (`CoffeeChatService.getMyActivities`)**: 마이페이지 '커피챗 수' 카드를 클릭해 진입하는 '나의 활동/커피챗' 화면 백엔드. `findByRequesterId(memberId)` + `findByReceiverId(memberId)` 결과를 합쳐 **`status == ACCEPTED` 인 row 만 통과**(PENDING/REJECTED 는 서비스 단에서 제외)시키고 `CoffeeChat.id` 기준 중복 제거(같은 row 가 양쪽에 들어오는 안전 가드)한 뒤, 각 커피챗마다 본인 아닌 쪽(상대방)을 추출해 `CoffeeChatPartnerResponse` 로 조립. 상대방 부가 정보(`TargetJob` 관심직무 + `TechStack` 기술스택)는 `TargetJobRepository.findByMemberId` / `TechStackRepository.findByMemberId` 로 조회 — 타 도메인 레포를 직접 주입받는 패턴은 QnA·매칭 조립 선례와 동일. 필터 기준은 마이페이지 활동 카운트(`countMyAcceptedCoffeeChats`)와 일치 — 카운트와 목록이 같은 기준으로 동작하게 일원화.
- **커피챗 매칭 플로우 (`CoffeeChatMatchingController` / `CoffeeChatMatchingService(Impl)`)**: Figma 매칭 화면(정보 입력 → 결과 리스트 → 상대 상세)의 백엔드. 신규 컨트롤러는 base `/api/v1/coffeechat` 로 기존 `CoffeeChatController` 와 메서드 경로가 달라 충돌 없음. 엔드포인트 2개:
  - `POST /api/v1/coffeechat/matching` — 매칭 폼 6필드(`MatchingRequest`) 수신 → 알고리즘 호출 → 후보 카드 리스트(`MatchedCandidateResponse`). 후보 없으면 빈 리스트.
  - `GET /api/v1/coffeechat/matching/{memberId}` — 매칭 상대 상세(`MatchedCandidateDetailResponse`). "내 매칭 결과였는지"는 검증하지 않음.
  - 구 `GET /api/v1/coffeechat/my-activity` 엔드포인트는 제거(중복) — '나의 활동 / 커피챗' 은 `GET /api/v1/members/me/activity/coffeechats` (`MyActivityController`) 로 일원화한다. 위 매칭 컨트롤러는 매칭 화면 전용으로 책임을 좁힌다.
- **매칭은 무상태 pass-through**: 매칭 입력값·결과는 저장하지 않으며 매칭 이력 엔티티가 없다. 데이터 파트가 후보 풀 조회·점수 계산·정렬·top 3 선택까지 모두 수행하고 백엔드는 회원 ID 만 추출 — `CoffeeChat.jobScore/ability/goal` 은 계속 미사용 컬럼.
- **매칭 알고리즘 연동 (`MatchingAlgorithmClient` / `MatchingAlgorithmClientImpl`)**: 데이터 파트(`ddingconnect-data`)와 정합한다 — 백엔드는 폼 6필드(플랫 스키마)를 `POST /api/data/coffeechat/match` 로 1회 전달하고, 응답 `top_matches` (이미 정렬된 상위 3장)에서 회원 `id` 만 추출해 `List<Long>` 으로 반환한다. **후보 풀 조회·점수 계산·정렬·top N 선택은 모두 데이터 파트 책임**이며 백엔드는 클라이언트가 얇다. base URL 은 설정값 `matching.algorithm.base-url`(env `MATCHING_ALGORITHM_BASE_URL` 오버라이드, 기본 `http://localhost:8000`). 호출 실패·타임아웃·비정상 응답·`top_matches=null` 은 모두 502 (`MATCHING_ALGORITHM_FAILED`); `top_matches=[]`(후보 0건)는 정상 흐름으로 빈 리스트 반환.
- **요청 스키마 (`AlgorithmMatchRequest`)**: 데이터 파트 `RecommendInput` 와 정합 — `{year, gpa, major, job, tech_stacks, goal}` (snake_case, 6필드 플랫). 폼 `MatchingRequest.grade(Integer)` 는 Pydantic `str` 매핑에 맞춰 `String.valueOf(grade)` 로 변환(null → `""`). `capability(String)` 는 콤마 split → trim → 대문자 → `TechStackName` 화이트리스트 필터(미지원 토큰 버림) → 중복 제거 후 `List<String>` 으로 직렬화.
- **응답 스키마 (`AlgorithmMatchResponse` + `TopMatch`)**: 데이터 파트 `RecommendOutput`·`TopMatchItem` 와 정합 — `{status, top_matches: [{id, name, department, company, job, career, location, tech_stacks, match_score}, ...]}` envelope. 백엔드는 `id` 만 사용하고 나머지 부가 필드는 받기만 하고 무시한다(record 가 모든 필드를 파싱하므로 정의는 유지, 활용 안 함). 카드 조립은 라이브 DB 로 `CandidateProfileAssembler` 가 재조회한다(데이터 파트 응답의 stale 가능성 차단).
- **후보 프로필 조립 (`CandidateProfileAssembler`)**: `memberId` → 카드/상세 DTO 조립(매칭 결과·상대 상세·나의 활동 공통 로직). 회원 공통 정보 + 역할별 부가 정보(GRADUATE = 입학연도·회사·경력, 상세는 명함·공고 추가 / STUDENT = 학년)를 모은다. 입학연도는 `Member.studentNumber` 의 3·4번째 자리(0-index 2~3) 파생(별도 컬럼 없음, 4자리 미만이면 null), `region` 은 소스 미확정으로 항상 null. 직군은 `TargetJobRepository.findByMemberId`, 기술스택은 `TechStackRepository.findByMemberId` 로 전체 리스트 조회(0개면 빈 리스트). 타 도메인 레포 직접 주입은 조회 전용 조립 목적이며 QnA 선례상 허용. 응답 DTO `MatchedCandidateResponse`(카드)/`MatchedCandidateDetailResponse`(상세)는 한 record 에 역할별 nullable 필드를 두는 `MemberResponse` 패턴 — 상세에 "나와의 커피챗 상태"는 넣지 않는다(수락/거절은 기존 커피챗 알람으로 전달).
- **(현황) 카카오 오픈채팅 링크 생성 API 미구현**: 카카오 오픈채팅 링크를 발급/생성하는 API 는 아직 없다 (카카오 연동 미구현). 현재는 `CreateCoffeeChatRequest.kakaoOpenChatLink` (`String`, 검증 없음) 로 요청자가 링크를 직접 전달하면 `CoffeeChat.kakaoOpenChatLink` (`varchar(255)`) 에 그대로 저장만 한다. 추후 다른 방식으로 링크를 연결할 예정이며 연동 방식은 미정 — 커피챗 매칭 플로우의 신청 단계(`커피챗 신청하기`)에서 링크 입력/획득 경로를 함께 확정해야 한다.

### 통합 알람 (global/alarm + global/sse)
- **패키지**: 통합 알람 조회 계층(`AlarmController` / `AlarmSwagger` / `AlarmService(Impl)` / `AlarmResponse` / `RelativeTimeFormatter`)과 알람 종류 enum(`AlarmType`)은 `global/alarm/` 에 둔다. SSE 실시간 푸시 채널(`SseController` / `SseService(Impl)` / `SseEmitterRepository` / `SseTestController` / `SseSwagger`)과 도메인 알람의 커밋 후 SSE 발행 브리지(`AlarmNotificationEvent` / `AlarmNotificationListener`)는 `global/sse/` 에 둔다. 알람 조회는 SSE 와 무관한 순수 REST/JPA 기능이라 `sse` 하위가 아닌 형제 패키지로 분리했다. 의존 방향은 `global/sse → global/alarm` 단방향(`SseService` / `AlarmNotificationEvent` 가 `AlarmType` 참조) — `global/alarm` 은 `global/sse` 를 참조하지 않아 패키지 순환이 없다. 별도 `domain/alarm/` 도메인은 없음 — 알람은 자체 엔티티 없이 4개 도메인의 알람 엔티티를 집계하는 cross-cutting 관심사라 `global` 에 둔다. 알람 엔티티(`AnswerAlarm` / `JobAlarm` / `RoadmapAlarm` / `CoffeeChatAlarm`) 자체는 각 도메인 패키지에 그대로 존속.
- `AlarmType` enum(`global/alarm/AlarmType`)은 통합 알람 조회와 SSE 푸시 이벤트가 **공유** (단일 정의).
- 4종(`AnswerAlarm`, `JobAlarm`, `RoadmapAlarm`, `CoffeeChatAlarm`)을 합쳐 단일 `AlarmResponse` 리스트로 반환 (`GET /api/v1/alarms`)
- 정렬: `createdAt DESC`, null은 뒤로
- 읽음 처리는 Builder 패턴으로 새 인스턴스 만들어 저장 (불변 스타일)
- 각 타입별 소유자 검증 헬퍼 분리 (`verify*AlarmOwner`)
- **알람 row 저장 위치 = 각 도메인 `*ServiceImpl` 의 본체 save 직후, 같은 `@Transactional` 안**. 본체 저장 실패 시 알람 row 도 롤백되어 원자성 보장. SSE 실시간 푸시는 이벤트로 분리해 커밋 후 발행 (아래 "알람 SSE 발행" 참조).
  - `AnswerAlarm` — `AnswerServiceImpl.create()`, 질문 작성자에게 발행. 단, **본인이 본인 질문에 답변한 경우는 미발행** (자기 자신 알람 방지).
  - `RoadmapAlarm` — `RoadmapServiceImpl.create()`, **본인(생성자)** 에게 발행.
  - `JobAlarm` — `JobPostServiceImpl.create()`, `PostContents.jobType` 과 `TargetJob.interestedJob` 이 같은 학생들에게 N건 발행. **등록한 졸업생 본인 제외**, 같은 멤버가 동일 카테고리 중복 보유 시 1건만. enum 매칭은 `TargetJobCategory.valueOf(jobType.name())` 으로 (CLAUDE.md `interested_job` 의 값 매칭 방침과 정합).
  - `CoffeeChatAlarm` — `CoffeeChatServiceImpl.create()` / `updateStatus()`, PENDING 2건(수신자=요청 도착 + 요청자=신청 접수 확인, content 는 상대 학과·닉네임 포함 동적 생성), ACCEPTED 2건(요청자+수신자, 카카오링크), REJECTED 1건(요청자).
- **상대 시간 표시 (`AlarmResponse.relativeTime`)**: 응답 변환 시점에 `RelativeTimeFormatter.format(createdAt)` 으로 계산 ("방금 전 / N분 전 / N시간 전 / N일 전 / N개월 전 / N년 전"). 매 조회마다 재계산되어 시간 흐름에 따라 자연스럽게 업데이트됨. `createdAt` 도 함께 응답에 포함 (프론트 자체 포맷팅 여지 보존).
- **SSE 실시간 푸시**: `GET /api/v1/notifications/subscribe` 로 SSE 연결, `SseService.send(receiver, AlarmType, content)` 로 `notification` 이벤트 전송, 10초 주기 `ping` heartbeat. `SseTestController`(`POST /api/v1/notifications/test`) 수동 발송도 계속 동작.
- **알람 SSE 발행 (커밋 후)**: 도메인 4개 `*ServiceImpl`(`AnswerServiceImpl`/`JobPostServiceImpl`/`RoadmapServiceImpl`/`CoffeeChatServiceImpl`)이 알람 row `save` 직후 같은 `@Transactional` 안에서 `ApplicationEventPublisher.publishEvent(new AlarmNotificationEvent(receiver, type, content))` 발행 → `global/sse/AlarmNotificationListener` 가 `@TransactionalEventListener(AFTER_COMMIT)` 로 수신해 `SseService.send()` 호출. **본체 커밋 성공 후에만** 푸시되므로, 인라인 `send()` 와 달리 본체 롤백 시 DB 에 없는 알람을 클라이언트가 받는 "유령 알림"이 생기지 않음 (push 유실은 `GET /api/v1/alarms` 재조회로 self-heal — 실패 비대칭상 커밋 후 발행만 "DB = source of truth" 와 정합). 기존 SSE 코드(`SseService`/`SseServiceImpl`/`SseEmitterRepository`/`SseController`/`SseSwagger`/`SseTestController`)와 `AlarmType` 은 동결, 신규 파일은 `AlarmNotificationEvent`(record)·`AlarmNotificationListener` 2개뿐. 알람 content 리터럴은 각 서비스 `private static final` 상수로 추출(알람 row 빌더와 이벤트가 같은 상수 공유).

### Q&A
- `Question` 1:N `Answer`
- **답변 등록은 졸업생 + 질문 작성자 본인** (`AnswerServiceImpl.create`): 기본은 졸업생만 답변 가능하나, **본인이 작성한 질문글에 한해 학생(STUDENT)도 답변 허용**. 검증식 = `member.getRole() != GRADUATE && !question.getMember().getId().equals(member.getId())` 일 때만 403 (`ANSWER_NOT_GRADUATE`). 도메인 의도 = Q&A 답변은 멘토(졸업생)가 학생 질문에 답하는 흐름이되, 질문자 본인의 자문자답은 예외 허용. `Question` 조회를 역할 검증보다 **먼저** 수행하므로 존재하지 않는 questionId 는 403 이 아닌 404 (`QUESTION_NOT_FOUND`)가 먼저 발생. 본인 질문 본인 답변 시 알람 미발행 분기는 그대로 동작. 수정/삭제는 기존 작성자 권한만 검증 (역할 제한 X).
- 좋아요: `QuestionLike`, `AnswerLike` (복합키 `AnswerLikeId`), toggle 방식. **본인이 작성한 글에는 좋아요 불가** (`QuestionServiceImpl.toggleLike` / `AnswerServiceImpl.toggleLike` 진입부에서 작성자 == 호출자 체크. 위반 시 400 `QUESTION_SELF_LIKE` / `ANSWER_SELF_LIKE`).
- **카운트·내 좋아요 여부**는 응답 DTO 에 포함 (`QuestionResponse.likeCount`/`likedByMe`/`answerCount`, `AnswerResponse.likeCount`/`likedByMe`). 매 GET 시 `countBy*` / `existsByMemberIdAnd*` 쿼리 호출 (캐시 컬럼 미도입). 모든 Q&A GET API (`getList`, `getOne`, `Answer.getList`) 가 `Member` 를 받아 `likedByMe` 채움.
- **나의 활동 — 본인 질문 목록 (`GET /api/v1/questions/me`)**: `QuestionController.getMyQuestions` → `QuestionService.getMyQuestions` → `questionRepository.findByMemberId(memberId)` 로 본인 글만 조회. 응답 매핑은 기존 `toResponse(q, member)` 재사용 → `likeCount`/`answerCount`/`likedByMe` 일관 처리. **전체 목록 `getList(member)` 는 절대 수정하지 않는다** — Q&A 게시판은 전체 질문을 보여줘야 하므로 별도 엔드포인트로 분리. 마이페이지 '질문 수' 카드를 클릭해 진입하는 '나의 활동/Q&A' 화면용.
- 좋아요 토글 응답: `LikeToggleResponse(liked, likeCount)` — 토글 후 새 상태·카운트 즉시 반환. 프론트 추가 GET 불필요.
- 작성자만 수정/삭제
- **삭제 캐스케이드 (서비스 레벨)**:
  - `Answer` 삭제 (`AnswerServiceImpl.delete`): `AnswerAlarm` → `AnswerLike` → `Answer` 순. 자식 FK 모두 `nullable = false`/복합 PK 라 정리 필수.
  - `Question` 삭제 (`QuestionServiceImpl.delete`): 손자(`AnswerAlarm`, `AnswerLike`) → `Answer` → `QuestionLike` → `Question` 순. 손자는 2-level 경로(`@Modifying @Query`) 로 일괄 삭제. `QuestionServiceImpl` 가 `AnswerRepository/AnswerAlarmRepository/AnswerLikeRepository` 를 직접 주입받는 도메인 간 약결합 허용 (각 답변별 소유자가 다를 수 있어 `AnswerService.delete` 위임은 부적합).

### 로드맵 (roadmap)
- **흐름 (백엔드 중심, 커피챗 패턴)**: 프론트가 입력 폼 6필드 + `memberId` 를 백엔드 생성 API 로 전송 → 백엔드가 데이터 파트 `POST /api/data/generate` 를 호출해 AI 로드맵 생성 → AI 결과 JSON 을 백엔드 자체 DB `Roadmap.content` 에 저장 → **본인의 "생성된 로드맵" 카드 목록**(`List<RoadmapListResponse>`, 최신순, 방금 만든 항목 최상단)을 그대로 반환 → 프론트는 단일 호출로 "취업로드맵 최종 생성 결과" 화면(입력 폼 + 카드 리스트)을 렌더. 카드의 `id` 로 단건 조회(`GET /api/v1/roadmaps/{roadmapId}`)를 호출하면 본문(`content`) 확인 가능. 데이터 파트는 **AI 생성만** 담당하고 저장·조회·알람은 백엔드가 맡는다.
- **DB 미공유**: 백엔드 DB 와 데이터 파트 DB(`ddingconnect.db`)는 별개다. 데이터 파트가 자체 DB 에 저장하는 row 는 백엔드와 무관하며, 백엔드 상세 조회·알람은 백엔드 DB 의 `Roadmap` row 기준이다.
- **데이터 파트 호출 클라이언트 (`RoadmapAiClient` / `RoadmapAiClientImpl`)**: 커피챗 `MatchingAlgorithmClient` 와 동일한 얇은 클라이언트 패턴. `RestClient` 로 `POST {data.base-url}/api/data/generate?member_id={id}` 1회 호출 — 회원 식별자는 `member_id` **URL 쿼리 파라미터**로 전달(데이터 파트 시그니처 정합, `X-User-Id` 헤더 아님). 요청 body 는 데이터 파트 `RoadmapRequest` 스키마(6필드 플랫, snake_case `{grade, gpa, major, target_job, current_skills, target_company}`) — `targetJob`/`currentSkills` enum 은 Jackson 이 enum 명으로 직렬화하고, `currentSkills` 가 null 이면 빈 배열로 보낸다. 응답은 데이터 파트 `RoadmapResponse` JSON 을 `String` 으로 그대로 받아 `Roadmap.content` 에 저장(카드 형식 파싱 없음 — 데이터 파트가 `response_model` 로 정형화 보장). base URL 은 설정값 `data.base-url`(env `DATA_BASE_URL`, 기본 `http://localhost:8000`). 호출 실패·타임아웃·HTTP 오류는 모두 502 (`ROADMAP_AI_GENERATION_FAILED`). AI(OpenAI) 생성이 수십 초 걸릴 수 있어 read 타임아웃은 60초로 커피챗 클라이언트(5초)보다 길게 둔다.
- **`CreateRoadmapRequest` = 입력 폼 6필드**: `grade`(`Integer`) · `gpa`(`Double`) · `major` · `targetJob`(`TargetJobCategory`) · `currentSkills`(`List<TechStackName>`) · `targetCompany`. `targetJob`/`currentSkills` 가 enum 타입이라 데이터 파트 `TargetJobCategory`(11종)·`TechStackName`(24종)과 값이 정합하며, Jackson 역직렬화가 알 수 없는 값을 400 으로 거른다.
- **생성 엔드포인트 회원 식별 (`POST /api/v1/roadmaps`)**: 다른 엔드포인트와 동일하게 `@LoginMember Member` 로 회원을 받아 JWT 토큰의 회원 PK 로 식별한다. 컨트롤러는 `roadmapService.create(member.getId(), request)` 로 위임 — 서비스 시그니처(`create(Long memberId, CreateRoadmapRequest)`)는 무변경. `RoadmapServiceImpl.create` 의 `memberRepository.findById` + `MEMBER_NOT_FOUND`(404) 가드는 안전망으로 보존(탈퇴 경합 시점 대비). 데이터 파트 호출(`POST /api/data/generate?member_id={id}`)은 백엔드↔데이터 파트 내부 통신이라 그대로 유지 — `member_id` URL 쿼리 방식은 데이터 파트 시그니처 정합 목적.
- **`RoadmapServiceImpl.create` 플로우**: 회원 조회 → `RoadmapAiClient.generate` 데이터 파트 호출 → `validateContent` 로 AI 응답 검증(null/blank → `ROADMAP_INVALID_CONTENT` 400) → `Roadmap.content` 저장 → PDF 렌더 + S3 업로드 → `RoadmapAlarm` 저장 + `AlarmNotificationEvent` 발행(본인=생성자 1건) → **`RoadmapRepository.findByMemberIdOrderByCreatedAtDesc(memberId)` 로 본인 카드 목록 재조회 후 `List<RoadmapListResponse>` 로 반환**(같은 트랜잭션 내 find 호출은 JPA 가 pending save 를 flush 한 뒤 실행하므로 방금 만든 로드맵이 최상단에 포함됨). 단일 `@Transactional` — 외부 HTTP 호출을 트랜잭션 안에 포함하지만, 알람 row 와 본체 저장의 원자성(공통 알람 규칙)을 위해 유지한다.
- `Roadmap.content`: MySQL `TEXT` 컬럼 (`@Column(columnDefinition = "TEXT")`) — 데이터 파트 AI 가 생성한 `RoadmapResponse` JSON 문자열을 그대로 저장. 본문이 255자를 넘을 수 있어 `VARCHAR` 가 아닌 `TEXT`.
- **상세 조회 (`GET /api/v1/roadmaps/{roadmapId}`)**: `RoadmapServiceImpl.getOne(Member, Long)` 가 저장된 `Roadmap.content`(AI 생성 JSON) 를 `RoadmapResponse.content` 로 그대로 반환. **본인 소유 검증** — 타인 소유면 `ROADMAP_UNAUTHORIZED`(403), 미존재 시 `ROADMAP_NOT_FOUND`(404). 다운로드 엔드포인트와 동일한 권한 모델.
- **목록 조회 (`GET /api/v1/roadmaps`)**: `@LoginMember` 회원이 생성한 로드맵만 `RoadmapRepository.findByMemberIdOrderByCreatedAtDesc` 로 **최신순** 반환 — 마이 "생성된 로드맵" 카드 화면용. 전 회원 `findAll` 노출이 아니라 회원 스코프 + 인증 필수(화이트리스트 아님). **카드 화면 전용 경량 DTO `RoadmapListResponse(id, title, createdAt)`** 만 반환 — `content`(JSON) 전체를 내려보내지 않고 `roadmap_title` 만 추출해 페이로드 절감. `title` 추출 실패·빈 제목 시 `"로드맵"` 폴백(`RoadmapListResponse.from`). 전체 본문이 필요하면 상세 조회로 별도 호출. `Roadmap.title` 컬럼 신설은 하지 않음(엔티티 스키마 무변경 결정).
- **생성 응답 = 카드 목록 (`POST /api/v1/roadmaps` ≡ `GET /api/v1/roadmaps` 와 동일 DTO)**: 생성 직후 화면이 곧 "생성된 로드맵" 카드 리스트라(입력 폼 + 카드 그리드 한 화면) `POST` 응답을 **단일 `RoadmapResponse` 가 아닌 `List<RoadmapListResponse>`**(목록 조회와 동일 DTO)로 정의한다 — 프론트는 추가 `GET /api/v1/roadmaps` 호출 없이 생성 응답만으로 화면을 렌더한다. 방금 만든 로드맵의 전체 본문이 필요하면 카드의 `id` 로 단건 조회(`GET /api/v1/roadmaps/{roadmapId}`)를 호출한다(상세 응답은 여전히 `RoadmapResponse(id, memberId, content, createdAt)`).
- **목록 조회 GRADUATE 차단 가드 (`RoadmapController.getRoadmaps`)**: 졸업생 마이페이지/나의 활동에는 로드맵 탭 자체가 없으므로 `member.getRole() == MemberRole.GRADUATE` 면 진입부에서 `MemberHandler(MEMBER_FIELD_ROLE_MISMATCH)` 로 400 거부. 프론트가 호출하지 않더라도 서버 일관성을 위해 차단한다. 단건 조회·다운로드는 **역할 가드는 없고 본인 소유 검증만** 적용(GRADUATE 도 본인 소유면 통과) — 역할 가드는 목록에만 적용.
- **입력 6필드 미저장**: 현재 결정은 결과 `content` 만 저장하고 입력 폼 원본은 저장하지 않는다. 재생성·입력 이력·수정 화면 프리필이 필요해지면 별도 컬럼/엔티티 추가 검토 — 제품 요구 확정 후 결정.
- update API 미지원 (재생성 = 새 create + 기존 delete)
- 삭제는 소유자(member.id 일치)만 가능 (`ROADMAP_UNAUTHORIZED`)
- **삭제 캐스케이드 (서비스 레벨)**: `RoadmapServiceImpl.delete` 에서 `RoadmapAlarm` 먼저 `deleteByRoadmapId` 로 정리 → `Roadmap` 삭제. `RoadmapAlarm.roadmap` 이 `nullable = false` FK 라 정리 없이는 MySQL FK constraint 위반. 본체 정리 후 S3 객체(`roadmaps/{id}.pdf`) 도 `s3Service.deleteImage(buildKey(id))` 로 best-effort 삭제(try/catch + `log.warn`) — 실패 시 트랜잭션 영향 없음, dangling S3 객체는 후속 cleanup job 도입 검토(범위 밖).
- **PDF 다운로드 — 파일 URL 반환 (`GET /api/v1/roadmaps/{roadmapId}/download`)**: 백엔드는 파일 바이너리 직접 반환(`ResponseEntity<byte[]>`) 이나 리다이렉트가 아닌 **JSON 으로 S3 presigned URL** 을 반환한다. 응답 `RoadmapDownloadResponse(fileUrl, fileName, expiresAt)`. 프론트는 `fileUrl` 을 받아 `<a href download>` 또는 `window.location.href` 로 호출해 PDF 를 받아간다. 권한 검증은 `delete()` 와 동일 — 본인 소유 X → `ROADMAP_UNAUTHORIZED`(403), 미존재 → `ROADMAP_NOT_FOUND`(404). 컨트롤러는 `@LoginMember Member` 사용.
- **PDF 렌더링 — 생성 시 1회 (`RoadmapPdfRenderer`)**: 로드맵 **생성 직후** (`RoadmapServiceImpl.create` 의 `roadmapRepository.save(...)` 직후) 같은 `@Transactional` 안에서 PDF 1회 렌더 + S3 업로드. 다운로드 요청마다 재렌더링하지 않고 S3 객체를 재사용 → 다운로드 응답 지연 최소화. PDF 렌더·S3 업로드 실패 시 본체 저장도 함께 롤백(dangling DB row 방지). 외부 IO(S3) 가 트랜잭션 안에 포함되는 점은 `RoadmapAiClient.generate` 와 동일 패턴. 라이브러리는 `com.github.librepdf:openpdf 1.3.34` (LGPL/MPL, iText 7 AGPL 미채택). 렌더는 데이터 파트 `RoadmapResponse` 스키마와 1:1 매핑되는 내부 `record`(`@JsonProperty` snake_case 매핑)로 JSON 파싱 후 표지(`roadmap_title`) → 3단계 step 카드(파란 `phase_badge` 칩 + `title` + `카테고리 · 내용` 행) → 추천 자격증·활동 2열 회색 칩 → 마지막 페이지 `summary_advice` 순으로 그린다.
- **한글 폰트 vendoring (`classpath:fonts/NotoSansKR-Regular.ttf`, SIL OFL 1.1)**: OpenPDF 기본 폰트는 한글이 깨지므로 Google Fonts 의 Noto Sans KR Regular 정식 static TTF 를 `src/main/resources/fonts/` 에 레포에 정적으로 포함하고 `BaseFont.createFont(name, IDENTITY_H, EMBEDDED, NOT_CACHED, fontBytes, null)` 로 임베딩. 로드는 `@PostConstruct` 1회만(메모리 캐시). 빌드 시 외부 다운로드 방식·후속 PR 보강 방식은 미채택 — 외부 URL 의존 회피 + 1차 빌드부터 동작 보장. 폰트 라이선스 SIL OFL 1.1 (상업 사용 자유).
- **S3 키 규약 (`roadmaps/{roadmapId}.pdf`)**: 결정론적 키라 별도 `Roadmap.contentUrl` 컬럼 신설 불필요. `RoadmapServiceImpl.S3_KEY_PREFIX = "roadmaps/"` 상수 + `buildKey(roadmapId)` 헬퍼로 캡슐화.
- **다운로드 파일명 (sanitize + 폴백)**: `Roadmap.content` JSON 에서 `roadmap_title` 추출 후 Windows/macOS 금지문자(`\/:*?"<>|` + 제어문자) 를 정규식 제거하고 `"{title}.pdf"` 로 반환. JSON 파싱 실패·빈 제목 시 `"roadmap-{id}.pdf"` 폴백. 이 파일명은 presigned URL 의 `response-content-disposition` 쿼리(`attachment; filename="..."`)로 함께 지정돼 브라우저가 동일 이름으로 저장하도록 강제.
- **presigned URL TTL — 짧게**: `aws.s3.download-presign-ttl` (ISO-8601 `Duration`, 기본 `PT5M`) 로 `application-s3.yml` 에서 외부화. URL 유출 시 노출 시간 최소화 목적. 응답 `expiresAt = now + TTL`.
- **데이터 파트 rate limit 주의**: 데이터 파트 `/generate` 의 rate limit 은 `member_id` 를 URL 쿼리로만 받고 limiter 키로 쓰지 않아 IP 기준으로 동작 → 백엔드가 단일 IP 로 호출하면 전체 사용자가 한도를 공유한다. 회원별 제한이 필요하면 limiter 키 용도로 `X-User-Id` 헤더 병행 전송을 검토(범위 밖).

## 공통 패턴

- **부분 업데이트**: 엔티티를 Builder로 재구성, `null`인 필드는 기존 값 유지 (`request.x() != null ? request.x() : entity.getX()`)
- **권한 검증**: 작성자 ID 비교 후 도메인별 `*Handler` 예외 throw
- **응답 포맷**: 모든 컨트롤러는 `ApiResponse<T>` 래퍼 사용
- **성공 메시지**: 삭제·탈퇴 등 컨트롤러가 `ApiResponse` result 로 반환하는 사용자 노출 성공 메시지는 `global/common/SuccessMessage` 상수로 관리 (컨트롤러·`*ControllerTest` 가 동일 심볼 참조)
- **에러 코드**: `ErrorStatus` enum에 모두 정의 (`AUTH401`, `POST403`, `ALARM404` …)
- **감사**: `BaseEntity` 상속 → `createdAt`/`updatedAt` 자동 (`@JpaAuditingConfig`)
- **Swagger 어노테이션**: 컨트롤러는 `*Swagger` 인터페이스 implements 패턴으로 분리

## 화면 ↔ 도메인 매핑 (Figma 화면 구성 기준)

| 화면 섹션 | 백엔드 도메인 |
|---|---|
| 인증/온보딩 | `global/auth` + `member` |
| 홈 화면 (포인트/인사/활동 카운트) | `member`(`HomeService` 애그리게이터) — 활동 카운트는 마이페이지와 동일 집계 기준 재사용. 포인트 모달·충전 페이지 데이터(`PointBalanceResponse`/`PointChargeResponse`)도 같은 컨트롤러에서 노출. |
| 홈/알람 패널 (종 아이콘) | `global/sse` (4종 통합 조회 + SSE 실시간 푸시) |
| 폼 입력 (보라/초록) | `roadmap` / `techstack` / `interested_job` 등록·수정 |
| Q&A 스레드 (핑크) | `qna/question` + `qna/answer` |
| 구직 공고 (파랑) | `job_post` |
| 프로필 | `member` + `techstack` + `interested_job` |
| 마이페이지 | `member`(`MyPageService` 애그리게이터) — 활동 통계(`coffeechat`/`roadmap`/`qna/question`) + `techstack` + `interested_job` + `job_post` 조합 |
| 커피챗 매칭 (정보 입력 → 결과 → 상세) | `coffeechat` (매칭 알고리즘 연동, 무상태 pass-through) |
| 커피챗 요청/수락/거절 + 나의 활동 | `coffeechat` (요청 상태 전이, 수락된 커피챗 이력) |

### Figma 원본 PNG (lazy 로딩)

화면 구성·UI 플로우 관련 작업 시 아래 PNG를 `Read` 도구로 읽어 시각 정보를 확보한 뒤 작업한다. 위 매핑 표만으로 부족한 경우(픽셀 단위 배치, 색상, 컴포넌트 형태, 화면 전이 확인 등)에만 로드하여 토큰 소모를 줄인다.

- `backend/0409.png` — Figma export (`backend/` 하위, 모든 환경에서 접근 가능)

운영 규칙:
- 텍스트 매핑·도메인 규칙으로 답이 나오면 PNG를 굳이 읽지 않는다 (lazy)
- 화면 레이아웃/색상/컴포넌트 형태 질문, UI 명세 작성, 새 화면-도메인 매핑 추가 시에는 반드시 읽고 진행
- 새 Figma export가 추가되면 위 목록에 경로를 같이 등재할 것 (CLAUDE.md 자동 유지관리 규칙 적용)

## 테스트

- `src/test/java/.../EntityIntegrationTest.java` — 엔티티 관계 통합 검증 (649 lines)
- 도메인별 `*ControllerTest`, `*ServiceImplTest`
- `global/aws/S3ServiceTest` — `createUploadPresign` content-type 화이트리스트·presigned PUT URL 발급·최종 `fileUrl`(`getUrl(key)`) 규약·ttl 반영 검증 (`@Mock S3Presigner`, `ReflectionTestUtils` 로 `@Value` 필드 주입)
- `domain/member/controller/MemberControllerTest`·`domain/member/service/MemberServiceImplTest` — 용도별 presigned 발급 엔드포인트(`/me/profile-image|business-card|portfolio/presigned-url`) 위임·GRADUATE 가드(명함)·비허용 content-type 전파·요청 검증 실패(공백 fileName) 시 서비스 미호출 검증. 자산 삭제 엔드포인트(`DELETE /me/profile-image|business-card|portfolio`) 위임·GRADUATE 가드(명함)·S3 best-effort 삭제 + DB 클리어·이미 비어 있을 때 no-op·S3 실패 swallowing 검증.
- `support/WithMockLoginMember` — `@LoginMember` 인증 mock 어노테이션
- 테스트 설정: `src/test/resources/application.properties`, `logback-test.xml`

## 작업 시 주의

- **기존 코드 우선 확인·재사용 (작업 전 필수)**: 새 기능을 구현하기 전에, 같은 일을 하는 기존 메서드·클래스·유틸·상수·DTO 가 코드베이스에 이미 있는지 먼저 검색한다. 있으면 새로 만들지 말고 그대로 재사용한다. 기존 코드가 없더라도 같은 로직·값이 둘 이상의 위치에서 필요해지면 인라인 복붙 대신 공용 위치(`global/common` 등)로 추출해 단일 정의를 참조한다. 예: `@mju.ac.kr` 이메일 정규식은 각 DTO 인라인 대신 `global/common/ValidationPattern` 공용 상수로 통일.
- **하드코딩 절대 금지**: 문자열·숫자 리터럴은 `private static final` 상수 또는 enum/설정값으로 분리한다. 매직 넘버와 반복 문자열(이벤트명·URL·경로·메시지·TTL 등)을 코드에 직접 박지 말 것. 테스트 코드도 동일하게 적용하며, 공유 값은 `*TestConstants` 등 상수 클래스로 참조한다.
- 새 엔티티는 반드시 `BaseEntity` 상속
- 새 에러는 `ErrorStatus`에 정의 후 도메인 `*Handler`에서 throw
- 컨트롤러 응답은 `ApiResponse.onSuccess(...)` 통일
- JWT 화이트리스트 수정 시 `SecurityConfig`, `JwtAuthenticationFilter` 두 곳 모두 동기화
- 부분 업데이트 시 빌더 패턴 + null 체크 관례 유지

