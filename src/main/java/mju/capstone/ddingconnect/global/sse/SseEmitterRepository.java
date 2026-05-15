package mju.capstone.ddingconnect.global.sse;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * SSE Emitter를 memberId 기준으로 관리하는 저장소.
 *
 * 구조:
 *  - emitters        : memberId → SseEmitter (실제 SSE 스트림 객체)
 *  - heartbeatTasks  : memberId → ScheduledFuture (10초 주기 ping 태스크)
 *
 * Emitter 생명주기:
 *  save() → [연결 유지] → onCompletion / onTimeout / onError → deleteById()
 *
 * 동시성:
 *  두 Map 모두 ConcurrentHashMap을 사용해 멀티스레드 환경(heartbeat 스레드 + 요청 스레드)에서 안전하게 동작.
 */
@Slf4j
@Repository
public class SseEmitterRepository {

    private static final long HEARTBEAT_INTERVAL_SECONDS = 10L;
    private static final String HEARTBEAT_EVENT_NAME = "ping";
    private static final String HEARTBEAT_DATA = "ping";

    // memberId → SseEmitter: 현재 연결된 SSE 스트림
    private final ConcurrentHashMap<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    // memberId → ScheduledFuture: 각 연결에 붙은 heartbeat 태스크
    // Emitter 제거 시 반드시 함께 취소해야 스레드 누수가 발생하지 않음
    private final ConcurrentHashMap<Long, ScheduledFuture<?>> heartbeatTasks = new ConcurrentHashMap<>();

    // 단일 스레드로 모든 heartbeat를 순차 처리 (연결 수가 많아지면 스레드 수 조정 필요)
    private final ScheduledExecutorService scheduler;

    public SseEmitterRepository() {
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    // 테스트 전용 생성자: 외부에서 mock scheduler를 주입해 heartbeat 동작 검증
    SseEmitterRepository(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * 새 Emitter를 등록한다.
     *
     * 같은 memberId로 이미 연결이 있으면 기존 연결을 먼저 종료(중복 구독 방지).
     * 등록 후 연결 종료 콜백(onCompletion/onTimeout/onError)을 설정하고 heartbeat를 시작한다.
     */
    public SseEmitter save(Long memberId, SseEmitter emitter) {
        // 기존 연결이 있으면 먼저 정리 (같은 사용자의 중복 구독 방지)
        completeExisting(memberId);

        emitters.put(memberId, emitter);

        // 클라이언트가 정상적으로 연결을 끊었을 때 (EventSource.close() 등)
        emitter.onCompletion(() -> {
            log.info("[SSE] 연결 종료 - memberId={}", memberId);
            deleteById(memberId);
        });

        // 설정된 timeout이 지나도록 응답이 없을 때
        emitter.onTimeout(() -> {
            log.info("[SSE] 연결 타임아웃 - memberId={}", memberId);
            deleteById(memberId);
        });

        // 네트워크 오류 등 예외 상황
        emitter.onError(e -> {
            log.warn("[SSE] 연결 오류 - memberId={}", memberId, e);
            deleteById(memberId);
        });

        scheduleHeartbeat(memberId);
        log.info("[SSE] Emitter 등록 완료 - memberId={}", memberId);
        return emitter;
    }

    /**
     * memberId에 해당하는 Emitter를 조회한다.
     * 연결되지 않은 사용자면 Optional.empty()를 반환하므로, 호출부에서 ifPresent로 안전하게 처리 가능.
     */
    public Optional<SseEmitter> findById(Long memberId) {
        return Optional.ofNullable(emitters.get(memberId));
    }

    /**
     * Emitter와 heartbeat 태스크를 함께 제거한다.
     *
     * heartbeat 태스크를 취소하지 않으면 스레드가 계속 살아있어 누수가 발생하므로
     * Emitter 제거 시 반드시 이 메서드를 통해 처리한다.
     */
    public void deleteById(Long memberId) {
        emitters.remove(memberId);

        // heartbeat 태스크도 함께 취소 (cancel(false): 실행 중인 태스크는 중단하지 않고 다음 스케줄만 취소)
        ScheduledFuture<?> task = heartbeatTasks.remove(memberId);
        if (task != null) {
            task.cancel(false);
        }
        log.info("[SSE] Emitter 삭제 - memberId={}", memberId);
    }

    /** 서버 종료 시 scheduler 스레드풀을 정리한다. */
    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
    }

    /**
     * 같은 memberId로 기존 연결이 있으면 종료하고 heartbeat 태스크를 취소한다.
     *
     * emitters.remove()를 먼저 호출한 뒤 complete()를 호출하는 이유:
     * complete()가 onCompletion 콜백(→ deleteById)을 트리거하므로,
     * 미리 Map에서 꺼내두지 않으면 deleteById가 새로 등록될 Emitter를 제거할 수 있다.
     */
    private void completeExisting(Long memberId) {
        SseEmitter existing = emitters.remove(memberId);
        if (existing != null) {
            log.info("[SSE] 기존 Emitter 종료 (중복 구독) - memberId={}", memberId);
            try {
                existing.complete();
            } catch (Exception ignored) {
                // 이미 종료된 Emitter에 complete()를 호출하면 예외가 발생할 수 있으나 무시
            }
        }
        ScheduledFuture<?> existingTask = heartbeatTasks.remove(memberId);
        if (existingTask != null) {
            existingTask.cancel(false);
        }
    }

    /** heartbeat 태스크를 등록한다. 첫 ping은 10초 후부터 시작된다. */
    private void scheduleHeartbeat(Long memberId) {
        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(
                () -> sendHeartbeat(memberId),
                HEARTBEAT_INTERVAL_SECONDS,
                HEARTBEAT_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );
        heartbeatTasks.put(memberId, task);
    }

    /**
     * 연결된 클라이언트에 ping 이벤트를 전송한다.
     *
     * scheduleAtFixedRate의 Runnable이 RuntimeException을 던지면 이후 스케줄이 취소되므로
     * IOException 외에 모든 예외를 catch해 태스크가 중단되지 않도록 보호한다.
     */
    private void sendHeartbeat(Long memberId) {
        SseEmitter emitter = emitters.get(memberId);
        if (emitter == null) return;
        try {
            emitter.send(SseEmitter.event().name(HEARTBEAT_EVENT_NAME).data(HEARTBEAT_DATA));
            log.debug("[SSE] Heartbeat 전송 - memberId={}", memberId);
        } catch (Exception e) {
            // 전송 실패 = 연결이 끊긴 것으로 판단하고 정리
            log.warn("[SSE] Heartbeat 전송 실패, Emitter 삭제 - memberId={}", memberId, e);
            deleteById(memberId);
        }
    }
}
