# ddingconnect

명지대 캡스톤 디자인 — 졸업생/재학생 커뮤니티 플랫폼. 백엔드 레포 `somlon/cluade_clone` 기준 작업 문서.

## 문서 구성

상세 문서는 `docs/agent/` 에 분리돼 있다. 아래 `@import` 두 줄로 세션 시작 시 자동 로드된다.

@docs/agent/backend.md
@docs/agent/data.md

- `docs/agent/backend.md` — 백엔드(Spring Boot): 기술 스택·빌드·패키지 구조·핵심 도메인 규칙·공통 패턴·화면 매핑·테스트·작업 주의
- `docs/agent/data.md` — 데이터 파트(`ddingconnect-data`, FastAPI): 커피챗 매칭·OCR 인증·로드맵 AI 및 백엔드 연동 계약
- `docs/agent/TODO.md` — 진행 예정 작업. **`@import` 하지 않는다**(자주 바뀌어 자동 로드에서 제외). 사용자가 "to-do 리스트 수행" / "TODO N 작업" 등을 지시하면 `docs/agent/TODO.md` 를 먼저 읽고 그 절차대로 수행한다.

## 작업 레포 범위 규칙 (필수)

- **기본 작업 레포는 이 레포(`somlon/cluade_clone`) 단 하나다.** 사용자의 별도 지시가 없는 한 모든 코드 변경·브랜치 생성·커밋·푸시·PR 은 `cluade_clone` 에서만 수행한다.
- **`mju-capstone-4/ddingconnect-backend` 레포는 사용자가 해당 요청에서 "ddingconnect-backend 에서 작업하라" 고 명시적으로 지시한 경우에만** 쓰기 작업(파일 수정·브랜치 생성·커밋·푸시·PR)을 한다. 명시적 지시가 없으면 `ddingconnect-backend` 에 어떤 변경도 가하지 않는다.
- 비교·참고 목적의 **읽기 조회**(파일 내용 확인 등)는 `ddingconnect-backend` 에 대해서도 허용된다 — 금지 대상은 쓰기 작업뿐이다.
- 작업 대상 레포가 불확실하면 `ddingconnect-backend` 를 건드리지 말고 사용자에게 먼저 확인한다.

## 문서 자동 유지관리 (필수)

작업으로 인해 아래 항목 중 하나라도 변경되면, **같은 작업/커밋 내에서 해당 문서도 함께 갱신해야 한다.** 코드 변경과 문서 변경을 분리하지 말 것. 변경 성격에 맞는 파일(`docs/agent/backend.md` / `docs/agent/data.md` / `docs/agent/TODO.md` / 루트 `CLAUDE.md`)을 갱신한다.

갱신 트리거 (→ 갱신할 파일·섹션):
- **패키지/폴더 구조 변경**: 도메인 추가·삭제·이름 변경 → `backend.md` 의 `## 패키지 구조` 트리
- **새 도메인 규칙/플로우**: 새 엔티티 규칙, 상태머신, 알람 발행 규칙 등 → `backend.md` 의 `## 핵심 도메인 규칙`
- **공통 패턴 변경**: 응답 래퍼, 권한 검증, 빌더 패턴 등 관례 변경 → `backend.md` 의 `## 공통 패턴`
- **기술 스택/의존성 변경**: `build.gradle` 의존성 추가·제거·버전 변경 → `backend.md` 의 `## 기술 스택`
- **빌드/프로파일 설정 변경**: `application*.yml`, `build.gradle` 빌드 경로/프로파일 변경 → `backend.md` 의 `## 빌드 특이사항`
- **인증/JWT 정책 변경**: 화이트리스트, 토큰 정책, 클레임 변경 → `backend.md` 의 `### 인증/JWT`
- **테스트 인프라 변경**: 테스트 헬퍼/설정 추가·변경 → `backend.md` 의 `## 테스트`
- **에러 코드 체계 변경**: `ErrorStatus` 네이밍 규칙이나 prefix 변경 → `backend.md` 의 `## 공통 패턴` 에러 코드 항목
- **데이터 파트 변경**: `ddingconnect-data` 엔드포인트·요청/응답·매칭 로직·연동 계약 변경 → `data.md`
- **진행 예정 작업 변경**: TODO 추가·완료·내용 변경 → `TODO.md`

운영 원칙:
- 변경이 사소하더라도(예: 도메인 1개 추가) 위 트리거/규칙에 영향이 있으면 반드시 반영
- 변경점이 어느 섹션과도 맞지 않으면, 새 섹션을 만들어서라도 기록
- 단순 버그 수정·리팩터링이라 구조/규칙 변동이 없으면 갱신 불필요 (불필요한 diff 금지)
- 커밋 메시지에 `docs(...): ...` 항목을 함께 남기거나, 코드 커밋 본문에 갱신 사실 명시
- 의심스러우면 갱신하는 쪽을 택할 것 — 문서 누락보다 과기록이 낫다
