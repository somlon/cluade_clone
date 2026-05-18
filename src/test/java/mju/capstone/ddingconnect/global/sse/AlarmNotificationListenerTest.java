package mju.capstone.ddingconnect.global.sse;

import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.global.alarm.AlarmType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static mju.capstone.ddingconnect.global.sse.SseTestConstants.TEST_ALARM_CONTENT;
import static mju.capstone.ddingconnect.global.sse.SseTestConstants.TEST_MEMBER_ID;
import static mju.capstone.ddingconnect.global.sse.SseTestConstants.TEST_NICKNAME;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlarmNotificationListener 단위 테스트")
class AlarmNotificationListenerTest {

    @Mock SseService sseService;
    @InjectMocks AlarmNotificationListener listener;

    @Test
    @DisplayName("onAlarmNotification - 이벤트 수신 시 SseService.send 로 위임한다")
    void onAlarmNotificationDelegatesToSend() {
        Member receiver = Member.builder().id(TEST_MEMBER_ID).nickname(TEST_NICKNAME).build();
        AlarmNotificationEvent event =
                new AlarmNotificationEvent(receiver, AlarmType.ANSWER, TEST_ALARM_CONTENT);

        listener.onAlarmNotification(event);

        verify(sseService).send(receiver, AlarmType.ANSWER, TEST_ALARM_CONTENT);
    }
}
