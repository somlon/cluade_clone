# 작업 계획서: SSE를 통한 알람 기능 (#13)

- 작성일: 2026-05-06
- 담당: changmin
- 이슈: #13
- 브랜치: feat/#13-sse-notification (base: develop)

---

## 1. 작업 개요

SSE(Server-Sent Events)를 이용한 실시간 알람 시스템을 구축한다.

### 구현 범위
- SSE 구독 엔드포인트 (`GET /api/v1/notifications/subscribe`)
- 내부 편의 메서드 `send()` — 어느 서비스에서든 주입 후 호출 가능
- 10초 주기 heartbeat(ping) 자동 전송
- 알람 발송 시 기존 도메인 알람 엔티티 저장은 각 도메인 서비스가 담당

### 구현 범위 외 (이번 PR 제외)
- 알람 목록 조회 API
- 읽음 처리 API

### 핵심 설계 원칙
- SSE 관련 코드는 **전부 `global/sse/` 패키지**에서 관리
- `SseEmitterRepository` = ConcurrentHashMap + emitter CRUD 메서드 + heartbeat
- 기존 알람 엔티티 저장은 각 도메인 서비스 책임 (SSE 레이어는 관여 안 함)
- SSE 인증: 기존 Spring Security JWT 필터 그대로 통과 (별도 처리 불필요)
- 상수는 별도 파일 없이 `SseServiceImpl` 내 `private static final` 필드로 관리
- build.gradle 변경 없음 (`SseEmitter`는 `spring-boot-starter-web` 내장)
- 하드코딩 절대 금지

---

## 2. 구현 대상 파일 목록

### 신규 생성

| 파일 | 위치 | 설명 |
|------|------|------|
| `AlarmType.java` | `global/sse/` | 알람 유형 enum (ANSWER, JOB, ROADMAP, COFFEE_CHAT) |
| `SseEmitterRepository.java` | `global/sse/` | ConcurrentHashMap 기반 emitter 저장소 + 10초 heartbeat |
| `SseService.java` | `global/sse/` | 서비스 인터페이스 |
| `SseServiceImpl.java` | `global/sse/` | subscribe() + send() 구현 + 상수 포함 |
| `SseController.java` | `global/sse/` | SSE 구독 엔드포인트 |
| `SseSwagger.java` | `global/sse/` | Swagger 인터페이스 |
| `SseConfig.java` | `global/config/` | async timeout 설정 |

### 수정

| 파일 | 변경 내용 |
|------|-----------|
| `global/response/code/status/SuccessStatus.java` | `SSE_CONNECTED` 성공 코드 추가 |

### 테스트 신규 생성

| 파일 | 위치 | 설명 |
|------|------|------|
| `SseControllerTest.java` | `test/.../global/sse/` | @WebMvcTest 슬라이스 테스트 |
| `SseServiceTest.java` | `test/.../global/sse/` | Mockito 단위 테스트 |

---

## 3. 상세 설계

### 3-1. AlarmType enum

```java
// global/sse/AlarmType.java
public enum AlarmType {
    ANSWER,
    JOB,
    ROADMAP,
    COFFEE_CHAT
}
```

---

### 3-2. SseEmitterRepository

ConcurrentHashMap으로 emitter를 관리하고, 등록 시 ScheduledExecutorService로 10초마다 heartbeat를 전송한다.

```java
// global/sse/SseEmitterRepository.java
@Component
public class SseEmitterRepository {

    private final ConcurrentHashMap<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public SseEmitter save(Long memberId, SseEmitter emitter) {
        emitters.put(memberId, emitter);
        emitter.onCompletion(() -> emitters.remove(memberId));
        emitter.onTimeout(() -> emitters.remove(memberId));
        emitter.onError(e -> emitters.remove(memberId));
        scheduleHeartbeat(memberId);
        return emitter;
    }

    public Optional<SseEmitter> findById(Long memberId) {
        return Optional.ofNullable(emitters.get(memberId));
    }

    public void deleteById(Long memberId) {
        emitters.remove(memberId);
    }

    private void scheduleHeartbeat(Long memberId) {
        scheduler.scheduleAtFixedRate(() -> {
            findById(memberId).ifPresentOrElse(
                emitter -> {
                    try {
                        emitter.send(SseEmitter.event().comment("ping"));
                    } catch (IOException e) {
                        deleteById(memberId);
                    }
                },
                () -> { /* emitter 없으면 아무것도 안 함 */ }
            );
        }, HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.SECONDS);
    }

    private static final long HEARTBEAT_INTERVAL = 10L;
}
```

---

### 3-3. SseService 인터페이스

```java
// global/sse/SseService.java
public interface SseService {
    SseEmitter subscribe(Member member);
    void send(Member receiver, AlarmType type, String content);
}
```

---

### 3-4. SseServiceImpl

상수는 `private static final` 필드로 이 클래스 안에 포함한다.

