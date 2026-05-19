package mju.capstone.ddingconnect.domain.coffeechat.controller;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import mju.capstone.ddingconnect.domain.coffeechat.dto.request.MatchingRequest;
import mju.capstone.ddingconnect.domain.coffeechat.dto.response.MatchedCandidateDetailResponse;
import mju.capstone.ddingconnect.domain.coffeechat.dto.response.MatchedCandidateResponse;
import mju.capstone.ddingconnect.domain.coffeechat.service.CoffeeChatMatchingService;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.global.auth.annotation.LoginMember;
import mju.capstone.ddingconnect.global.response.exception.handler.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/coffeechat")
@RequiredArgsConstructor
public class CoffeeChatMatchingController implements CoffeeChatMatchingSwagger {

    private final CoffeeChatMatchingService coffeeChatMatchingService;

    /** 커피챗 매칭 요청 (화면 1 → 2) */
    @PostMapping("/matching")
    public ApiResponse<List<MatchedCandidateResponse>> match(
            @Parameter(hidden = true) @LoginMember Member member,
            @RequestBody MatchingRequest request) {
        return ApiResponse.onSuccess(coffeeChatMatchingService.match(member, request));
    }

    /** 매칭 상대 상세 조회 (화면 3) */
    @GetMapping("/matching/{memberId}")
    public ApiResponse<MatchedCandidateDetailResponse> getCandidateDetail(
            @PathVariable Long memberId) {
        return ApiResponse.onSuccess(coffeeChatMatchingService.getCandidateDetail(memberId));
    }

    /** 나의 활동 › 커피챗 조회 */
    @GetMapping("/my-activity")
    public ApiResponse<List<MatchedCandidateDetailResponse>> getMyActivity(
            @Parameter(hidden = true) @LoginMember Member member) {
        return ApiResponse.onSuccess(coffeeChatMatchingService.getMyActivity(member));
    }
}
