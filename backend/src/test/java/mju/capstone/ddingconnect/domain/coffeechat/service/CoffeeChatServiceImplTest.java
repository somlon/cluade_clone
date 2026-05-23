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
import mju.capstone.ddingconnect.domain.member.domain.MemberRole;
import mju.capstone.ddingconnect.domain.member.domain.repository.MemberRepository;
import mju.capstone.ddingconnect.global.response.code.status.ErrorStatus;
import mju.capstone.ddingconnect.global.response.exception.handler.CoffeeChatHandler;
import mju.capstone.ddingconnect.global.response.exception.handler.MemberHandler;
import mju.capstone.ddingconnect.global.sse.AlarmNotificationEvent;
import mju.capstone.ddingconnect.global.alarm.AlarmType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

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
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock mju.capstone.ddingconnect.domain.interested_job.domain.repository.TargetJobRepository targetJobRepository;
    @Mock mju.capstone.ddingconnect.domain.techstack.domain.repository.TechStackRepository techStackRepository;
    @InjectMocks CoffeeChatServiceImpl coffeeChatService;

    private Member studentRequester;
    private Member graduateReceiver;
    private Member other;
    private CoffeeChat coffeeChat;

    @BeforeEach
    void setUp() {
        studentRequester = Member.builder().id(1L).email("req@mju.ac.kr").nickname("김후배")
                .department("응용소프트웨어학과").role(MemberRole.STUDENT).build();
        graduateReceiver = Member.builder().id(2L).email("rec@mju.ac.kr").nickname("이선배")
                .department("응용소프트웨어학과").role(MemberRole.GRADUATE).build();
        other = Member.builder().id(3L).email("o@mju.ac.kr").nickname("타인").role(MemberRole.GRADUATE).build();
        coffeeChat = CoffeeChat.builder().id(10L).requester(studentRequester).receiver(graduateReceiver)
                .status(CoffeeChatStatus.PENDING)
                .kakaoOpenChatLink("https://open.kakao.com/test").build();
    }

    @Test
    @DisplayName("create - 학생→졸업생 요청 시 수신자(요청 도착)·요청자(신청 접수) 양쪽에 알람 1건씩 발행")
    void createPublishesAlarmWithDynamicContent() {
        CreateCoffeeChatRequest req = new CreateCoffeeChatRequest(graduateReceiver.getId(), "https://link");
        when(memberRepository.findById(graduateReceiver.getId())).thenReturn(Optional.of(graduateReceiver));
        when(coffeeChatRepository.save(any(CoffeeChat.class))).thenReturn(coffeeChat);

        CoffeeChatResponse response = coffeeChatService.create(studentRequester, req);

        String expectedReceiverContent = String.format(CoffeeChatServiceImpl.PENDING_CONTENT_FORMAT,
                studentRequester.getDepartment(), studentRequester.getNickname());
        String expectedRequesterContent = String.format(CoffeeChatServiceImpl.REQUEST_SENT_CONTENT_FORMAT,
                graduateReceiver.getDepartment(), graduateReceiver.getNickname());

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo(CoffeeChatStatus.PENDING);

        // 수신자에게 "요청 도착", 요청자에게 "신청 접수" 알람 각 1건 (총 2건)
        ArgumentCaptor<CoffeeChatAlarm> captor = ArgumentCaptor.forClass(CoffeeChatAlarm.class);
        verify(coffeeChatAlarmRepository, times(2)).save(captor.capture());
        List<CoffeeChatAlarm> alarms = captor.getAllValues();
        assertThat(alarms).allSatisfy(a -> assertThat(a.getIsRead()).isFalse());

        CoffeeChatAlarm receiverAlarm = alarms.stream()
                .filter(a -> a.getMember().getId().equals(graduateReceiver.getId()))
                .findFirst().orElseThrow();
        assertThat(receiverAlarm.getContent()).isEqualTo(expectedReceiverContent);

        CoffeeChatAlarm requesterAlarm = alarms.stream()
                .filter(a -> a.getMember().getId().equals(studentRequester.getId()))
                .findFirst().orElseThrow();
        assertThat(requesterAlarm.getContent()).isEqualTo(expectedRequesterContent);

        // 커밋 후 SSE 푸시용 이벤트도 수신자·요청자 양쪽 대상으로 2건 발행된다
        ArgumentCaptor<AlarmNotificationEvent> eventCaptor = ArgumentCaptor.forClass(AlarmNotificationEvent.class);
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues()).extracting(e -> e.receiver().getId())
                .containsExactlyInAnyOrder(studentRequester.getId(), graduateReceiver.getId());
        assertThat(eventCaptor.getAllValues()).allSatisfy(e ->
                assertThat(e.type()).isEqualTo(AlarmType.COFFEE_CHAT));
    }

    @Test
    @DisplayName("create - 졸업생→학생 요청도 정상 통과")
    void createAllowsGraduateToStudent() {
        Member studentReceiver = Member.builder().id(5L).nickname("재학생").department("학과")
                .role(MemberRole.STUDENT).build();
        Member graduateRequester = Member.builder().id(6L).nickname("졸업생").department("학과")
                .role(MemberRole.GRADUATE).build();
        CreateCoffeeChatRequest req = new CreateCoffeeChatRequest(studentReceiver.getId(), "https://link");
        when(memberRepository.findById(studentReceiver.getId())).thenReturn(Optional.of(studentReceiver));
        when(coffeeChatRepository.save(any(CoffeeChat.class))).thenReturn(coffeeChat);

        coffeeChatService.create(graduateRequester, req);

        // 수신자(요청 도착)·요청자(신청 접수) 양쪽에 알람 1건씩
        verify(coffeeChatAlarmRepository, times(2)).save(any(CoffeeChatAlarm.class));
    }

    @Test
    @DisplayName("create - 자기 자신에게 요청 시 COFFEE_CHAT_SELF_REQUEST 예외")
    void createThrowsOnSelfRequest() {
        CreateCoffeeChatRequest req = new CreateCoffeeChatRequest(studentRequester.getId(), "https://link");
        when(memberRepository.findById(studentRequester.getId())).thenReturn(Optional.of(studentRequester));

        assertThatThrownBy(() -> coffeeChatService.create(studentRequester, req))
                .isInstanceOf(CoffeeChatHandler.class);
        verify(coffeeChatRepository, never()).save(any());
        verify(coffeeChatAlarmRepository, never()).save(any());
    }

    @Test
    @DisplayName("create - STUDENT 가 STUDENT 에게 요청 시 COFFEE_CHAT_ROLE_MISMATCH 예외")
    void createThrowsOnStudentToStudent() {
        Member otherStudent = Member.builder().id(8L).role(MemberRole.STUDENT).build();
        CreateCoffeeChatRequest req = new CreateCoffeeChatRequest(otherStudent.getId(), "https://link");
        when(memberRepository.findById(otherStudent.getId())).thenReturn(Optional.of(otherStudent));

        assertThatThrownBy(() -> coffeeChatService.create(studentRequester, req))
                .isInstanceOf(CoffeeChatHandler.class);
        verify(coffeeChatRepository, never()).save(any());
    }

    @Test
    @DisplayName("create - GRADUATE 가 GRADUATE 에게 요청 시 COFFEE_CHAT_ROLE_MISMATCH 예외")
    void createThrowsOnGraduateToGraduate() {
        Member otherGraduate = Member.builder().id(9L).role(MemberRole.GRADUATE).build();
        CreateCoffeeChatRequest req = new CreateCoffeeChatRequest(otherGraduate.getId(), "https://link");
        when(memberRepository.findById(otherGraduate.getId())).thenReturn(Optional.of(otherGraduate));

        assertThatThrownBy(() -> coffeeChatService.create(graduateReceiver, req))
                .isInstanceOf(CoffeeChatHandler.class);
    }

    @Test
    @DisplayName("create - UNKNOWN 이 요청자 시 COFFEE_CHAT_ROLE_MISMATCH 예외")
    void createThrowsWhenRequesterIsUnknown() {
        Member unknown = Member.builder().id(11L).role(MemberRole.UNKNOWN).build();
        CreateCoffeeChatRequest req = new CreateCoffeeChatRequest(graduateReceiver.getId(), "https://link");
        when(memberRepository.findById(graduateReceiver.getId())).thenReturn(Optional.of(graduateReceiver));

        assertThatThrownBy(() -> coffeeChatService.create(unknown, req))
                .isInstanceOf(CoffeeChatHandler.class);
    }

    @Test
    @DisplayName("create - UNKNOWN 이 수신자 시 COFFEE_CHAT_ROLE_MISMATCH 예외")
    void createThrowsWhenReceiverIsUnknown() {
        Member unknownReceiver = Member.builder().id(12L).role(MemberRole.UNKNOWN).build();
        CreateCoffeeChatRequest req = new CreateCoffeeChatRequest(unknownReceiver.getId(), "https://link");
        when(memberRepository.findById(unknownReceiver.getId())).thenReturn(Optional.of(unknownReceiver));

        assertThatThrownBy(() -> coffeeChatService.create(studentRequester, req))
                .isInstanceOf(CoffeeChatHandler.class);
    }

    @Test
    @DisplayName("create - 수신자가 존재하지 않으면 MEMBER_NOT_FOUND 예외")
    void createThrowsWhenReceiverNotFound() {
        CreateCoffeeChatRequest req = new CreateCoffeeChatRequest(999L, "https://link");
        when(memberRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> coffeeChatService.create(studentRequester, req))
                .isInstanceOf(MemberHandler.class);
        verify(coffeeChatAlarmRepository, never()).save(any());
    }

    @Test
    @DisplayName("create - 진행 중(PENDING/ACCEPTED) 커피챗이 있으면 COFFEE_CHAT_ALREADY_REQUESTED 예외, 본체 미저장")
    void createThrowsOnDuplicateInProgressRequest() {
        CreateCoffeeChatRequest req = new CreateCoffeeChatRequest(graduateReceiver.getId(), "https://link");
        when(memberRepository.findById(graduateReceiver.getId())).thenReturn(Optional.of(graduateReceiver));
        when(coffeeChatRepository.existsByRequesterIdAndReceiverIdAndStatusIn(any(), any(), any()))
                .thenReturn(true);

        assertThatThrownBy(() -> coffeeChatService.create(studentRequester, req))
                .isInstanceOf(CoffeeChatHandler.class)
                .hasFieldOrPropertyWithValue("code", ErrorStatus.COFFEE_CHAT_ALREADY_REQUESTED);
        verify(coffeeChatRepository, never()).save(any());
        verify(coffeeChatAlarmRepository, never()).save(any());
    }

    @Test
    @DisplayName("create - 가장 최근 요청이 24시간 이내면 COFFEE_CHAT_REQUEST_TOO_SOON 예외, 본체 미저장")
    void createThrowsOnCooldown() {
        CreateCoffeeChatRequest req = new CreateCoffeeChatRequest(graduateReceiver.getId(), "https://link");
        when(memberRepository.findById(graduateReceiver.getId())).thenReturn(Optional.of(graduateReceiver));
        // 진행 중 요청은 없으나(규칙 b 통과), 쿨다운 기간 내 최근 요청이 존재
        when(coffeeChatRepository.existsByRequesterIdAndReceiverIdAndCreatedAtAfter(any(), any(), any()))
                .thenReturn(true);

        assertThatThrownBy(() -> coffeeChatService.create(studentRequester, req))
                .isInstanceOf(CoffeeChatHandler.class)
                .hasFieldOrPropertyWithValue("code", ErrorStatus.COFFEE_CHAT_REQUEST_TOO_SOON);
        verify(coffeeChatRepository, never()).save(any());
        verify(coffeeChatAlarmRepository, never()).save(any());
    }

    @Test
    @DisplayName("getSentList - 보낸 커피챗 목록을 반환한다")
    void getSentListReturnsList() {
        when(coffeeChatRepository.findByRequesterId(studentRequester.getId())).thenReturn(List.of(coffeeChat));

        List<CoffeeChatResponse> result = coffeeChatService.getSentList(studentRequester);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).requesterId()).isEqualTo(studentRequester.getId());
    }

    @Test
    @DisplayName("getReceivedList - 받은 커피챗 목록을 반환한다")
    void getReceivedListReturnsList() {
        when(coffeeChatRepository.findByReceiverId(graduateReceiver.getId())).thenReturn(List.of(coffeeChat));

        List<CoffeeChatResponse> result = coffeeChatService.getReceivedList(graduateReceiver);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).receiverId()).isEqualTo(graduateReceiver.getId());
    }

    @Test
    @DisplayName("updateStatus - 수락 시 요청자/수신자 양쪽에 카카오 링크 포함 알람 2건을 발행한다")
    void updateStatusAcceptPublishesTwoAlarms() {
        UpdateCoffeeChatStatusRequest req = new UpdateCoffeeChatStatusRequest(CoffeeChatStatus.ACCEPTED);
        when(coffeeChatRepository.findById(10L)).thenReturn(Optional.of(coffeeChat));
        when(coffeeChatRepository.save(any(CoffeeChat.class))).thenAnswer(inv -> inv.getArgument(0));

        CoffeeChatResponse response = coffeeChatService.updateStatus(graduateReceiver, 10L, req);

        assertThat(response.status()).isEqualTo(CoffeeChatStatus.ACCEPTED);

        ArgumentCaptor<CoffeeChatAlarm> captor = ArgumentCaptor.forClass(CoffeeChatAlarm.class);
        verify(coffeeChatAlarmRepository, times(2)).save(captor.capture());
        List<CoffeeChatAlarm> alarms = captor.getAllValues();

        assertThat(alarms).extracting(a -> a.getMember().getId())
                .containsExactlyInAnyOrder(studentRequester.getId(), graduateReceiver.getId());
        assertThat(alarms).allSatisfy(a -> {
            assertThat(a.getContent()).contains("수락");
            assertThat(a.getContent()).contains(coffeeChat.getKakaoOpenChatLink());
            assertThat(a.getIsRead()).isFalse();
        });

        // 커밋 후 SSE 푸시용 이벤트도 요청자/수신자 양쪽 대상으로 2건 발행된다
        ArgumentCaptor<AlarmNotificationEvent> eventCaptor = ArgumentCaptor.forClass(AlarmNotificationEvent.class);
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues()).extracting(e -> e.receiver().getId())
                .containsExactlyInAnyOrder(studentRequester.getId(), graduateReceiver.getId());
        assertThat(eventCaptor.getAllValues()).allSatisfy(e -> {
            assertThat(e.type()).isEqualTo(AlarmType.COFFEE_CHAT);
            assertThat(e.content()).contains("수락");
        });
    }

    @Test
    @DisplayName("updateStatus - 거절 시 요청자에게만 알람 1건을 발행한다")
    void updateStatusRejectPublishesOneAlarm() {
        UpdateCoffeeChatStatusRequest req = new UpdateCoffeeChatStatusRequest(CoffeeChatStatus.REJECTED);
        when(coffeeChatRepository.findById(10L)).thenReturn(Optional.of(coffeeChat));
        when(coffeeChatRepository.save(any(CoffeeChat.class))).thenAnswer(inv -> inv.getArgument(0));

        coffeeChatService.updateStatus(graduateReceiver, 10L, req);

        ArgumentCaptor<CoffeeChatAlarm> captor = ArgumentCaptor.forClass(CoffeeChatAlarm.class);
        verify(coffeeChatAlarmRepository, times(1)).save(captor.capture());
        CoffeeChatAlarm alarm = captor.getValue();
        assertThat(alarm.getMember().getId()).isEqualTo(studentRequester.getId());
        assertThat(alarm.getContent()).contains("거절");

        // 커밋 후 SSE 푸시용 이벤트가 요청자 대상으로 1건 발행된다
        ArgumentCaptor<AlarmNotificationEvent> eventCaptor = ArgumentCaptor.forClass(AlarmNotificationEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
        AlarmNotificationEvent event = eventCaptor.getValue();
        assertThat(event.receiver().getId()).isEqualTo(studentRequester.getId());
        assertThat(event.type()).isEqualTo(AlarmType.COFFEE_CHAT);
        assertThat(event.content()).contains("거절");
    }

    @Test
    @DisplayName("updateStatus - 수신자가 아니면 UNAUTHORIZED 예외, 알람 미발행")
    void updateStatusThrowsWhenUnauthorized() {
        UpdateCoffeeChatStatusRequest req = new UpdateCoffeeChatStatusRequest(CoffeeChatStatus.ACCEPTED);
        when(coffeeChatRepository.findById(10L)).thenReturn(Optional.of(coffeeChat));

        assertThatThrownBy(() -> coffeeChatService.updateStatus(other, 10L, req))
                .isInstanceOf(CoffeeChatHandler.class);
        verify(coffeeChatAlarmRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateStatus - 존재하지 않으면 NOT_FOUND 예외")
    void updateStatusThrowsWhenNotFound() {
        UpdateCoffeeChatStatusRequest req = new UpdateCoffeeChatStatusRequest(CoffeeChatStatus.ACCEPTED);
        when(coffeeChatRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> coffeeChatService.updateStatus(graduateReceiver, 999L, req))
                .isInstanceOf(CoffeeChatHandler.class);
        verify(coffeeChatAlarmRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete - 요청자가 정상 취소하면 CoffeeChatAlarm 먼저 삭제 후 CoffeeChat 삭제")
    void deleteCancelsSuccessfully() {
        when(coffeeChatRepository.findById(10L)).thenReturn(Optional.of(coffeeChat));

        coffeeChatService.delete(studentRequester, 10L);

        InOrder inOrder = inOrder(coffeeChatAlarmRepository, coffeeChatRepository);
        inOrder.verify(coffeeChatAlarmRepository).deleteByCoffeeChatId(10L);
        inOrder.verify(coffeeChatRepository).delete(coffeeChat);
    }

    @Test
    @DisplayName("delete - 요청자가 아니면 UNAUTHORIZED 예외, 자식/본체 모두 미삭제")
    void deleteThrowsWhenUnauthorized() {
        when(coffeeChatRepository.findById(10L)).thenReturn(Optional.of(coffeeChat));

        assertThatThrownBy(() -> coffeeChatService.delete(graduateReceiver, 10L))
                .isInstanceOf(CoffeeChatHandler.class);
        verify(coffeeChatAlarmRepository, never()).deleteByCoffeeChatId(any());
        verify(coffeeChatRepository, never()).delete(any());
    }

    @Test
    @DisplayName("delete - 존재하지 않으면 NOT_FOUND 예외")
    void deleteThrowsWhenNotFound() {
        when(coffeeChatRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> coffeeChatService.delete(studentRequester, 999L))
                .isInstanceOf(CoffeeChatHandler.class);
    }

    @Test
    @DisplayName("countMyAcceptedCoffeeChats - 요청자/수신자 ACCEPTED 커피챗 수를 합산한다")
    void countMyAcceptedCoffeeChatsSumsBothSides() {
        Long memberId = studentRequester.getId();
        when(coffeeChatRepository.countByRequesterIdAndStatus(memberId, CoffeeChatStatus.ACCEPTED)).thenReturn(2L);
        when(coffeeChatRepository.countByReceiverIdAndStatus(memberId, CoffeeChatStatus.ACCEPTED)).thenReturn(3L);

        long result = coffeeChatService.countMyAcceptedCoffeeChats(studentRequester);

        assertThat(result).isEqualTo(5L);
    }

    @Test
    @DisplayName("getMyActivities - 요청자/수신자 양방향 합치고 중복 제거 + 상대방 정보로 카드 조립")
    void getMyActivitiesAssemblesPartnerCards() {
        Long memberId = studentRequester.getId();

        // sent: studentRequester → graduateReceiver
        CoffeeChat sent = CoffeeChat.builder().id(101L)
                .requester(studentRequester).receiver(graduateReceiver)
                .status(mju.capstone.ddingconnect.domain.coffeechat.domain.CoffeeChatStatus.PENDING)
                .build();
        // received: other → studentRequester
        CoffeeChat received = CoffeeChat.builder().id(102L)
                .requester(other).receiver(studentRequester)
                .status(mju.capstone.ddingconnect.domain.coffeechat.domain.CoffeeChatStatus.ACCEPTED)
                .build();

        when(coffeeChatRepository.findByRequesterId(memberId)).thenReturn(List.of(sent));
        when(coffeeChatRepository.findByReceiverId(memberId)).thenReturn(List.of(received));
        // 상대방 부가 정보 (관심직무·기술스택)
        when(targetJobRepository.findByMemberId(graduateReceiver.getId())).thenReturn(List.of(
                mju.capstone.ddingconnect.domain.interested_job.domain.TargetJob.builder()
                        .interestedJob(mju.capstone.ddingconnect.domain.interested_job.domain.TargetJobCategory.BACKEND)
                        .build()));
        when(techStackRepository.findByMemberId(graduateReceiver.getId())).thenReturn(List.of(
                mju.capstone.ddingconnect.domain.techstack.domain.TechStack.builder()
                        .name(mju.capstone.ddingconnect.domain.techstack.domain.TechStackName.JAVA).build()));
        when(targetJobRepository.findByMemberId(other.getId())).thenReturn(List.of());
        when(techStackRepository.findByMemberId(other.getId())).thenReturn(List.of());

        var result = coffeeChatService.getMyActivities(studentRequester);

        assertThat(result).hasSize(2);
        // 첫 카드: 요청자=본인, 상대=graduateReceiver
        assertThat(result.get(0).coffeeChatId()).isEqualTo(101L);
        assertThat(result.get(0).partnerId()).isEqualTo(graduateReceiver.getId());
        assertThat(result.get(0).partnerNickname()).isEqualTo("이선배");
        assertThat(result.get(0).partnerTechStacks())
                .containsExactly(mju.capstone.ddingconnect.domain.techstack.domain.TechStackName.JAVA);
        // 두 번째 카드: 수신자=본인, 상대=other
        assertThat(result.get(1).coffeeChatId()).isEqualTo(102L);
        assertThat(result.get(1).partnerId()).isEqualTo(other.getId());
        assertThat(result.get(1).partnerJobs()).isEmpty();
    }

    @Test
    @DisplayName("getMyActivities - sent/received 에 같은 커피챗이 중복 들어오면 id 기준 한 번만 응답")
    void getMyActivitiesDedupesByCoffeeChatId() {
        Long memberId = studentRequester.getId();
        CoffeeChat dup = CoffeeChat.builder().id(200L)
                .requester(studentRequester).receiver(graduateReceiver)
                .status(mju.capstone.ddingconnect.domain.coffeechat.domain.CoffeeChatStatus.PENDING)
                .build();

        when(coffeeChatRepository.findByRequesterId(memberId)).thenReturn(List.of(dup));
        when(coffeeChatRepository.findByReceiverId(memberId)).thenReturn(List.of(dup));
        when(targetJobRepository.findByMemberId(graduateReceiver.getId())).thenReturn(List.of());
        when(techStackRepository.findByMemberId(graduateReceiver.getId())).thenReturn(List.of());

        var result = coffeeChatService.getMyActivities(studentRequester);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).coffeeChatId()).isEqualTo(200L);
    }
}
