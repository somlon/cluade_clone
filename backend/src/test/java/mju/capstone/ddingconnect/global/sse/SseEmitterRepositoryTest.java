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
    void saveStoresAndReturnsEmitter() {
        stubScheduler();
        SseEmitter emitter = new SseEmitter();

        SseEmitter result = repository.save(TEST_MEMBER_ID, emitter);

        assertThat(result).isEqualTo(emitter);
        assertThat(repository.findById(TEST_MEMBER_ID)).isPresent();
    }

    @Test
    void saveRegistersHeartbeatSchedule() {
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
    void saveCompletesExistingEmitterAndCancelsTaskOnDuplicate() {
        ScheduledFuture<?> firstTask = stubScheduler();
        SseEmitter firstEmitter = mock(SseEmitter.class);
        repository.save(TEST_MEMBER_ID, firstEmitter);

        SseEmitter secondEmitter = new SseEmitter();
        repository.save(TEST_MEMBER_ID, secondEmitter);

        verify(firstEmitter).complete();
        verify(firstTask).cancel(false);
    }

    @Test
    void findByIdReturnsExistingEmitter() {
        stubScheduler();
        SseEmitter emitter = new SseEmitter();
        repository.save(TEST_MEMBER_ID, emitter);

        assertThat(repository.findById(TEST_MEMBER_ID)).contains(emitter);
    }

    @Test
    void findByIdReturnsEmptyWhenAbsent() {
        assertThat(repository.findById(TEST_MEMBER_ID)).isEmpty();
    }

    @Test
    void deleteByIdRemovesEmitterAndCancelsTask() {
        ScheduledFuture<?> mockFuture = stubScheduler();
        repository.save(TEST_MEMBER_ID, new SseEmitter());

        repository.deleteById(TEST_MEMBER_ID);

        assertThat(repository.findById(TEST_MEMBER_ID)).isEmpty();
        verify(mockFuture).cancel(false);
    }

    @Test
    void deleteByIdCompletesWithoutErrorForUnknownId() {
        repository.deleteById(TEST_MEMBER_ID);
    }

    @Test
    void shutdownStopsScheduler() {
        repository.shutdown();

        verify(mockScheduler).shutdown();
    }
}
