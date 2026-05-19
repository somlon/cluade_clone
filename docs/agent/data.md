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

### POST /api/data/coffeechat/match — 커피챗 매칭 점수 (★ 백엔드 핵심 호출)

`routers/recommend.py`. **인증·rate limit 없음** (verify/generate 와 달리 `get_current_user` 미적용).

요청 (`MatchRequest`) — 두 명의 정보를 함께 전달:

```jsonc
{
  "requester": {                          // UserInfo
    "user_id": 1,                         // int
    "job": "DATA",                        // str (대문자 가정)
    "tech_stacks": ["PYTHON", "DJANGO"],  // List[str]
    "goal": "네이버"                       // str
  },
  "receiver": { /* UserInfo 동일 구조 */ }
}
```

- `UserInfo`: `user_id:int`, `job:str`, `tech_stacks:List[str]`, `goal:str`
- `MatchRequest`: `requester:UserInfo`, `receiver:UserInfo`
- `job`/`tech_stacks` 는 그냥 `str`/`List[str]` — **Enum 검증 없음**. 알고리즘은 값이 대문자(`"DATA"`, `"PYTHON"` 등)로 들어온다고 가정한다.

응답 — Pydantic `response_model` 없이 dict 직접 반환:

```jsonc
{
  "status": "success",
  "match_results": {
    "jobScore": 50.0,        // float 0~100
    "ability": 33.3,         // float 0~100 (자카드 유사도)
    "goal": 0.0,             // float 0 또는 100
    "totalMatchRate": 33.3   // float, 가중 평균
  }
}
```

### POST /api/data/verify — 재학/졸업증명서 OCR 인증

`routers/ocr_router.py`. **인증 필수**(`X-User-Id` 헤더 → `get_current_user`), **요청 제한 `5/day`**(IP 기준, slowapi).

- 요청: `multipart/form-data`, 필드 `file`. **PDF 만 허용** — 확장자 `.pdf` + `python-magic` 의 실제 MIME(`application/pdf`) 이중 검증.
- 처리: 파일 바이트를 네이버 OCR 로 전송 → 텍스트 파싱 → 로그인 유저 닉네임(`current_user.nickname`)과 증명서 추출 실명 대조. 승인 시 `Member.certificate` 를 `"VERIFIED"` 로 갱신·커밋.
- 응답 dict: `is_approved:bool`, `extracted_name:str|None`, `is_myongji:bool`, `is_certificate:bool`, `raw_text:str`.
- 에러: 비 PDF → 400, 그 외 → 500.

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

`backend.md` 의 커피챗 매칭 섹션(`MatchingAlgorithmClient`)이 가정하는 내용과 실제 `ddingconnect-data` 구현이 어긋난다. 백엔드 연동 코드를 확정하기 전 반드시 확인:

- **엔드포인트 경로**: 실제는 `POST /api/data/coffeechat/match`. 백엔드 문서의 추정 경로 `/match` 와 다름.
- **올바른 전제 = 후보 ID 리스트 반환**: 커피챗 매칭 알고리즘은 백엔드에 **후보 회원 ID 리스트**를 반환해야 한다 — `backend.md` 의 커피챗 매칭 가정이 맞다. 후보 회원 탐색·선별은 데이터 파트(알고리즘) 책임이다.
- **현재 구현 불일치**: 실제 `POST /api/data/coffeechat/match` 는 후보 리스트가 아니라, 요청 바디로 받은 두 명(`requester`·`receiver`)의 매칭 점수(`jobScore`/`ability`/`goal`/`totalMatchRate`)만 반환한다. 위 올바른 전제와 어긋난다 — 정렬은 데이터 파트(`ddingconnect-data`) 담당 영역이다.
- **네이밍 불일치**: 요청은 snake_case(`user_id`, `tech_stacks`), 응답은 camelCase 혼용(`jobScore`·`totalMatchRate` 는 camelCase, `ability`·`goal` 은 소문자). DTO 매핑 시 주의.
- **인증 비대칭**: `coffeechat/match` 만 인증·rate limit 이 없다(`verify`=5/day, `generate`=3/day 는 적용).
- **base URL**: 데이터 서버 기본 포트 8000 — 백엔드 `matching.algorithm.base-url` 기본값(`http://localhost:8000`)과 일치.
- **인증 모델**: 백엔드가 `X-User-Id` 헤더로 신원을 전달하면 이 서비스가 그 ID 로 공용 DB `member` 를 조회만 한다(JWT 검증 아님).

## 미완성 / 한계 / 노이즈

- 매칭 알고리즘은 룰 기반 PoC 수준 — `goal` 문자열 완전 일치 의존(오타·표기차에 취약), 임베딩·카테고리화 미구현.
- `UserInfo.job`/`tech_stacks` Enum 검증 없음 — 대문자 입력을 암묵 가정.
- `real_recommend.py`(scikit-learn 코사인 유사도)·크롤러 4종(`crawler`/`master_crawler`/`detail_crawler`/`data_cleaner`)·JSON 3종은 모두 **운영 API 미연결 실험/PoC**. 원티드 비공식 JSON API(`job_group_id=518` = 개발 직군) 직접 호출.
- `logger.py` 가 로거를 구성하나 어디서도 import 안 함. 라우터들은 예외를 `HTTPException(500)` `detail` 에 그대로 노출(`str(e)`)해 내부 정보 누출 여지.
- `dependencies.py` 에 중복 import 존재(병합 실수 정황).
- README·Dockerfile·CI·테스트 코드 전무. `.DS_Store` 가 커밋돼 있음.
