package mju.capstone.ddingconnect.global.sse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseServiceImpl implements SseService {

    private static final long SSE_TIMEOUT = -1L;
    private static final String CONNECT_EVENT_NAME = "connect";
    private static final String CONNECT_MESSAGE = "connected";
    private static final String NOTIFICATION_EVENT_NAME = "notification";

    private final SseEmitterRepository sseEmitterRepository;

    @Override
    public SseEmitter subscribe(Member member) {
        log.info("[SSE] 구독 요청 - memberId={}", member.getId());
        SseEmitter emitter = sseEmitterRepository.save(member.getId(), new SseEmitter(SSE_TIMEOUT));
        try {
            emitter.send(SseEmitter.event()
                    .name(CONNECT_EVENT_NAME)
                    .data(CONNECT_MESSAGE));
            log.info("[SSE] 구독 완료 - memberId={}", member.getId());
        } catch (IOException e) {
            log.warn("[SSE] 연결 이벤트 전송 실패 - memberId={}", member.getId(), e);
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
                log.info("[SSE] 알람 전송 완료 - memberId={}, type={}", receiver.getId(), type);
            } catch (IOException e) {
                log.warn("[SSE] 알람 전송 실패, Emitter 삭제 - memberId={}", receiver.getId(), e);
                sseEmitterRepository.deleteById(receiver.getId());
            }
        });
    }
}
