package mju.capstone.ddingconnect.domain.coffeechat.service;

import mju.capstone.ddingconnect.domain.coffeechat.dto.request.CreateCoffeeChatRequest;
import mju.capstone.ddingconnect.domain.coffeechat.dto.request.UpdateCoffeeChatStatusRequest;
import mju.capstone.ddingconnect.domain.coffeechat.dto.response.CoffeeChatPartnerResponse;
import mju.capstone.ddingconnect.domain.coffeechat.dto.response.CoffeeChatResponse;
import mju.capstone.ddingconnect.domain.member.domain.Member;

import java.util.List;

public interface CoffeeChatService {

    /** 커피챗 요청 */
    CoffeeChatResponse create(Member member, CreateCoffeeChatRequest request);

    /** 내가 보낸 커피챗 목록 */
    List<CoffeeChatResponse> getSentList(Member member);

    /** 내가 받은 커피챗 목록 */
    List<CoffeeChatResponse> getReceivedList(Member member);

    /** 커피챗 상태 변경 (수락/거절) */
    CoffeeChatResponse updateStatus(Member member, Long coffeeChatId, UpdateCoffeeChatStatusRequest request);

    /** 커피챗 취소 (삭제) */
    void delete(Member member, Long coffeeChatId);

    /** 본인이 요청자/수신자로 참여한 수락(ACCEPTED) 상태 커피챗 수 */
    long countMyAcceptedCoffeeChats(Member member);

    /**
     * 나의 활동 페이지 — 본인이 요청자/수신자로 참여한 모든 커피챗을 합쳐 상대방 카드 리스트로 반환.
     * - findByRequesterId + findByReceiverId 결과를 합쳐 중복 제거(같은 커피챗이 양쪽에 안 나타나지만 안전 가드)
     * - 본인 아닌 쪽(상대방) 의 닉네임/학과/관심직무/기술스택을 묶어 CoffeeChatPartnerResponse 로 반환
     */
    List<CoffeeChatPartnerResponse> getMyActivities(Member member);
}
