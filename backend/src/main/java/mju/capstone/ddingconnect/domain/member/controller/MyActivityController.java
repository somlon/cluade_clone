package mju.capstone.ddingconnect.domain.member.controller;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import mju.capstone.ddingconnect.domain.coffeechat.dto.response.CoffeeChatPartnerResponse;
import mju.capstone.ddingconnect.domain.coffeechat.service.CoffeeChatService;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.global.auth.annotation.LoginMember;
import mju.capstone.ddingconnect.global.response.exception.handler.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * [나의 활동 페이지 컨트롤러]
 * 마이페이지 활동 통계 카드 클릭 시 진입하는 '나의 활동' 페이지의 도메인별 본인 스코프 목록 조회.
 *
 * - 커피챗 → 본 컨트롤러 (`/me/activity/coffeechats`)
 * - 로드맵 → `GET /api/v1/roadmaps` 재사용 (이미 본인 글만 반환)
 * - Q&A   → `GET /api/v1/questions/me` (QuestionController 에 별도 신설)
 *
 * 커피챗만 본 컨트롤러로 분리한 이유: 응답 DTO 가 상대방 정보 위주(`CoffeeChatPartnerResponse`)라
 * 기존 `CoffeeChatController` 의 본인 시점 응답(`CoffeeChatResponse`) 과 분리하기 위함.
 */
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MyActivityController implements MyActivitySwagger {

    private final CoffeeChatService coffeeChatService;

    /** 나의 활동 — 커피챗 목록 (상대방 정보 카드) */
    @GetMapping("/me/activity/coffeechats")
    public ApiResponse<List<CoffeeChatPartnerResponse>> getMyCoffeeChats(
            @Parameter(hidden = true) @LoginMember Member member) {
        return ApiResponse.onSuccess(coffeeChatService.getMyActivities(member));
    }
}
