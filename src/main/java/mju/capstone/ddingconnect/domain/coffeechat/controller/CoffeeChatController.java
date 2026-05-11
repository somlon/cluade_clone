package mju.capstone.ddingconnect.domain.coffeechat.controller;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import mju.capstone.ddingconnect.domain.coffeechat.dto.request.CreateCoffeeChatRequest;
import mju.capstone.ddingconnect.domain.coffeechat.dto.request.UpdateCoffeeChatStatusRequest;
import mju.capstone.ddingconnect.domain.coffeechat.dto.response.CoffeeChatResponse;
import mju.capstone.ddingconnect.domain.coffeechat.service.CoffeeChatService;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.global.auth.annotation.LoginMember;
import mju.capstone.ddingconnect.global.response.exception.handler.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/coffeechat")
@RequiredArgsConstructor
public class CoffeeChatController implements CoffeeChatSwagger {

    private final CoffeeChatService coffeeChatService;

    /** 커피챗 요청 (Create) */
    @PostMapping
    public ApiResponse<CoffeeChatResponse> requestCoffeeChat(
            @Parameter(hidden = true) @LoginMember Member member,
            @RequestBody CreateCoffeeChatRequest request) {
        return ApiResponse.onSuccess(coffeeChatService.create(member, request));
    }

    /** 내가 보낸 커피챗 목록 조회 (Read) */
    @GetMapping("/sent")
    public ApiResponse<List<CoffeeChatResponse>> getSentCoffeeChats(
            @Parameter(hidden = true) @LoginMember Member member) {
        return ApiResponse.onSuccess(coffeeChatService.getSentList(member));
    }

    /** 내가 받은 커피챗 목록 조회 (Read) */
    @GetMapping("/received")
    public ApiResponse<List<CoffeeChatResponse>> getReceivedCoffeeChats(
            @Parameter(hidden = true) @LoginMember Member member) {
        return ApiResponse.onSuccess(coffeeChatService.getReceivedList(member));
    }

    /** 커피챗 수락/거절 (Update) */
    @PatchMapping("/{coffeeChatId}/status")
    public ApiResponse<CoffeeChatResponse> updateCoffeeChatStatus(
            @Parameter(hidden = true) @LoginMember Member member,
            @PathVariable Long coffeeChatId,
            @RequestBody UpdateCoffeeChatStatusRequest request) {
        return ApiResponse.onSuccess(coffeeChatService.updateStatus(member, coffeeChatId, request));
    }

    /** 커피챗 취소 (Delete) */
    @DeleteMapping("/{coffeeChatId}")
    public ApiResponse<String> deleteCoffeeChat(
            @Parameter(hidden = true) @LoginMember Member member,
            @PathVariable Long coffeeChatId) {
        coffeeChatService.delete(member, coffeeChatId);
        return ApiResponse.onSuccess("커피챗이 취소되었습니다.");
    }
}
