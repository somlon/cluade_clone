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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final CoffeeChatRepository coffeeChatRepository;
    private final CoffeeChatAlarmRepository coffeeChatAlarmRepository;
    private final MemberRepository memberRepository;

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

        CoffeeChat coffeeChat = CoffeeChat.builder()
                .requester(member)
                .receiver(receiver)
                .status(CoffeeChatStatus.PENDING)
                .kakaoOpenChatLink(request.kakaoOpenChatLink())
                .build();

        CoffeeChat saved = coffeeChatRepository.save(coffeeChat);

        // [알람 발행] 수신자에게 "요청 도착" 알람 1건
        // content 에 요청자 학과/닉네임을 포함해 알림 화면에서 식별 가능하게 함
        String pendingContent = String.format("%s %s님이 커피챗을 요청했어요!",
                member.getDepartment(), member.getNickname());
        coffeeChatAlarmRepository.save(CoffeeChatAlarm.builder()
                .coffeeChat(saved)
                .member(receiver)
                .content(pendingContent)
                .isRead(false)
                .build());

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
            String acceptedContent =
                    "커피챗 요청이 수락되었습니다. 카카오톡 오픈채팅 링크: " + saved.getKakaoOpenChatLink();

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

        } else if (request.status() == CoffeeChatStatus.REJECTED) {
            // 거절: 요청자에게만 거절 알람 1건
            coffeeChatAlarmRepository.save(CoffeeChatAlarm.builder()
                    .coffeeChat(saved)
                    .member(saved.getRequester())
                    .content("커피챗 요청이 거절되었습니다.")
                    .isRead(false)
                    .build());
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
}
