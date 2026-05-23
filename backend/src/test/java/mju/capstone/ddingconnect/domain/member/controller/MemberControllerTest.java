package mju.capstone.ddingconnect.domain.member.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mju.capstone.ddingconnect.domain.interested_job.domain.TargetJobCategory;
import mju.capstone.ddingconnect.domain.job_post.dto.request.CreateJobPostLinkRequest;
import mju.capstone.ddingconnect.domain.member.domain.MemberRole;
import mju.capstone.ddingconnect.domain.member.dto.request.UpdateGraduateMyPageRequest;
import mju.capstone.ddingconnect.domain.member.dto.request.UpdateGraduateProfileRequest;
import mju.capstone.ddingconnect.domain.member.dto.request.UpdateMemberRequest;
import mju.capstone.ddingconnect.domain.member.dto.request.UpdateStudentMyPageRequest;
import mju.capstone.ddingconnect.domain.member.dto.request.UpdateStudentProfileRequest;
import mju.capstone.ddingconnect.domain.member.dto.response.MemberResponse;
import mju.capstone.ddingconnect.domain.member.dto.response.MyPageResponse;
import mju.capstone.ddingconnect.domain.member.service.MemberService;
import mju.capstone.ddingconnect.domain.member.service.MyPageService;
import mju.capstone.ddingconnect.domain.techstack.domain.TechStackName;
import mju.capstone.ddingconnect.global.auth.annotation.LoginMemberArgumentResolver;
import mju.capstone.ddingconnect.global.common.SuccessMessage;
import mju.capstone.ddingconnect.global.config.WebMvcConfig;
import mju.capstone.ddingconnect.support.WithMockLoginMember;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MemberController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({WebMvcConfig.class, LoginMemberArgumentResolver.class})
@DisplayName("MemberController 슬라이스 테스트")
class MemberControllerTest {

    private static final String BASE_URL = "/api/v1/members";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean MemberService memberService;
    @MockitoBean MyPageService myPageService;

    @BeforeEach
    void setUp() { WithMockLoginMember.loginAsStudent(); }

    @AfterEach
    void tearDown() { WithMockLoginMember.clear(); }

