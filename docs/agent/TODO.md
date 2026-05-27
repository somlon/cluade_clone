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


### TODO X — `POST /api/v1/roadmaps` 회원 식별 결함 정합 (`@LoginMember` 교체, breaking)

**배경**: 현재 `RoadmapController.createRoadmap` 가 `@RequestParam Long memberId` 로 회원을 식별한다. 로그인 회원(JWT) 과 무관하게 어떤 회원 id 를 쿼리로 넘기든 그 회원 명의로 로드맵이 저장되는 **보안 결함**. 예: tester02 토큰으로 `?memberId=1` 호출 → tester01 명의 저장 → tester02 본인 목록·상세에는 안 보임. PR #91 후 Swagger 검증 중 발견.

**결정 사항**:
- 컨트롤러를 `@LoginMember Member member` 로 받고 `@RequestParam memberId` 제거. `roadmapService.create(member.getId(), request)` 로 위임 (서비스 시그니처 무변경).
- 데이터 파트 호출(`POST /api/data/generate?member_id={id}`)은 그대로 — 백엔드 ↔ 데이터 파트 내부 통신이라 외부 노출 없음.

**작업 범위**:
- `RoadmapController.createRoadmap` — 시그니처에 `@LoginMember Member member` 주입, `@RequestParam memberId` 제거
- `RoadmapSwagger.createRoadmap` — `@RequestParam` Swagger 어노테이션 제거, `@Parameter(hidden = true) @LoginMember Member` 로 교체
- `RoadmapControllerTest.createRoadmap` — 쿼리 파라미터 제거, `WithMockLoginMember` 적용
- `docs/agent/backend.md` 의 "생성 엔드포인트 회원 식별 (`POST /api/v1/roadmaps?memberId={id}`)" 섹션 갱신 — `@LoginMember` 기반으로 표현 정합

**Breaking change 주의**: 프론트가 `?memberId=` 쿼리를 더 이상 붙이지 않아야 함. 프론트 변경과 동시 머지 권장.

**우선순위**: 인증된 회원만 호출 가능(JWT 필수) 이라 익명 공격 표면 없음. 회원 간 도용만 가능 — 운영 전 머지 권장 수준.
