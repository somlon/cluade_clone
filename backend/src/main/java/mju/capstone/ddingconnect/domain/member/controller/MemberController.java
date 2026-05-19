package mju.capstone.ddingconnect.domain.member.controller;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.member.dto.request.UpdateMemberRequest;
import mju.capstone.ddingconnect.domain.member.dto.request.UpdateMyPageRequest;
import mju.capstone.ddingconnect.domain.member.dto.response.MemberResponse;
import mju.capstone.ddingconnect.domain.member.dto.response.MyPageResponse;
import mju.capstone.ddingconnect.domain.member.service.MemberService;
import mju.capstone.ddingconnect.domain.member.service.MyPageService;
import mju.capstone.ddingconnect.global.auth.annotation.LoginMember;
import mju.capstone.ddingconnect.global.common.SuccessMessage;
import mju.capstone.ddingconnect.global.response.exception.handler.ApiResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController implements MemberSwagger {

    private final MemberService memberService;
    private final MyPageService myPageService;

    /** JWT 인증 테스트용 엔드포인트 (기존 코드 유지) */
    @GetMapping("/test")
    public ApiResponse<String> test(
            @Parameter(hidden = true) @LoginMember Member member) {
        return ApiResponse.onSuccess(member.getEmail());
    }

    /** 내 프로필 조회 (Read) */
    @GetMapping("/me")
    public ApiResponse<MemberResponse> getMyProfile(
            @Parameter(hidden = true) @LoginMember Member member) {
        return ApiResponse.onSuccess(memberService.getMyProfile(member));
    }

    /** 회원 정보 수정 (Update) */
    @PatchMapping("/me")
    public ApiResponse<MemberResponse> updateMyProfile(
            @Parameter(hidden = true) @LoginMember Member member,
            @Valid @RequestBody UpdateMemberRequest request) {
        return ApiResponse.onSuccess(memberService.updateMyProfile(member, request));
    }

    /** 회원 탈퇴 (Delete - 소프트 삭제) */
    @DeleteMapping("/me")
    public ApiResponse<String> withdraw(
            @Parameter(hidden = true) @LoginMember Member member) {
        memberService.withdraw(member);
        return ApiResponse.onSuccess(SuccessMessage.MEMBER_WITHDRAWN);
    }

    /** 마이페이지 조회 (Read) — 프로필/활동 통계/기술 스택/역할별 항목 통합 */
    @GetMapping("/mypage")
    public ApiResponse<MyPageResponse> getMyPage(
            @Parameter(hidden = true) @LoginMember Member member) {
        return ApiResponse.onSuccess(myPageService.getMyPage(member));
    }

    /** 마이페이지 통합 수정 (Update) — 프로필/기술 스택/관심 직군/구직 공고를 1회 요청으로 일괄 수정 */
    @PatchMapping("/mypage")
    public ApiResponse<MyPageResponse> updateMyPage(
            @Parameter(hidden = true) @LoginMember Member member,
            @Valid @RequestBody UpdateMyPageRequest request) {
        return ApiResponse.onSuccess(myPageService.updateMyPage(member, request));
    }
}