```java
// global/sse/SseServiceImpl.java
@Service
@RequiredArgsConstructor
public class SseServiceImpl implements SseService {

    private static final long SSE_TIMEOUT = 60 * 60 * 1000L;
    private static final String CONNECT_EVENT_NAME = "connect";
    private static final String CONNECT_MESSAGE = "connected";
    private static final String NOTIFICATION_EVENT_NAME = "notification";

    private final SseEmitterRepository sseEmitterRepository;

    @Override
    public SseEmitter subscribe(Member member) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        sseEmitterRepository.save(member.getId(), emitter);
        try {
            emitter.send(SseEmitter.event()
                .name(CONNECT_EVENT_NAME)
                .data(CONNECT_MESSAGE));
        } catch (IOException e) {
            sseEmitterRepository.deleteById(member.getId());
        }
        return emitter;
    }

    @Override
    public void send(Member receiver, AlarmType type, String content) {
        sseEmitterRepository.findById(receiver.getId()).ifPresent(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                    .name(NOTIFICATION_EVENT_NAME)
                    .data(Map.of("type", type.name(), "content", content)));
            } catch (IOException e) {
                sseEmitterRepository.deleteById(receiver.getId());
            }
        });
    }
}
```

---

### 3-5. SseController

```java
// global/sse/SseController.java
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class SseController implements SseSwagger {

    private final SseService sseService;

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@LoginMember Member member) {
        return sseService.subscribe(member);
    }
}
```

---

### 3-6. SseConfig

Spring MVC 기본 async 타임아웃이 SseEmitter timeout보다 짧으면 연결이 조기 종료될 수 있으므로,
async timeout을 SseEmitter timeout과 동일하게 맞춘다.

```java
// global/config/SseConfig.java
@Configuration
public class SseConfig implements WebMvcConfigurer {

    private static final long SSE_ASYNC_TIMEOUT = 60 * 60 * 1000L;

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setDefaultTimeout(SSE_ASYNC_TIMEOUT);
    }
}
```

---

### 3-7. 각 도메인 서비스에서의 사용 예시

각 도메인 서비스(AnswerService 등)는 알람 엔티티를 직접 저장한 뒤 `sseService.send()`를 호출한다.

```java
// 예: AnswerService 내부 (참고용, 이번 PR 구현 대상 아님)
answerAlarmRepository.save(AnswerAlarm.builder()
    .answer(answer)
    .content(alarmContent)
    .isRead(false)
    .build());
sseService.send(receiver, AlarmType.ANSWER, alarmContent);
```

---

## 4. 최종 패키지 구조

```
global/
├── sse/
│   ├── AlarmType.java
│   ├── SseEmitterRepository.java
│   ├── SseService.java
│   ├── SseServiceImpl.java
│   ├── SseController.java
│   └── SseSwagger.java
└── config/
    └── SseConfig.java              ← 신규 (기존 WebMvcConfig.java 수정 없음)
```

---

## 5. 테스트 계획

### SseControllerTest (@WebMvcTest)
- `GET /api/v1/notifications/subscribe` → 200 OK, Content-Type: `text/event-stream` 검증
- 비인증 요청 → 401 반환 검증

### SseServiceTest (Mockito 단위)
- `subscribe()` → SseEmitterRepository.save() 호출 확인, 연결 이벤트 전송 확인
- `subscribe()` → 연결 이벤트 전송 실패(IOException) 시 deleteById() 호출 확인
- `send()` → emitter 존재 시 SSE 이벤트 전송 확인
- `send()` → emitter 없을 때 아무 예외 없이 종료 확인
- `send()` → 전송 실패(IOException) 시 deleteById() 호출 확인

---

## 6. 리스크 및 대응

| 리스크 | 대응 |
|--------|------|
| Spring MVC async timeout이 SSE timeout보다 짧아 조기 종료 | SseConfig에서 async timeout을 SSE timeout과 동일하게 설정 |
| SseEmitter 연결 종료 시 emitter 누수 | onCompletion / onTimeout / onError 콜백에서 deleteById() 호출 |
| heartbeat 전송 실패 시 emitter 누수 | IOException catch → deleteById(), 예외 외부 전파 없음 |
| ScheduledExecutorService 스레드 누수 | 연결 해제 시 emitter null 확인 후 스케줄 자연 종료 유도 |
| @WebMvcTest에서 SseEmitter async 처리 | MockMvc asyncDispatch 또는 상태코드/Content-Type만 검증 |

---

## 7. 완료 기준

- [ ] `./gradlew.bat test` 전체 통과
- [ ] 하드코딩 없음 (상수는 SseServiceImpl, SseEmitterRepository 내 private static final)
- [ ] `GET /api/v1/notifications/subscribe` 연결 정상 동작
- [ ] 10초마다 heartbeat(ping) 자동 전송
- [ ] `sseService.send()` 어느 서비스에서든 주입 후 호출 가능
- [ ] 연결 해제 시 emitter 누수 없음
- [ ] verifier 검증 통과
- [ ] reviewer LGTM
- [ ] PR 설명에 작업 요약 포함
