package mju.capstone.ddingconnect.global.sse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static mju.capstone.ddingconnect.global.sse.SseTestConstants.HEARTBEAT_INTERVAL_SECONDS;
import static mju.capstone.ddingconnect.global.sse.SseTestConstants.TEST_MEMBER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class SseEmitterRepositoryTest {

    private ScheduledExecutorService mockScheduler;
    private SseEmitterRepository repository;

    @BeforeEach
    void setUp() {
        mockScheduler = mock(ScheduledExecutorService.class);
        repository = new SseEmitterRepository(mockScheduler);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ScheduledFuture<?> stubScheduler() {
        ScheduledFuture mockFuture = mock(ScheduledFuture.class);
        given(mockScheduler.scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class)))
                .willReturn(mockFuture);
        return mockFuture;
    }

    @Test
    void save_emitter_저장_및_반환() {
        stubScheduler();
        SseEmitter emitter = new SseEmitter();

        SseEmitter result = repository.save(TEST_MEMBER_ID, emitter);

        assertThat(result).isEqualTo(emitter);
        assertThat(repository.findById(TEST_MEMBER_ID)).isPresent();
    }

    @Test
    void save_heartbeat_스케줄_등록() {
        ScheduledFuture<?> mockFuture = stubScheduler();
        SseEmitter emitter = new SseEmitter();

        repository.save(TEST_MEMBER_ID, emitter);

        verify(mockScheduler).scheduleAtFixedRate(
                any(Runnable.class),
                eq(HEARTBEAT_INTERVAL_SECONDS),
                eq(HEARTBEAT_INTERVAL_SECONDS),
                eq(TimeUnit.SECONDS));
    }

    @Test
    void save_중복구독시_기존_emitter_complete_및_task_취소() {
        ScheduledFuture<?> firstTask = stubScheduler();
        SseEmitter firstEmitter = mock(SseEmitter.class);
        repository.save(TEST_MEMBER_ID, firstEmitter);

        SseEmitter secondEmitter = new SseEmitter();
        repository.save(TEST_MEMBER_ID, secondEmitter);

        verify(firstEmitter).complete();
        verify(firstTask).cancel(false);
    }

    @Test
    void findById_존재하는_emitter_반환() {
        stubScheduler();
        SseEmitter emitter = new SseEmitter();
        repository.save(TEST_MEMBER_ID, emitter);

        assertThat(repository.findById(TEST_MEMBER_ID)).contains(emitter);
    }

    @Test
    void findById_없으면_empty_반환() {
        assertThat(repository.findById(TEST_MEMBER_ID)).isEmpty();
    }

    @Test
    void deleteById_emitter_제거_및_task_취소() {
        ScheduledFuture<?> mockFuture = stubScheduler();
        repository.save(TEST_MEMBER_ID, new SseEmitter());

        repository.deleteById(TEST_MEMBER_ID);

        assertThat(repository.findById(TEST_MEMBER_ID)).isEmpty();
        verify(mockFuture).cancel(false);
    }

    @Test
    void deleteById_없는_id는_예외_없이_종료() {
        repository.deleteById(TEST_MEMBER_ID);
    }

    @Test
    void shutdown_scheduler_종료() {
        repository.shutdown();

        verify(mockScheduler).shutdown();
    }
}
