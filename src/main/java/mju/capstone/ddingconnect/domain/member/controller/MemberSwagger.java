package mju.capstone.ddingconnect.domain.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.member.dto.request.UpdateMemberRequest;
import mju.capstone.ddingconnect.domain.member.dto.response.MemberResponse;
import mju.capstone.ddingconnect.domain.member.dto.response.MyPageResponse;
import mju.capstone.ddingconnect.global.auth.annotation.LoginMember;
import mju.capstone.ddingconnect.global.response.exception.handler.ApiResponse;
import org.springframework.web.bind.annotation.*;

@Tag(name = "회원", description = "회원 컨트롤러 — 프로필 조회/수정/탈퇴(소프트 삭제), 마이페이지 조회 엔드포인트를 제공합니다. (회원가입은 인증 API에서 담당)")
public interface MemberSwagger {

    @Operation(
            summary = "JWT 인증 테스트",
            description = "JWT 토큰 인증이 정상적으로 동작하는지 확인하는 테스트용 엔드포인트입니다. 로그인된 회원의 이메일을 반환합니다.",
            hidden = true
    )
    @GetMapping("/test")
    ApiResponse<String> test(
            @Parameter(hidden = true) @LoginMember Member member);




    @Operation(
            summary = "내 프로필 조회",
            description = "로그인된 회원의 프로필 정보를 조회합니다."
    )
    @GetMapping("/me")
    ApiResponse<MemberResponse> getMyProfile(
            @Parameter(hidden = true) @LoginMember Member member);




    @Operation(
            summary = "회원 정보 수정",
            description = "로그인된 회원의 프로필 정보(닉네임, 자기소개 등)를 수정합니다."
    )
    @PatchMapping("/me")
    ApiResponse<MemberResponse> updateMyProfile(
            @Parameter(hidden = true) @LoginMember Member member,
            @Parameter(description = "수정할 회원 정보")
            @RequestBody UpdateMemberRequest request);




    @Operation(
            summary = "회원 탈퇴",
            description = "로그인된 회원의 계정을 소프트 삭제(비활성화) 방식으로 탈퇴 처리합니다."
    )
    @DeleteMapping("/me")
    ApiResponse<String> withdraw(
            @Parameter(hidden = true) @LoginMember Member member);




    @Operation(
            summary = "마이페이지 조회",
            description = "로그인된 회원의 마이페이지 정보를 한 번에 조회합니다. "
                    + "프로필, 활동 통계(커피챗/로드맵/질문 수), 기술 스택과 "
                    + "역할별 항목(재학생: 관심 직군, 졸업생: 등록 구직 공고)을 포함합니다."
    )
    @GetMapping("/mypage")
    ApiResponse<MyPageResponse> getMyPage(
            @Parameter(hidden = true) @LoginMember Member member);

}
