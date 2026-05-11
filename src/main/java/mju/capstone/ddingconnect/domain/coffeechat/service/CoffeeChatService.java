package mju.capstone.ddingconnect.domain.coffeechat.service;

import mju.capstone.ddingconnect.domain.coffeechat.dto.request.CreateCoffeeChatRequest;
import mju.capstone.ddingconnect.domain.coffeechat.dto.request.UpdateCoffeeChatStatusRequest;
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
}