    @Test
    @DisplayName("GET /api/v1/members/test - JWT 인증 테스트")
    void jwtAuthTest() throws Exception {
        mockMvc.perform(get(BASE_URL + "/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("test@mju.ac.kr"));
    }

    @Test
    @DisplayName("GET /api/v1/members/me - 내 프로필 조회")
    void getMyProfile() throws Exception {
        MemberResponse res = new MemberResponse(1L, "test@mju.ac.kr", null, "테스터",
                "60201234", "컴퓨터공학과", null, null, null, null, 0L,
                MemberRole.STUDENT, 3, null, null, null, null);
        given(memberService.getMyProfile(any())).willReturn(res);

        mockMvc.perform(get(BASE_URL + "/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.email").value("test@mju.ac.kr"))
                .andExpect(jsonPath("$.result.grade").value(3));
    }

    @Test
    @DisplayName("PATCH /api/v1/members/me - 회원 정보 수정")
    void updateMyProfile() throws Exception {
        UpdateMemberRequest req = new UpdateMemberRequest("새이름", null, "새닉네임", null, null,
                null, null, null, null, null, null, null, null, null);
        MemberResponse res = new MemberResponse(1L, "test@mju.ac.kr", "새이름", "새닉네임",
                "60201234", "컴퓨터공학과", null, null, null, null, 0L,
                MemberRole.STUDENT, 3, null, null, null, null);
        given(memberService.updateMyProfile(any(), any())).willReturn(res);

        mockMvc.perform(patch(BASE_URL + "/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("새이름"))
                .andExpect(jsonPath("$.result.nickname").value("새닉네임"));
    }

    @Test
    @DisplayName("DELETE /api/v1/members/me - 회원 탈퇴")
    void withdrawMember() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(SuccessMessage.MEMBER_WITHDRAWN));
        verify(memberService).withdraw(any());
    }

    @Test
    @DisplayName("GET /api/v1/members/mypage - 마이페이지 조회")
    void getMyPage() throws Exception {
        MemberResponse profile = new MemberResponse(1L, "test@mju.ac.kr", null, "테스터",
                "60201234", "컴퓨터공학과", null, null, null, null, 0L,
                MemberRole.STUDENT, 3, null, null, null, null);
        MyPageResponse res = new MyPageResponse(
                profile,
                new MyPageResponse.ActivityStats(2L, 1L, 5L),
                List.of(), List.of(), List.of());
        given(myPageService.getMyPage(any())).willReturn(res);

        mockMvc.perform(get(BASE_URL + "/mypage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.profile.email").value("test@mju.ac.kr"))
                .andExpect(jsonPath("$.result.activity.questionCount").value(5));
    }

    // ── 재학생 마이페이지 통합 수정 ─────────────────────────────────────

    @Test
    @DisplayName("PATCH /api/v1/members/mypage/student - 재학생 마이페이지 통합 수정")
    void updateStudentMyPage() throws Exception {
        UpdateStudentProfileRequest profile = new UpdateStudentProfileRequest(
                null, null, "새닉네임", null, null, null, null, null, null, 3);
        UpdateStudentMyPageRequest req = new UpdateStudentMyPageRequest(
                profile, List.of(TechStackName.JAVA), List.of(TargetJobCategory.BACKEND));

        MemberResponse updated = new MemberResponse(1L, "test@mju.ac.kr", null, "새닉네임",
                "60201234", "컴퓨터공학과", null, null, null, null, 0L,
                MemberRole.STUDENT, 3, null, null, null, null);
        MyPageResponse res = new MyPageResponse(
                updated,
                new MyPageResponse.ActivityStats(2L, 1L, 5L),
                List.of(), List.of(), List.of());
        given(myPageService.updateStudentMyPage(any(), any())).willReturn(res);

        mockMvc.perform(patch(BASE_URL + "/mypage/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.profile.nickname").value("새닉네임"))
                .andExpect(jsonPath("$.result.activity.questionCount").value(5));
    }

    @Test
    @DisplayName("PATCH /api/v1/members/mypage/student - 프로필 형식 오류 시 400")
    void updateStudentMyPageValidationFailure() throws Exception {
        // githubLink 가 github.com URL 형식이 아니므로 @Valid 가 profile 로 cascade 되어 검증 실패
        UpdateStudentProfileRequest invalidProfile = new UpdateStudentProfileRequest(
                null, null, null, null, null, "invalid-link", null, null, null, null);
        UpdateStudentMyPageRequest req = new UpdateStudentMyPageRequest(invalidProfile, null, null);

        mockMvc.perform(patch(BASE_URL + "/mypage/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false));
        verify(myPageService, never()).updateStudentMyPage(any(), any());
    }

    @Test
    @DisplayName("PATCH /api/v1/members/mypage/student - 비인증 요청은 거부되어 서비스에 도달하지 않는다")
    void updateStudentMyPageUnauthenticated() throws Exception {
        // @LoginMember 해석은 SecurityContext 인증 정보에 의존한다 — 인증을 비운다.
        WithMockLoginMember.clear();
        UpdateStudentMyPageRequest req = new UpdateStudentMyPageRequest(null, null, null);

        mockMvc.perform(patch(BASE_URL + "/mypage/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.isSuccess").value(false));
        verify(myPageService, never()).updateStudentMyPage(any(), any());
    }

    // ── 졸업생 마이페이지 통합 수정 ─────────────────────────────────────

    @Test
    @DisplayName("PATCH /api/v1/members/mypage/graduate - 졸업생 마이페이지 통합 수정")
    void updateGraduateMyPage() throws Exception {
        WithMockLoginMember.loginAsGraduate();

        UpdateGraduateProfileRequest profile = new UpdateGraduateProfileRequest(
                null, null, "새닉네임", null, null, null, null, null, null,
                null, null, "카카오", 3);
        UpdateGraduateMyPageRequest req = new UpdateGraduateMyPageRequest(
                profile, List.of(TechStackName.JAVA),
                List.of(new CreateJobPostLinkRequest("https://jobs.example.com/1")),
                List.of(100L));

        MemberResponse updated = new MemberResponse(2L, "g@mju.ac.kr", null, "새닉네임",
                "60201234", "컴퓨터공학과", null, null, null, null, 0L,
                MemberRole.GRADUATE, null, null, null, "카카오", 3);
        MyPageResponse res = new MyPageResponse(
                updated,
                new MyPageResponse.ActivityStats(0L, 0L, 0L),
                List.of(), List.of(), List.of());
        given(myPageService.updateGraduateMyPage(any(), any())).willReturn(res);

        mockMvc.perform(patch(BASE_URL + "/mypage/graduate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.profile.nickname").value("새닉네임"))
                .andExpect(jsonPath("$.result.profile.company").value("카카오"));
    }

    @Test
    @DisplayName("PATCH /api/v1/members/mypage/graduate - 잘못된 공고 링크 형식이면 400")
    void updateGraduateMyPageInvalidLink() throws Exception {
        WithMockLoginMember.loginAsGraduate();
        UpdateGraduateMyPageRequest req = new UpdateGraduateMyPageRequest(
                null, null,
                // detailUrl 이 http(s):// 로 시작하지 않아 @Pattern 위반
                List.of(new CreateJobPostLinkRequest("not-a-url")),
                null);

        mockMvc.perform(patch(BASE_URL + "/mypage/graduate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false));
        verify(myPageService, never()).updateGraduateMyPage(any(), any());
    }

    @Test
    @DisplayName("PATCH /api/v1/members/me/profile-image - multipart 업로드 → MemberService.updateProfileImage 위임")
    void updateProfileImageDelegates() throws Exception {
        org.springframework.mock.web.MockMultipartFile image =
                new org.springframework.mock.web.MockMultipartFile(
                        "image", "p.png", "image/png", new byte[]{1, 2, 3});
        MemberResponse res = new MemberResponse(1L, "test@mju.ac.kr", null, "테스터",
                null, null, null, null, null,
                "https://bucket.s3.region.amazonaws.com/new.png",
                0L, MemberRole.STUDENT, 3, null, null, null, null);
        given(memberService.updateProfileImage(any(), any())).willReturn(res);

        mockMvc.perform(multipart(BASE_URL + "/me/profile-image")
                        .file(image)
                        .with(req -> { req.setMethod("PATCH"); return req; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.profileImage")
                        .value("https://bucket.s3.region.amazonaws.com/new.png"));

        verify(memberService).updateProfileImage(any(), any());
    }

    @Test
    @DisplayName("PATCH /api/v1/members/me/business-card - multipart 업로드 → MemberService.updateBusinessCard 위임")
    void updateBusinessCardDelegates() throws Exception {
        WithMockLoginMember.loginAsGraduate();
        org.springframework.mock.web.MockMultipartFile image =
                new org.springframework.mock.web.MockMultipartFile(
                        "image", "card.png", "image/png", new byte[]{1, 2, 3});
        MemberResponse res = new MemberResponse(2L, "g@mju.ac.kr", null, "졸업생",
                null, null, null, null, null, null,
                0L, MemberRole.GRADUATE, null,
                "https://bucket.s3.region.amazonaws.com/card.png",
                null, "네이버", 3);
        given(memberService.updateBusinessCard(any(), any())).willReturn(res);

        mockMvc.perform(multipart(BASE_URL + "/me/business-card")
                        .file(image)
                        .with(req -> { req.setMethod("PATCH"); return req; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.businessCardImage")
                        .value("https://bucket.s3.region.amazonaws.com/card.png"));

        verify(memberService).updateBusinessCard(any(), any());
    }

    @Test
    @DisplayName("PATCH /api/v1/members/me/portfolio - multipart PDF 업로드 → MemberService.updatePortfolio 위임")
    void updatePortfolioDelegates() throws Exception {
        org.springframework.mock.web.MockMultipartFile file =
                new org.springframework.mock.web.MockMultipartFile(
                        "file", "portfolio.pdf", "application/pdf", new byte[]{1, 2, 3});
        MemberResponse res = new MemberResponse(1L, "test@mju.ac.kr", null, "테스터",
                null, null, null, null,
                "https://bucket.s3.region.amazonaws.com/portfolio.pdf", null,
                0L, MemberRole.STUDENT, 3, null, null, null, null);
        given(memberService.updatePortfolio(any(), any())).willReturn(res);

        mockMvc.perform(multipart(BASE_URL + "/me/portfolio")
                        .file(file)
                        .with(req -> { req.setMethod("PATCH"); return req; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.portfolio")
                        .value("https://bucket.s3.region.amazonaws.com/portfolio.pdf"));

        verify(memberService).updatePortfolio(any(), any());
    }
}
