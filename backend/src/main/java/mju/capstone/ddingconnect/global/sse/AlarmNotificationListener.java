package mju.capstone.ddingconnect.global.sse;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 알람 이벤트를 본체 트랜잭션 커밋 후 수신해 SSE 푸시 — 롤백 시 유령 알림 방지. */
@Component
@RequiredArgsConstructor
public class AlarmNotificationListener {

    private final SseService sseService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAlarmNotification(AlarmNotificationEvent event) {
        sseService.send(event.receiver(), event.type(), event.content());
    }
}
