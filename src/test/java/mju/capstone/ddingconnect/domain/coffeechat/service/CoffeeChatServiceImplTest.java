package mju.capstone.ddingconnect.domain.coffeechat.service;

import mju.capstone.ddingconnect.domain.coffeechat.domain.CoffeeChat;
import mju.capstone.ddingconnect.domain.coffeechat.domain.CoffeeChatAlarm;
import mju.capstone.ddingconnect.domain.coffeechat.domain.CoffeeChatStatus;
import mju.capstone.ddingconnect.domain.coffeechat.domain.repository.CoffeeChatAlarmRepository;
import mju.capstone.ddingconnect.domain.coffeechat.domain.repository.CoffeeChatRepository;
import mju.capstone.ddingconnect.domain.coffeechat.dto.request.CreateCoffeeChatRequest;
import mju.capstone.ddingconnect.domain.coffeechat.dto.request.UpdateCoffeeChatStatusRequest;
import mju.capstone.ddingconnect.domain.coffeechat.dto.response.CoffeeChatResponse;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.member.domain.repository.MemberRepository;
import mju.capstone.ddingconnect.global.response.exception.handler.CoffeeChatHandler;
import mju.capstone.ddingconnect.global.response.exception.handler.MemberHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CoffeeChatServiceImpl 단위 테스트")
class CoffeeChatServiceImplTest {

    @Mock CoffeeChatRepository coffeeChatRepository;
    @Mock CoffeeChatAlarmRepository coffeeChatAlarmRepository;
    @Mock MemberRepository memberRepository;
    @InjectMocks CoffeeChatServiceImpl coffeeChatService;

    private Member requester;
    private Member receiver;
    private Member other;
    private CoffeeChat coffeeChat;

    @BeforeEach
    void setUp() {
        requester = Member.builder().id(1L).email("req@mju.ac.kr").nickname("요청자").build();
        receiver = Member.builder().id(2L).email("rec@mju.ac.kr").nickname("수신자").build();
        other = Member.builder().id(3L).email("o@mju.ac.kr").nickname("타인").build();
        coffeeChat = CoffeeChat.builder().id(10L).requester(requester).receiver(receiver)
                .status(CoffeeChatStatus.PENDING)
                .kakaoOpenChatLink("https://open.kakao.com/test").build();
    }

    @Test
    @DisplayName("create - 커피챗을 정상 요청하고 수신자에게 알람 1건 발행한다")
    void create_정상요청_알람발행() {
        CreateCoffeeChatRequest req = new CreateCoffeeChatRequest(receiver.getId(), "https://link");
        when(memberRepository.findById(receiver.getId())).thenReturn(Optional.of(receiver));
        when(coffeeChatRepository.save(any(CoffeeChat.class))).thenReturn(coffeeChat);

        CoffeeChatResponse response = coffeeChatService.create(requester, req);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo(CoffeeChatStatus.PENDING);

        // 알람 1건 발행 검증 (수신자에게)
        ArgumentCaptor<CoffeeChatAlarm> captor = ArgumentCaptor.forClass(CoffeeChatAlarm.class);
        verify(coffeeChatAlarmRepository, times(1)).save(captor.capture());
        CoffeeChatAlarm alarm = captor.getValue();
        assertThat(alarm.getMember().getId()).isEqualTo(receiver.getId());
        assertThat(alarm.getIsRead()).isFalse();
        assertThat(alarm.getContent()).contains("도착");
    }

    @Test
    @DisplayName("create - 수신자가 존재하지 않으면 MEMBER_NOT_FOUND 예외")
    void create_수신자없음_예외() {
        CreateCoffeeChatRequest req = new CreateCoffeeChatRequest(999L, "https://link");
        when(memberRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> coffeeChatService.create(requester, req))
                .isInstanceOf(MemberHandler.class);
        verify(coffeeChatAlarmRepository, never()).save(any());
    }

