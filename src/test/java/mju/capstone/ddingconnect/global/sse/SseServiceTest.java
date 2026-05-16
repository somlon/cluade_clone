package mju.capstone.ddingconnect.global.sse;

import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.member.domain.MemberRole;
import mju.capstone.ddingconnect.global.alarm.AlarmType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Optional;

import static mju.capstone.ddingconnect.global.sse.SseTestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SseServiceTest {

    @Mock
    private SseEmitterRepository sseEmitterRepository;

    @InjectMocks
    private SseServiceImpl sseService;

    private Member testMember;

    @BeforeEach
    void setUp() {
        testMember = Member.builder()
                .id(TEST_MEMBER_ID)
                .email(TEST_EMAIL)
                .nickname(TEST_NICKNAME)
                .role(MemberRole.STUDENT)
                .build();
    }

    @Test
    void subscribe_emitter_저장_및_반환() {
        SseEmitter savedEmitter = new SseEmitter();
        given(sseEmitterRepository.save(eq(TEST_MEMBER_ID), any(SseEmitter.class))).willReturn(savedEmitter);

        SseEmitter result = sseService.subscribe(testMember);

        verify(sseEmitterRepository).save(eq(TEST_MEMBER_ID), any(SseEmitter.class));
        assertThat(result).isNotNull();
    }

    @Test
    void subscribe_연결이벤트_전송_실패시_emitter_삭제() throws IOException {
        try (MockedConstruction<SseEmitter> mocked = mockConstruction(SseEmitter.class, (mock, context) ->
                doThrow(new IOException()).when(mock).send(any(SseEmitter.SseEventBuilder.class))
        )) {
            given(sseEmitterRepository.save(eq(TEST_MEMBER_ID), any(SseEmitter.class)))
                    .willAnswer(invocation -> invocation.getArgument(1));

            sseService.subscribe(testMember);

            verify(sseEmitterRepository).deleteById(TEST_MEMBER_ID);
        }
    }

    @Test
    void send_emitter_존재시_이벤트_전송() throws IOException {
        SseEmitter mockEmitter = mock(SseEmitter.class);
        given(sseEmitterRepository.findById(TEST_MEMBER_ID)).willReturn(Optional.of(mockEmitter));

        sseService.send(testMember, AlarmType.ANSWER, TEST_ALARM_CONTENT);

        verify(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void send_emitter_없으면_아무것도_안함() {
        given(sseEmitterRepository.findById(TEST_MEMBER_ID)).willReturn(Optional.empty());

        sseService.send(testMember, AlarmType.JOB, TEST_ALARM_CONTENT);

        verify(sseEmitterRepository, never()).deleteById(any());
    }

    @Test
    void send_전송_실패시_emitter_삭제() throws IOException {
        SseEmitter mockEmitter = mock(SseEmitter.class);
        doThrow(new IOException()).when(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));
        given(sseEmitterRepository.findById(TEST_MEMBER_ID)).willReturn(Optional.of(mockEmitter));

        sseService.send(testMember, AlarmType.COFFEE_CHAT, TEST_ALARM_CONTENT);

        verify(sseEmitterRepository).deleteById(TEST_MEMBER_ID);
    }
}
