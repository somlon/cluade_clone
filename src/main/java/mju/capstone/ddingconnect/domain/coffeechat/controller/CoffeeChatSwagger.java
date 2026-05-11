package mju.capstone.ddingconnect.domain.coffeechat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import mju.capstone.ddingconnect.domain.coffeechat.dto.request.CreateCoffeeChatRequest;
import mju.capstone.ddingconnect.domain.coffeechat.dto.request.UpdateCoffeeChatStatusRequest;
import mju.capstone.ddingconnect.domain.coffeechat.dto.response.CoffeeChatResponse;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.global.auth.annotation.LoginMember;
import mju.capstone.ddingconnect.global.response.exception.handler.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "커피챗", description = "커피챗 컨트롤러 — 요청 등록/보낸·받은 목록 조회/상태 변경(수락·거절)/취소 CRUD 엔드포인트를 제공합니다.")
public interface CoffeeChatSwagger {

    @Operation(
            summary = "커피챗 요청",
            description = "특정 회원에게 커피챗을 요청합니다. 로그인된 회원만 호출할 수 있습니다."
    )
    @PostMapping
    ApiResponse<CoffeeChatResponse> requestCoffeeChat(
            @Parameter(hidden = true) @LoginMember Member member,
            @Parameter(description = "커피챗 요청 정보 (대상 회원 ID, 메시지 등)")
            @RequestBody CreateCoffeeChatRequest request);




    @Operation(
            summary = "내가 보낸 커피챗 목록 조회",
            description = "로그인된 회원이 보낸 커피챗 요청 목록을 조회합니다."
    )
    @GetMapping("/sent")
    ApiResponse<List<CoffeeChatResponse>> getSentCoffeeChats(
            @Parameter(hidden = true) @LoginMember Member member);




    @Operation(
            summary = "내가 받은 커피챗 목록 조회",
            description = "로그인된 회원이 받은 커피챗 요청 목록을 조회합니다."
    )
    @GetMapping("/received")
    ApiResponse<List<CoffeeChatResponse>> getReceivedCoffeeChats(
            @Parameter(hidden = true) @LoginMember Member member);




    @Operation(
            summary = "커피챗 수락/거절",
            description = "받은 커피챗 요청을 수락하거나 거절합니다. 요청을 받은 회원만 호출할 수 있습니다."
    )
    @PatchMapping("/{coffeeChatId}/status")
    ApiResponse<CoffeeChatResponse> updateCoffeeChatStatus(
            @Parameter(hidden = true) @LoginMember Member member,
            @Parameter(description = "상태를 변경할 커피챗 ID")
            @PathVariable Long coffeeChatId,
            @Parameter(description = "변경할 상태 정보 (ACCEPTED / REJECTED)")
            @RequestBody UpdateCoffeeChatStatusRequest request);




    @Operation(
            summary = "커피챗 취소",
            description = "본인이 보낸 커피챗 요청을 취소(삭제)합니다."
    )
    @DeleteMapping("/{coffeeChatId}")
    ApiResponse<String> deleteCoffeeChat(
            @Parameter(hidden = true) @LoginMember Member member,
            @Parameter(description = "취소할 커피챗 ID")
            @PathVariable Long coffeeChatId);

}
