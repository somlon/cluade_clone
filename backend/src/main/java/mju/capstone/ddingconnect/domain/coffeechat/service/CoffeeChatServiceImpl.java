package mju.capstone.ddingconnect.domain.coffeechat.service;

import lombok.RequiredArgsConstructor;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * [커피챗 서비스 구현체]
 *
 * 커피챗 매칭 흐름:
 * 1) 요청자가 카카오 오픈채팅 링크를 포함해 커피챗 요청 (status=PENDING)
 *    → 수신자에게 "요청 도착" 알람 1건 발행
 * 2) 수신자가 수락(ACCEPTED) → 양쪽(요청자/수신자)에 카카오 링크 포함 알람 2건 발행
 *    수신자가 거절(REJECTED) → 요청자에게만 거절 알람 1건 발행
 */
@Service
@RequiredArgsConstructor
public class CoffeeChatServiceImpl implements CoffeeChatService {

    // 테스트도 동일 상수를 참조하므로 package-private 노출
    static final String PENDING_CONTENT_FORMAT = "%s %s님이 커피챗을 요청했어요!";
    private static final String ACCEPTED_CONTENT_PREFIX = "커피챗 요청이 수락되었습니다. 카카오톡 오픈채팅 링크: ";
    private static final String REJECTED_CONTENT = "커피챗 요청이 거절되었습니다.";

    // 중복 신청 방지: 진행 중으로 간주해 재신청을 막는 상태 집합 (REJECTED 는 재신청 허용이라 제외)
    private static final List<CoffeeChatStatus> ACTIVE_STATUSES =
            List.of(CoffeeChatStatus.PENDING, CoffeeChatStatus.ACCEPTED);
    // 재요청 쿨다운 — 가장 최근 요청 이후 이 기간이 지나야 같은 상대에게 다시 신청 가능
    private static final Duration RE_REQUEST_COOLDOWN = Duration.ofHours(24);

