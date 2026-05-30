# 데이터 파트 (ddingconnect-data)

ddingconnect 의 Python/FastAPI 기반 데이터·AI 마이크로서비스. 별도 레포 `mju-capstone-4/ddingconnect-data` (분석 기준: `main`). 백엔드(`somlon/cluade_clone`, Spring Boot)가 HTTP 로 이 서비스를 호출한다.

> 루트 `CLAUDE.md` 가 `@import` 로 불러오는 데이터 파트 문서. `ddingconnect-data` 의 API·알고리즘·연동 계약이 바뀌면 이 파일을 갱신한다 (루트 `CLAUDE.md` 의 '문서 자동 유지관리' 규칙 참조).
> **이 레포는 읽기 참고용**이며 쓰기 작업 대상이 아니다 (루트 '작업 레포 범위 규칙' 참조).

## 역할 — 3가지 기능

`main.py` 가 라우터 3개를 등록한다. 기능별 담당이 분리돼 있다.

1. **커피챗 매칭 점수 계산** — 두 회원의 직무/역량/목표 유사도 점수 산출
2. **재학/졸업증명서 OCR 인증** — 네이버 클로바 OCR 로 명지대 증명서 검증
3. **커리어 로드맵 AI 생성** — OpenAI GPT 로 학년별 취업 로드맵 생성

루트 `GET /` 는 헬스 체크(`"띵커넥트 데이터 파트 서버 정상 작동 중!"`).

## 기술 스택

- FastAPI 0.128.8 / Starlette 0.49.3 / Pydantic 2.13.3
- ASGI 서버: Uvicorn 0.39.0 (운영 배포는 `gunicorn` 의존성으로 보아 `gunicorn -k uvicorn.workers.UvicornWorker` 의도로 추정)
- ORM/DB: SQLAlchemy + PyMySQL — 운영 MySQL, 로컬은 SQLite 기본값
- LLM: `openai` — 로드맵 생성, 모델 `gpt-4o-2024-08-06`
- OCR/HTTP: `requests`, 파일 검증 `python-magic`
- Rate limiting: `slowapi`
- 설정: `python-dotenv` (`.env`)
- ML(실험용): `scikit-learn`/`scipy`/`numpy` — `real_recommend.py` 에서만 사용, 운영 API 미연결
- 의존성 명세는 `requirements.txt` 만 존재 (`pyproject.toml` 없음). Python 버전 미명시(3.10~3.11 추정).
- **미사용 의존성**: `APScheduler`, `joblib`, `beautifulsoup4`, `python-jose` — requirements 에는 있으나 운영 코드에서 실사용 없음.

## 디렉터리 구조

레포 루트가 곧 애플리케이션 루트 (`src/` 같은 별도 패키지 없음).

```
ddingconnect-data/
├── main.py                  # FastAPI 앱 진입점, 라우터 3개 등록 (prefix=/api/data)
├── database.py              # SQLAlchemy 엔진/세션, get_db() 의존성
├── models.py                # ORM 모델: Member, Roadmap
├── dependencies.py          # get_current_user(X-User-Id 헤더 검증), slowapi limiter
├── logger.py                # 파일+콘솔 로깅 — 현재 어디서도 import 안 함(미사용)
├── init_test_db.py          # 로컬 SQLite 테이블 생성 + 테스트 유저(id=1) 시드
├── requirements.txt
│
├── routers/                 # API 엔드포인트 계층
│   ├── recommend.py         #   POST /coffeechat/match  (커피챗 매칭)
│   ├── ocr_router.py        #   POST /verify            (증명서 OCR 인증)
│   └── roadmap_router.py    #   POST /generate          (로드맵 AI 생성)
├── schemas/
│   └── roadmap_schema.py    #   로드맵 요청/응답 Pydantic + Enum
├── services/
│   ├── ocr_service.py       #   네이버 클로바 OCR 호출·파싱
│   └── roadmap_service.py   #   OpenAI 호출 로드맵 생성
├── recommend_model.py       # 커피챗 매칭 알고리즘 본체 (룰 기반)
│
└── (운영 API 미연결 — 원티드 채용공고 크롤링/전처리 실험 스크립트 + 산출 JSON)
    crawler.py · master_crawler.py · detail_crawler.py · data_cleaner.py · real_recommend.py
    jobs_data.json · final_jobs_data.json · cleaned_jobs_data.json
```