    @Test
    @DisplayName("getSentList - 보낸 커피챗 목록을 반환한다")
    void getSentList_정상반환() {
        when(coffeeChatRepository.findByRequesterId(requester.getId())).thenReturn(List.of(coffeeChat));

        List<CoffeeChatResponse> result = coffeeChatService.getSentList(requester);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).requesterId()).isEqualTo(requester.getId());
    }

    @Test
    @DisplayName("getReceivedList - 받은 커피챗 목록을 반환한다")
    void getReceivedList_정상반환() {
        when(coffeeChatRepository.findByReceiverId(receiver.getId())).thenReturn(List.of(coffeeChat));

        List<CoffeeChatResponse> result = coffeeChatService.getReceivedList(receiver);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).receiverId()).isEqualTo(receiver.getId());
    }

    @Test
    @DisplayName("updateStatus - 수락 시 요청자/수신자 양쪽에 카카오 링크 포함 알람 2건을 발행한다")
    void updateStatus_수락_알람2건() {
        UpdateCoffeeChatStatusRequest req = new UpdateCoffeeChatStatusRequest(CoffeeChatStatus.ACCEPTED);
        when(coffeeChatRepository.findById(10L)).thenReturn(Optional.of(coffeeChat));
        when(coffeeChatRepository.save(any(CoffeeChat.class))).thenAnswer(inv -> inv.getArgument(0));

        CoffeeChatResponse response = coffeeChatService.updateStatus(receiver, 10L, req);

        assertThat(response.status()).isEqualTo(CoffeeChatStatus.ACCEPTED);

        // 알람 2건 발행 검증 (요청자 + 수신자)
        ArgumentCaptor<CoffeeChatAlarm> captor = ArgumentCaptor.forClass(CoffeeChatAlarm.class);
        verify(coffeeChatAlarmRepository, times(2)).save(captor.capture());
        List<CoffeeChatAlarm> alarms = captor.getAllValues();

        assertThat(alarms).extracting(a -> a.getMember().getId())
                .containsExactlyInAnyOrder(requester.getId(), receiver.getId());
        assertThat(alarms).allSatisfy(a -> {
            assertThat(a.getContent()).contains("수락");
            assertThat(a.getContent()).contains(coffeeChat.getKakaoOpenChatLink());
            assertThat(a.getIsRead()).isFalse();
        });
    }

    @Test
    @DisplayName("updateStatus - 거절 시 요청자에게만 알람 1건을 발행한다")
    void updateStatus_거절_알람1건() {
        UpdateCoffeeChatStatusRequest req = new UpdateCoffeeChatStatusRequest(CoffeeChatStatus.REJECTED);
        when(coffeeChatRepository.findById(10L)).thenReturn(Optional.of(coffeeChat));
        when(coffeeChatRepository.save(any(CoffeeChat.class))).thenAnswer(inv -> inv.getArgument(0));

        coffeeChatService.updateStatus(receiver, 10L, req);

        ArgumentCaptor<CoffeeChatAlarm> captor = ArgumentCaptor.forClass(CoffeeChatAlarm.class);
        verify(coffeeChatAlarmRepository, times(1)).save(captor.capture());
        CoffeeChatAlarm alarm = captor.getValue();
        assertThat(alarm.getMember().getId()).isEqualTo(requester.getId());
        assertThat(alarm.getContent()).contains("거절");
    }

    @Test
    @DisplayName("updateStatus - 수신자가 아니면 UNAUTHORIZED 예외, 알람 미발행")
    void updateStatus_권한없음_예외() {
        UpdateCoffeeChatStatusRequest req = new UpdateCoffeeChatStatusRequest(CoffeeChatStatus.ACCEPTED);
        when(coffeeChatRepository.findById(10L)).thenReturn(Optional.of(coffeeChat));

        assertThatThrownBy(() -> coffeeChatService.updateStatus(other, 10L, req))
                .isInstanceOf(CoffeeChatHandler.class);
        verify(coffeeChatAlarmRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateStatus - 존재하지 않으면 NOT_FOUND 예외")
    void updateStatus_없음_예외() {
        UpdateCoffeeChatStatusRequest req = new UpdateCoffeeChatStatusRequest(CoffeeChatStatus.ACCEPTED);
        when(coffeeChatRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> coffeeChatService.updateStatus(receiver, 999L, req))
                .isInstanceOf(CoffeeChatHandler.class);
        verify(coffeeChatAlarmRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete - 요청자가 정상 취소하면 CoffeeChatAlarm 먼저 삭제 후 CoffeeChat 삭제")
    void delete_정상취소() {
        when(coffeeChatRepository.findById(10L)).thenReturn(Optional.of(coffeeChat));

        coffeeChatService.delete(requester, 10L);

        InOrder inOrder = inOrder(coffeeChatAlarmRepository, coffeeChatRepository);
        inOrder.verify(coffeeChatAlarmRepository).deleteByCoffeeChatId(10L);
        inOrder.verify(coffeeChatRepository).delete(coffeeChat);
    }

    @Test
    @DisplayName("delete - 요청자가 아니면 UNAUTHORIZED 예외, 자식/본체 모두 미삭제")
    void delete_권한없음_예외() {
        when(coffeeChatRepository.findById(10L)).thenReturn(Optional.of(coffeeChat));

        assertThatThrownBy(() -> coffeeChatService.delete(receiver, 10L))
                .isInstanceOf(CoffeeChatHandler.class);
        verify(coffeeChatAlarmRepository, never()).deleteByCoffeeChatId(any());
        verify(coffeeChatRepository, never()).delete(any());
    }

    @Test
    @DisplayName("delete - 존재하지 않으면 NOT_FOUND 예외")
    void delete_없음_예외() {
        when(coffeeChatRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> coffeeChatService.delete(requester, 999L))
                .isInstanceOf(CoffeeChatHandler.class);
    }
}
