package mju.capstone.ddingconnect.domain.member.controller;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mju.capstone.ddingconnect.domain.member.domain.Member;
import mju.capstone.ddingconnect.domain.member.dto.request.UpdateGraduateMyPageRequest;
import mju.capstone.ddingconnect.domain.member.dto.request.UpdateMemberRequest;
import mju.capstone.ddingconnect.domain.member.dto.request.UpdateStudentMyPageRequest;
import mju.capstone.ddingconnect.domain.member.dto.response.HomeResponse;
import mju.capstone.ddingconnect.domain.member.dto.response.MemberResponse;
import mju.capstone.ddingconnect.domain.member.dto.response.MyPageResponse;
import mju.capstone.ddingconnect.domain.member.dto.response.PointBalanceResponse;
import mju.capstone.ddingconnect.domain.member.dto.response.PointChargeResponse;
import mju.capstone.ddingconnect.domain.member.service.HomeService;
import mju.capstone.ddingconnect.domain.member.service.MemberService;
import mju.capstone.ddingconnect.domain.member.service.MyPageService;
import mju.capstone.ddingconnect.global.auth.annotation.LoginMember;
import mju.capstone.ddingconnect.global.aws.dto.PresignedUploadRequest;
import mju.capstone.ddingconnect.global.aws.dto.PresignedUploadResponse;
import mju.capstone.ddingconnect.global.common.SuccessMessage;
import mju.capstone.ddingconnect.global.response.exception.handler.ApiResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController implements MemberSwagger {

    private final MemberService memberService;
    private final MyPageService myPageService;
    private final HomeService homeService;

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

    /** 홈 화면 조회 (Read) — 포인트/닉네임/학과/학년 또는 경력/활동 카운트 통합 응답 */
    @GetMapping("/me/home")
    public ApiResponse<HomeResponse> getHome(
            @Parameter(hidden = true) @LoginMember Member member) {
        return ApiResponse.onSuccess(homeService.getHome(member));
    }

    /** 포인트 잔액 조회 (Read) — 홈 포인트 모달 진입 시 최신 P 갱신용 */
    @GetMapping("/me/point")
    public ApiResponse<PointBalanceResponse> getMyPoint(
            @Parameter(hidden = true) @LoginMember Member member) {
        return ApiResponse.onSuccess(new PointBalanceResponse(member.getPoint()));
    }

    /** 포인트 충전 페이지 데이터 (Read) — 보유 P + 충전 상품 목록 */
    @GetMapping("/me/point/products")
    public ApiResponse<PointChargeResponse> getPointProducts(
            @Parameter(hidden = true) @LoginMember Member member) {
        return ApiResponse.onSuccess(PointChargeResponse.of(member.getPoint()));
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

    /** 프로필 사진 업로드용 presigned URL 발급 (2-step ①) — 발급받은 fileUrl 을 PATCH /me 의 profileImage 에 저장 */
    @PostMapping("/me/profile-image/presigned-url")
    public ApiResponse<PresignedUploadResponse> createProfileImageUploadUrl(
            @Parameter(hidden = true) @LoginMember Member member,
            @Valid @RequestBody PresignedUploadRequest request) {
        return ApiResponse.onSuccess(memberService.createProfileImageUploadUrl(request));
    }

    /** 졸업생 명함 사진 업로드용 presigned URL 발급 (GRADUATE 전용, 2-step ①) — fileUrl 을 PATCH /mypage/graduate 의 businessCardImage 에 저장 */
    @PostMapping("/me/business-card/presigned-url")
    public ApiResponse<PresignedUploadResponse> createBusinessCardUploadUrl(
            @Parameter(hidden = true) @LoginMember Member member,
            @Valid @RequestBody PresignedUploadRequest request) {
        return ApiResponse.onSuccess(memberService.createBusinessCardUploadUrl(member, request));
    }

    /** 포트폴리오 파일 업로드용 presigned URL 발급 (2-step ①) — fileUrl 을 PATCH /me 의 portfolio 에 저장 */
    @PostMapping("/me/portfolio/presigned-url")
    public ApiResponse<PresignedUploadResponse> createPortfolioUploadUrl(
            @Parameter(hidden = true) @LoginMember Member member,
            @Valid @RequestBody PresignedUploadRequest request) {
        return ApiResponse.onSuccess(memberService.createPortfolioUploadUrl(request));
    }
}