`routers`/`schemas`/`services` 에 `__init__.py` 가 없으나 Python 네임스페이스 패키지로 동작한다.

## API 엔드포인트

모든 라우터가 `prefix="/api/data"` 로 등록. 앱 제목 `"DdingConnect Data API"`.

### POST /api/data/coffeechat/match — 커피챗 매칭 (★ 백엔드 핵심 호출)

`routers/recommend.py`. **인증·rate limit 없음** (verify/generate 와 달리 `get_current_user` 미적용).

요청 (`RecommendInput`) — 신청자(학생)의 폼 6필드:

```jsonc
{
  "year": "3",                      // 학년 (str, Pydantic str 필수)
  "gpa": "4.0",
  "major": "응용소프트웨어학과",
  "job": "BACKEND",                 // 관심 직무 (대문자 enum 명 가정)
  "tech_stacks": ["JAVA","SPRING"], // 보유 기술스택 (대문자 enum 명 가정)
  "goal": "카카오"                   // 목표 기업
}
```

- 데이터 파트가 내부적으로 `db.query(Graduate).all()` 로 졸업생 후보 풀을 조회, 각각 점수 계산 후 정렬해 **상위 3장** 만 반환한다 — 백엔드는 후보 풀·정렬·top N 로직을 갖지 않는다.
- 신청자 본인 제외, 매칭 데이터 누락 후보 필터링은 모두 **데이터 파트 책임** (현재 코드 기준 `graduates = db.query(Graduate).all()` 후 점수만 0 되는 식 — 제외 정책은 데이터 파트가 결정).

응답 (`RecommendOutput`) — Pydantic 검증된 정형 응답:

```jsonc
{
  "status": "success",
  "top_matches": [
    {
      "id": 101,
      "name": "이선배",
      "department": "응용소프트웨어학과",
      "company": "카카오",
      "job": "BACKEND",
      "career": "3년차",
      "location": "위치 미상",
      "tech_stacks": ["JAVA","SPRING"],
      "match_score": 86.7              // totalMatchRate 동치
    }
    // ... 최대 3장
  ]
}
```

- `top_matches` 는 `match_score DESC` 로 이미 정렬된 상태.
- 후보 0건이면 `top_matches: []` 빈 배열. 백엔드는 이를 정상으로 처리.
- 백엔드는 응답에서 `id` 만 추출해 후속 카드 조립은 라이브 DB(`CandidateProfileAssembler`)로 재처리한다. 응답의 `name`/`department`/... 부가 필드는 받기만 하고 사용하지 않는다(stale 차단).

### POST /api/data/verify — 재학/졸업증명서 OCR 인증

`routers/ocr_router.py`. **인증 필수**(`X-User-Id` 헤더 → `get_current_user`), **요청 제한 `5/day`**(IP 기준, slowapi).