    private final CoffeeChatRepository coffeeChatRepository;
    private final CoffeeChatAlarmRepository coffeeChatAlarmRepository;
    private final MemberRepository memberRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public CoffeeChatResponse create(Member member, CreateCoffeeChatRequest request) {
        Member receiver = memberRepository.findById(request.receiverId())
                .orElseThrow(() -> new MemberHandler(ErrorStatus.MEMBER_NOT_FOUND));

        // 자기 자신 거부 (정보 노출 최소화 위해 self 가 먼저)
        if (member.getId().equals(receiver.getId())) {
            throw new CoffeeChatHandler(ErrorStatus.COFFEE_CHAT_SELF_REQUEST);
        }

        // 학생 ↔ 졸업생 쌍만 허용 (UNKNOWN 양쪽 모두 거부)
        boolean studentToGraduate = member.getRole() == MemberRole.STUDENT
                && receiver.getRole() == MemberRole.GRADUATE;
        boolean graduateToStudent = member.getRole() == MemberRole.GRADUATE
                && receiver.getRole() == MemberRole.STUDENT;
        if (!studentToGraduate && !graduateToStudent) {
            throw new CoffeeChatHandler(ErrorStatus.COFFEE_CHAT_ROLE_MISMATCH);
        }

        // 중복 신청 방지 (b): requester→receiver 로 진행 중(PENDING/ACCEPTED)인 커피챗이 있으면 거부.
        // REJECTED 만 있으면 통과(재신청 허용).
        if (coffeeChatRepository.existsByRequesterIdAndReceiverIdAndStatusIn(
                member.getId(), receiver.getId(), ACTIVE_STATUSES)) {
            throw new CoffeeChatHandler(ErrorStatus.COFFEE_CHAT_ALREADY_REQUESTED);
        }

        // 재요청 쿨다운: (b)를 통과했더라도 가장 최근 요청이 24시간 이내면 거부
        // (거절 직후 빠른 반복 재신청 차단).
        LocalDateTime cooldownThreshold = LocalDateTime.now().minus(RE_REQUEST_COOLDOWN);
        if (coffeeChatRepository.existsByRequesterIdAndReceiverIdAndCreatedAtAfter(
                member.getId(), receiver.getId(), cooldownThreshold)) {
            throw new CoffeeChatHandler(ErrorStatus.COFFEE_CHAT_REQUEST_TOO_SOON);
        }

        CoffeeChat coffeeChat = CoffeeChat.builder()
                .requester(member)
                .receiver(receiver)
                .status(CoffeeChatStatus.PENDING)
                .kakaoOpenChatLink(request.kakaoOpenChatLink())
                .build();

        CoffeeChat saved = coffeeChatRepository.save(coffeeChat);

        // [알람 발행] 수신자에게 "요청 도착" 알람 1건
        // content 에 요청자 학과/닉네임을 포함해 알림 화면에서 식별 가능하게 함
        String pendingContent = String.format(PENDING_CONTENT_FORMAT,
                member.getDepartment(), member.getNickname());
        coffeeChatAlarmRepository.save(CoffeeChatAlarm.builder()
                .coffeeChat(saved)
                .member(receiver)
                .content(pendingContent)
                .isRead(false)
                .build());
        eventPublisher.publishEvent(new AlarmNotificationEvent(
                receiver, AlarmType.COFFEE_CHAT, pendingContent));

        return CoffeeChatResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CoffeeChatResponse> getSentList(Member member) {
        return coffeeChatRepository.findByRequesterId(member.getId())
                .stream().map(CoffeeChatResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CoffeeChatResponse> getReceivedList(Member member) {
        return coffeeChatRepository.findByReceiverId(member.getId())
                .stream().map(CoffeeChatResponse::from).toList();
    }

    @Override
    @Transactional
    public CoffeeChatResponse updateStatus(Member member, Long coffeeChatId, UpdateCoffeeChatStatusRequest request) {
        CoffeeChat coffeeChat = coffeeChatRepository.findById(coffeeChatId)
                .orElseThrow(() -> new CoffeeChatHandler(ErrorStatus.COFFEE_CHAT_NOT_FOUND));

        // 수신자만 상태 변경 가능
        if (!coffeeChat.getReceiver().getId().equals(member.getId())) {
            throw new CoffeeChatHandler(ErrorStatus.COFFEE_CHAT_UNAUTHORIZED);
        }

        CoffeeChat updated = CoffeeChat.builder()
                .id(coffeeChat.getId())
                .requester(coffeeChat.getRequester())
                .receiver(coffeeChat.getReceiver())
                .status(request.status())
                .kakaoOpenChatLink(coffeeChat.getKakaoOpenChatLink())
                .jobScore(coffeeChat.getJobScore())
                .ability(coffeeChat.getAbility())
                .goal(coffeeChat.getGoal())
                .build();

        CoffeeChat saved = coffeeChatRepository.save(updated);

        // [알람 발행] 상태별 분기
        if (request.status() == CoffeeChatStatus.ACCEPTED) {
            // 수락: 요청자/수신자 양쪽에 카카오 오픈채팅 링크 포함 알람 2건
            String acceptedContent = ACCEPTED_CONTENT_PREFIX + saved.getKakaoOpenChatLink();

            coffeeChatAlarmRepository.save(CoffeeChatAlarm.builder()
                    .coffeeChat(saved)
                    .member(saved.getRequester())
                    .content(acceptedContent)
                    .isRead(false)
                    .build());

            coffeeChatAlarmRepository.save(CoffeeChatAlarm.builder()
                    .coffeeChat(saved)
                    .member(saved.getReceiver())
                    .content(acceptedContent)
                    .isRead(false)
                    .build());

            eventPublisher.publishEvent(new AlarmNotificationEvent(
                    saved.getRequester(), AlarmType.COFFEE_CHAT, acceptedContent));
            eventPublisher.publishEvent(new AlarmNotificationEvent(
                    saved.getReceiver(), AlarmType.COFFEE_CHAT, acceptedContent));

        } else if (request.status() == CoffeeChatStatus.REJECTED) {
            // 거절: 요청자에게만 거절 알람 1건
            coffeeChatAlarmRepository.save(CoffeeChatAlarm.builder()
                    .coffeeChat(saved)
                    .member(saved.getRequester())
                    .content(REJECTED_CONTENT)
                    .isRead(false)
                    .build());
            eventPublisher.publishEvent(new AlarmNotificationEvent(
                    saved.getRequester(), AlarmType.COFFEE_CHAT, REJECTED_CONTENT));
        }

        return CoffeeChatResponse.from(saved);
    }

    @Override
    @Transactional
    public void delete(Member member, Long coffeeChatId) {
        CoffeeChat coffeeChat = coffeeChatRepository.findById(coffeeChatId)
                .orElseThrow(() -> new CoffeeChatHandler(ErrorStatus.COFFEE_CHAT_NOT_FOUND));

        // 요청자만 취소 가능
        if (!coffeeChat.getRequester().getId().equals(member.getId())) {
            throw new CoffeeChatHandler(ErrorStatus.COFFEE_CHAT_UNAUTHORIZED);
        }

        // CoffeeChat 을 NOT NULL FK 로 참조하는 CoffeeChatAlarm 먼저 삭제
        coffeeChatAlarmRepository.deleteByCoffeeChatId(coffeeChatId);
        coffeeChatRepository.delete(coffeeChat);
    }

    @Override
    @Transactional(readOnly = true)
    public long countMyAcceptedCoffeeChats(Member member) {
        Long memberId = member.getId();
        return coffeeChatRepository.countByRequesterIdAndStatus(memberId, CoffeeChatStatus.ACCEPTED)
                + coffeeChatRepository.countByReceiverIdAndStatus(memberId, CoffeeChatStatus.ACCEPTED);
    }
}
