package mju.capstone.ddingconnect.domain.member.controller;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.member.dto.request.UpdateGraduateMyPageRequest;
import mju.capstone.ddingconnect.domain.member.dto.request.UpdateMemberRequest;
import mju.capstone.ddingconnect.domain.member.dto.request.UpdateStudentMyPageRequest;
import mju.capstone.ddingconnect.domain.member.dto.response.MemberResponse;
import mju.capstone.ddingconnect.domain.member.dto.response.MyPageResponse;
import mju.capstone.ddingconnect.domain.member.service.MemberService;
import mju.capstone.ddingconnect.domain.member.service.MyPageService;
import mju.capstone.ddingconnect.global.auth.annotation.LoginMember;
import mju.capstone.ddingconnect.global.common.SuccessMessage;
import mju.capstone.ddingconnect.global.response.exception.handler.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    /** 재학생 마이페이지 통합 수정 (Update) — STUDENT 전용 */
    @PatchMapping("/mypage/student")
    public ApiResponse<MyPageResponse> updateStudentMyPage(
            @Parameter(hidden = true) @LoginMember Member member,
            @Valid @RequestBody UpdateStudentMyPageRequest request) {
        return ApiResponse.onSuccess(myPageService.updateStudentMyPage(member, request));
    }

    /** 졸업생 마이페이지 통합 수정 (Update) — GRADUATE 전용 */
    @PatchMapping("/mypage/graduate")
    public ApiResponse<MyPageResponse> updateGraduateMyPage(
            @Parameter(hidden = true) @LoginMember Member member,
            @Valid @RequestBody UpdateGraduateMyPageRequest request) {
        return ApiResponse.onSuccess(myPageService.updateGraduateMyPage(member, request));
    }

    /** 프로필 사진 업로드/교체 (Update) — multipart/form-data */
    @PatchMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<MemberResponse> updateProfileImage(
            @Parameter(hidden = true) @LoginMember Member member,
            @RequestPart("image") MultipartFile image) {
        return ApiResponse.onSuccess(memberService.updateProfileImage(member, image));
    }
}