- 요청: `multipart/form-data`, 필드 `file`. **PDF 만 허용** — 확장자 `.pdf` + `python-magic` 의 실제 MIME(`application/pdf`) 이중 검증.
- 처리: 파일 바이트를 네이버 OCR 로 전송 → 텍스트 파싱(`ocr_service.parse_ocr_result`) → 명지대 + 재학/졸업증명서 키워드 + 성명/학과/학년 정규식 추출.
- **응답 스키마 (PR #13, 2026-05-30 `main` 반영)** — 승인 시 추출 정보(`student_info`)를 함께 내려준다:
  ```jsonc
  // 승인
  { "status": "success", "user_id": 1, "is_approved": true,
    "message": "명지대학교 재학생 인증이 완료되었습니다.",
    "student_info": {
      "type": "재학생" | "졸업생",   // 재학증명서/졸업증명서
      "name": "홍길동",              // 추출 실패 시 null 가능(승인이어도)
      "department": "데이터사이언스전공",
      "grade": "4"                  // 재학생일 때만 포함(문자열, 정규식 [1-4]). 졸업생은 키 자체 없음
    } }
  // 미승인
  { "status": "fail", "user_id": 1, "is_approved": false,
    "message": "...", "details": { "is_myongji": false, "is_certificate": true } }
  ```
- (구 스키마 `extracted_name`/`raw_text` 평면 dict 는 PR #13 으로 위 `student_info` 중첩 구조로 교체됨.)
- 에러: 비 PDF → 400, 그 외 → 500.

#### 백엔드 연동 (회원가입 자동 채움)

`backend.md` 의 `### 인증/JWT` 회원가입 OCR 자동 채움(`CertificateOcrClient`)과의 계약:

- **멀티파트 relay + `X-User-Id`**: 백엔드 `signup` 이 업로드된 증명서 PDF 바이트를 그대로 `file` 파트로 전달하고, 막 저장한 `member.id` 를 `X-User-Id` 헤더로 보낸다(데이터 파트 `get_current_user` 인증). 데이터 파트가 파일명 `.pdf` 확장자를 검사하므로 백엔드는 `.pdf` 아닌 원본명을 `certificate.pdf` 로 대체해 보낸다.
- **사용 필드**: 백엔드는 `is_approved` + `student_info{type,name,department,grade}` 만 사용(`name`/`department` → `Member`, `grade` → 재학생 `Student`). `user_id`/`message`/`details` 는 무시.
- **rate limit 한계**: `5/day(IP 기준)` — 백엔드 단일 IP 호출이라 전체 가입자가 한도 공유. 한도 초과·데이터 파트 다운 시 백엔드는 **best-effort 로 가입을 통과**시키고 자동 채움만 생략한다(증명서 미승인도 동일).
- **트랜잭션 가시성 주의**: `/verify` 의 `get_current_user` 가 (공용 DB 전제 시) 별도 커넥션으로 member 를 조회하는데, 백엔드 가입 트랜잭션이 커밋되기 전이라 막 INSERT 한 member 가 안 보여 404 가 날 수 있다. 이 경우에도 백엔드는 best-effort 로 가입을 통과시킨다. 엄격 채움이 필요하면 member 선커밋 또는 인증 방식 재협의 필요(범위 밖).

### POST /api/data/generate — 커리어 로드맵 AI 생성

`routers/roadmap_router.py`. **인증 필수**(`X-User-Id`), **요청 제한 `3/day`**(IP 기준). `response_model=RoadmapResponse`.

요청 (`RoadmapRequest`, `schemas/roadmap_schema.py`):

| 필드 | 타입 | 설명 |
|---|---|---|
| `grade` | `int` | 현재 학년 |
| `gpa` | `float` | 현재 학점 |
| `major` | `str` | 전공 |
| `target_job` | `TargetJobCategory` (Enum) | 관심 직군 |
| `current_skills` | `List[TechStackName]` (Enum 리스트) | 보유 기술 스택 |
| `target_company` | `str` | 목표 기업 |

응답 (`RoadmapResponse`):

| 필드 | 타입 | 설명 |
|---|---|---|
| `roadmap_title` | `str` | 로드맵 전체 제목 |
| `steps` | `List[RoadmapStep]` | 3단계 로드맵 |
| `recommended_certifications` | `List[str]` | 추천 자격증 (~4개) |
| `recommended_activities` | `List[str]` | 추천 대외활동 (~4개) |
| `summary_advice` | `str` | 조언 한마디 |

- `RoadmapStep`: `phase_badge:str`(예 `'1-2학년 기초'`), `title:str`, `details:List[StepDetail]`
- `StepDetail`: `category:str`(학습/자격증/목표/프로젝트/활동/포트폴리오/준비 중 1), `content:str`(콤마 구분 키워드)
- 처리: `roadmap_service.generate_roadmap()` 호출 → 결과를 `Roadmap` 테이블에 `content`(JSON 문자열)로 저장·커밋. 예외 시 `db.rollback()` + 500.

#### Enum 정의 (`schemas/roadmap_schema.py`)

- `TargetJobCategory` (11종): `BACKEND, FRONTEND, FULLSTACK, MOBILE, AI_ML, DATA, DEVOPS, SECURITY, GAME, EMBEDDED, ETC`
- `TechStackName` (24종): `JAVA, PYTHON, JAVASCRIPT, TYPESCRIPT, KOTLIN, SWIFT, C, CPP, GO, RUST, RUBY, PHP, SCALA, REACT, VUE, ANGULAR, SPRING, DJANGO, NODE_JS, DOCKER, KUBERNETES, AWS, GCP, AZURE`

## 커피챗 매칭 알고리즘 (`recommend_model.py`)

**임베딩·ML 모델 없는 순수 룰 기반.** `get_coffeechat_match_result(user_a, user_b)` 가 점수 3개를 계산한다. (파일 상단 주석은 "테스트 파일"이라 적혀 있으나 실제로 `routers/recommend.py` 가 이 파일을 import 해 운영에 쓴다.)

1. **jobScore (직무, 0~100)** — `calculate_job_score`
   - 두 직무 문자열 완전 동일 → `100.0`
   - 둘 다 개발 직군 집합 `{BACKEND, FRONTEND, FULLSTACK, AI_ML, DATA, DEVOPS}` 에 속하면 → `50.0`
   - 그 외 → `0.0`
   - 하드코딩 주의: 이 dev 집합에 `MOBILE`/`SECURITY`/`GAME`/`EMBEDDED` 가 빠져 있어, 예컨대 모바일–프론트엔드는 50점이 아닌 0점이 된다.
2. **ability (역량, 0~100)** — `calculate_ability_score`
   - 두 유저 `tech_stacks` 의 **자카드 유사도** `|A∩B| / |A∪B| × 100`, 소수 1자리.
   - 둘 다 비어 있으면 `0.0`.
3. **goal (목표, 0~100)** — `calculate_goal_score`
   - 두 `goal` 문자열 완전 일치 → `100.0`, 아니면 `0.0`. 단순 완전 일치라 `"네이버"` vs `"네이버 (NAVER)"` 는 0점(주석에 "추후 기업 카테고리화" 명시, 미구현).
4. **totalMatchRate** — 가중 평균 `jobScore×0.4 + ability×0.4 + goal×0.2`, 소수 1자리.

입력 피처는 `job`·`tech_stacks`·`goal` 3개뿐. `user_id` 는 점수 계산에 미사용. 학년·학점·전공은 매칭에 미반영.

## 로드맵 AI 생성 (`services/roadmap_service.py`)

- OpenAI 실제 호출. 클라이언트 `OpenAI(api_key=os.getenv("OPENAI_API_KEY"))`, 모델 `gpt-4o-2024-08-06`(하드코딩).
- `client.beta.chat.completions.parse(...)` 의 **Structured Outputs** — `response_format=RoadmapResponse` 로 Pydantic 스키마를 넘겨 응답을 `RoadmapResponse` 객체로 직접 파싱.
- 시스템 프롬프트: "명지대 학생 취업을 돕는 10년차 커리어 컨설턴트" 역할. UI 구조 강제 — `steps` 는 반드시 3단계(`'1-2학년 기초'`/`'3학년 실전'`/`'4학년 취업'`), 각 step `details` 3개, `content` 는 키워드 위주, 추천 자격증·활동 각 4개.
- 유저 프롬프트: `RoadmapRequest` 필드를 f-string 으로 채움. `target_job` Enum 은 `JOB_CATEGORY_KOR` dict 로 한글화(예 `BACKEND` → `"백엔드 개발자"`).

## OCR 인증 (`services/ocr_service.py`)

- 네이버 클로바 OCR V2 호출 (`X-OCR-SECRET` 헤더).
- 정규식 `r"성\s*명\s*:?\s*([가-힣]{2,4})"` 로 성명 추출, 공백 제거 후 `"명지대학교"` + (`"재학증명서"` 또는 `"졸업증명서"`) 키워드 포함 여부로 1차 승인 판정.

## DB / 데이터

- SQLAlchemy ORM. 접속 URL 환경변수 `DATABASE_URL`, **미설정 시 기본 `sqlite:///./ddingconnect.db`**. 운영은 `pymysql` 정황상 MySQL.
- 테이블 2개:
  - `member` — `id`(PK), `email`(unique), `nickname`, `password`, `student_number`, `department`, `certificate`(기본 `"NONE"`, OCR 성공 시 `"VERIFIED"`), `is_deleted`, `created_at`, `updated_at`.
  - `roadmap` — `id`(PK), `member_id`(FK→`member.id`), `content`(Text, `RoadmapResponse` JSON 문자열), `created_at`, `updated_at`.
- **공용 DB 전제**: 코드 주석상 이 `member` 테이블은 Spring 백엔드와 같은 DB 를 공유한다. 데이터 서비스가 백엔드 DB 를 직접 read/write 한다. `init_test_db.py` 가 정의하는 컬럼 셋이 백엔드 실제 스키마와 일치하는지는 이 레포만으로 확인 불가 — 컬럼 불일치 시 충돌 위험.
- ML 모델 파일(`.pkl`/`.joblib` 등) 없음.
- `*_jobs_data.json` 3종은 원티드 크롤러 산출물로, 운영 API 와 무관.

## 실행 / 환경변수

- README·Dockerfile·docker-compose **없음**.
- 로컬 실행: `uvicorn main:app --reload` (기본 포트 8000).
- 로컬 DB 초기화: `python init_test_db.py` — SQLite 테이블 생성 + 테스트 유저(`id=1`, 닉네임 `"조휘성"`, 학번 `"60211234"`) 시드. OCR/로드맵 엔드포인트는 `X-User-Id` 가 DB 실제 회원과 매칭돼야 동작하므로 로컬 테스트에 시드가 필요.

`.env` 참조 환경변수:

| 변수 | 사용처 | 비고 |
|---|---|---|
| `DATABASE_URL` | `database.py` | 미설정 시 `sqlite:///./ddingconnect.db` |
| `OPENAI_API_KEY` | `roadmap_service.py` | 기본값 없음 |
| `OCR_INVOKE_URL` | `ocr_service.py` | 미설정 시 `ValueError` |
| `OCR_SECRET_KEY` | `ocr_service.py` | `X-OCR-SECRET` 헤더 |

## 백엔드 연동 시 주의 — 계약 불일치

`backend.md` 의 커피챗 매칭 섹션(`MatchingAlgorithmClient`)과 실제 `ddingconnect-data` 구현의 정합 상태와 주의점:

- **현 합의 — 데이터 파트가 후보 풀·정렬·top N 담당**: 데이터 파트가 자체 DB `db.query(Graduate).all()` 로 졸업생 풀을 가져와 점수 계산·정렬·상위 3장 추출까지 모두 처리한다. 백엔드는 폼 6필드를 1회 전달하고 회원 ID 만 받아 후속 카드 조립을 라이브 DB 로 재처리한다.
- **요청 스키마 = 6필드 플랫**: 요청 본문은 `{year, gpa, major, job, tech_stacks, goal}` (snake_case). 백엔드 record(`MatchingAlgorithmClientImpl.AlgorithmMatchRequest`) 가 `MatchingRequest` 폼을 변환해 직렬화. `MatchingRequest.grade(Integer)` → `year(String)` 변환은 Pydantic `str` 필수 매핑 정합 목적.
- **응답 스키마 = top 3 카드 배열**: 응답은 `{status, top_matches: [TopMatchItem, ...]}`. 백엔드는 `AlgorithmMatchResponse`/`TopMatch` record 로 받지만 **`id` 외 부가 필드는 사용하지 않는다** — `CandidateProfileAssembler` 가 라이브 DB 로 카드를 재조립.
- **`top_matches=[]` 정상 처리**: 후보 0건은 빈 리스트 반환(예외 아님). `top_matches=null` 또는 envelope 자체 누락만 502 변환.
- **네이밍**: 요청·응답 모두 snake_case 일관(`tech_stacks`, `top_matches`, `match_score`). 백엔드 record 는 camelCase 필드명 + `@JsonProperty` 명시 매핑.
- **인증 비대칭**: `coffeechat/match` 만 인증·rate limit 이 없다(`verify`=5/day, `generate`=3/day 는 적용). 백엔드도 `X-User-Id` 헤더 미전송 — 데이터 파트가 후보 풀에서 신청자 본인을 제외하려면 백엔드가 추가로 신청자 ID 를 전달해야 하지만, 현재 스키마(`RecommendInput`) 엔 신청자 ID 필드 자체가 없다. 본인 제외 정책은 데이터 파트 후속 작업 대상.
- **base URL**: 데이터 서버 기본 포트 8000 — 백엔드 `matching.algorithm.base-url` 기본값(`http://localhost:8000`)과 일치.
- **`job`/`tech_stacks` 값 정합**: 데이터 파트 알고리즘 `calculate_job_score`/`calculate_ability_score` 는 두 문자열 완전 일치 비교(대문자 enum 명 가정). 프론트 폼이 `TargetJobCategory`/`TechStackName` enum 명을 그대로 송신해야 점수가 정상 계산된다. 백엔드는 `tech_stacks` 만 `TechStackName` 화이트리스트로 정규화하고, `job` 은 폼 값 pass-through.

### 로드맵 생성 연동 (`/api/data/generate`)

`backend.md` 의 로드맵 섹션(`RoadmapAiClient` / `RoadmapAiClientImpl`)과 데이터 파트 `POST /api/data/generate` 의 연동 계약:

- **역할 분담**: 데이터 파트는 AI 로드맵 생성만 담당하고 저장·상세 조회·알람은 백엔드가 맡는다(커피챗과 동일한 백엔드 중심 패턴). 백엔드 DB 와 데이터 파트 DB(`ddingconnect.db`)는 공유하지 않는 별개 DB.
- **회원 식별 = `member_id` URL 쿼리**: 백엔드는 `POST /api/data/generate?member_id={id}` 로 호출한다 — 회원 식별자를 `X-User-Id` 헤더가 아닌 URL 쿼리 파라미터로 전달. 위 `### POST /api/data/generate` 엔드포인트 섹션은 `X-User-Id` 헤더 인증으로 기술돼 있으나, 백엔드 구현·연동 계약은 `member_id` URL 쿼리 방식이다 — 데이터 파트 `main` 재분석 시 위 섹션을 정합 갱신할 것.
- **요청 스키마**: 본문은 데이터 파트 `RoadmapRequest` 6필드(`{grade, gpa, major, target_job, current_skills, target_company}`, snake_case). 백엔드 record(`RoadmapAiClientImpl.RoadmapGenerationRequest`)가 `CreateRoadmapRequest` 를 변환해 직렬화하며, `target_job`/`current_skills` 는 enum 명으로 직렬화된다(`currentSkills` 가 null 이면 빈 배열).
- **응답 처리**: 백엔드는 데이터 파트 `RoadmapResponse` JSON 을 파싱 없이 `String` 으로 받아 백엔드 `Roadmap.content` 에 그대로 저장하고, 상세 조회 시 그 문자열을 그대로 반환한다(stale 가능성 없음 — 백엔드 DB 가 source of truth).
- **rate limit**: `/generate` 는 `member_id` 를 URL 쿼리로만 받고 limiter 키로 쓰지 않아 IP 기준으로 동작 → 백엔드가 단일 IP 로 호출하면 전체 사용자가 한도를 공유한다. 회원별 제한이 필요하면 `X-User-Id` 헤더 병행 전송 검토(범위 밖).

## 미완성 / 한계 / 노이즈

- 매칭 알고리즘은 룰 기반 PoC 수준 — `goal` 문자열 완전 일치 의존(오타·표기차에 취약), 임베딩·카테고리화 미구현.
- `UserInfo.job`/`tech_stacks` Enum 검증 없음 — 대문자 입력을 암묵 가정.
- `real_recommend.py`(scikit-learn 코사인 유사도)·크롤러 4종(`crawler`/`master_crawler`/`detail_crawler`/`data_cleaner`)·JSON 3종은 모두 **운영 API 미연결 실험/PoC**. 원티드 비공식 JSON API(`job_group_id=518` = 개발 직군) 직접 호출.
- `logger.py` 가 로거를 구성하나 어디서도 import 안 함. 라우터들은 예외를 `HTTPException(500)` `detail` 에 그대로 노출(`str(e)`)해 내부 정보 누출 여지.
- `dependencies.py` 에 중복 import 존재(병합 실수 정황).
- README·Dockerfile·CI·테스트 코드 전무. `.DS_Store` 가 커밋돼 있음.
