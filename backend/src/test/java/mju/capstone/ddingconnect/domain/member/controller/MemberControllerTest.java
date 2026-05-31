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
import mju.capstone.ddingconnect.global.aws.dto.PresignedUploadRequest;
import mju.capstone.ddingconnect.global.aws.dto.PresignedUploadResponse;
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

import java.time.LocalDateTime;
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
    @DisplayName("GET /api/v1/members/mypage - STUDENT: jobPosts·grade 외 GRADUATE 전용 키 제외, []·필수 키는 유지")
    void getMyPageForStudentExcludesGraduateOnlyKeys() throws Exception {
        // STUDENT: point 는 마이페이지에선 항상 제외(헬퍼 적용), grade 는 STUDENT 전용으로 노출
        MemberResponse profile = new MemberResponse(1L, "test@mju.ac.kr", null, "테스터",
                "60201234", "컴퓨터공학과", null, null, null, null, null,
                MemberRole.STUDENT, 3, null, null, null, null);
        MyPageResponse res = new MyPageResponse(
                profile,
                new MyPageResponse.ActivityStats(2L, 1L, 5L),
                List.of(),
                List.of(),    // STUDENT 의 targetJobs 0개 → [] 로 유지
                null);        // STUDENT 는 jobPosts 비해당 → null → 응답 키 제외
        given(myPageService.getMyPage(any())).willReturn(res);

        mockMvc.perform(get(BASE_URL + "/mypage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.profile.email").value("test@mju.ac.kr"))
                .andExpect(jsonPath("$.result.profile.grade").value(3))
                .andExpect(jsonPath("$.result.activity.questionCount").value(5))
                .andExpect(jsonPath("$.result.activity.roadmapCount").value(1))
                // "해당 역할 + 0개" 는 [] 로 보존
                .andExpect(jsonPath("$.result.targetJobs").isArray())
                .andExpect(jsonPath("$.result.targetJobs").isEmpty())
                // 비해당 역할 필드 + dead 필드는 키 자체 제외
                .andExpect(jsonPath("$.result.jobPosts").doesNotExist())
                .andExpect(jsonPath("$.result.profile.point").doesNotExist())
                .andExpect(jsonPath("$.result.profile.businessCardImage").doesNotExist())
                .andExpect(jsonPath("$.result.profile.jobType").doesNotExist())
                .andExpect(jsonPath("$.result.profile.company").doesNotExist())
                .andExpect(jsonPath("$.result.profile.careerYear").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/v1/members/mypage - GRADUATE: targetJobs·grade·roadmapCount 키 제외, jobPosts []는 유지")
    void getMyPageForGraduateExcludesStudentOnlyKeys() throws Exception {
        WithMockLoginMember.loginAsGraduate();
        // GRADUATE: point 항상 제외, businessCardImage/jobType/company/careerYear 노출
        MemberResponse profile = new MemberResponse(2L, "g@mju.ac.kr", null, "졸업생",
                "60201234", "컴퓨터공학과", null, null, null, null, null,
                MemberRole.GRADUATE, null, "card.png", null, "네이버", 3);
        MyPageResponse res = new MyPageResponse(
                profile,
                // roadmapCount = null → 응답에서 키 자체 제외
                new MyPageResponse.ActivityStats(0L, null, 0L),
                List.of(),
                null,            // GRADUATE 는 targetJobs 비해당 → null → 키 제외
                List.of());      // GRADUATE 의 jobPosts 0개 → [] 로 유지
        given(myPageService.getMyPage(any())).willReturn(res);

        mockMvc.perform(get(BASE_URL + "/mypage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.profile.email").value("g@mju.ac.kr"))
                .andExpect(jsonPath("$.result.profile.company").value("네이버"))
                .andExpect(jsonPath("$.result.activity.coffeeChatCount").value(0))
                .andExpect(jsonPath("$.result.activity.questionCount").value(0))
                // "해당 역할 + 0개" 는 [] 로 보존
                .andExpect(jsonPath("$.result.jobPosts").isArray())
                .andExpect(jsonPath("$.result.jobPosts").isEmpty())
                // 비해당 역할 필드 + dead 필드는 키 자체 제외
                .andExpect(jsonPath("$.result.targetJobs").doesNotExist())
                .andExpect(jsonPath("$.result.activity.roadmapCount").doesNotExist())
                .andExpect(jsonPath("$.result.profile.grade").doesNotExist())
                .andExpect(jsonPath("$.result.profile.point").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/v1/members/me - 다른 엔드포인트는 point 가 null 이면 키 제외(정책 분리)")
    void getMyProfileExcludesNullPoint() throws Exception {
        MemberResponse res = new MemberResponse(1L, "test@mju.ac.kr", null, "테스터",
                "60201234", "컴퓨터공학과", null, null, null, null, null,
                MemberRole.STUDENT, 3, null, null, null, null);
        given(memberService.getMyProfile(any())).willReturn(res);

        mockMvc.perform(get(BASE_URL + "/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.email").value("test@mju.ac.kr"))
                .andExpect(jsonPath("$.result.point").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/v1/members/me - 다른 엔드포인트는 point 값이 있으면 응답에 포함(정책 분리)")
    void getMyProfileIncludesPresentPoint() throws Exception {
        MemberResponse res = new MemberResponse(1L, "test@mju.ac.kr", null, "테스터",
                "60201234", "컴퓨터공학과", null, null, null, null, 1500L,
                MemberRole.STUDENT, 3, null, null, null, null);
        given(memberService.getMyProfile(any())).willReturn(res);

        mockMvc.perform(get(BASE_URL + "/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.point").value(1500));
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
    @DisplayName("POST /api/v1/members/me/profile-image/presigned-url - presigned URL 발급 → MemberService.createProfileImageUploadUrl 위임")
    void createProfileImageUploadUrlDelegates() throws Exception {
        PresignedUploadResponse res = new PresignedUploadResponse(
                "https://bucket.s3.region.amazonaws.com/p-uuid.png?X-Amz-Signature=sig",
                "https://bucket.s3.region.amazonaws.com/p-uuid.png",
                "p-uuid.png", LocalDateTime.now().plusMinutes(5));
        given(memberService.createProfileImageUploadUrl(any())).willReturn(res);

        mockMvc.perform(post(BASE_URL + "/me/profile-image/presigned-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PresignedUploadRequest("profile.png", "image/png"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.uploadUrl").value(res.uploadUrl()))
                .andExpect(jsonPath("$.result.fileUrl").value(res.fileUrl()))
                .andExpect(jsonPath("$.result.key").value("p-uuid.png"));

        verify(memberService).createProfileImageUploadUrl(any());
    }

    @Test
    @DisplayName("POST /api/v1/members/me/business-card/presigned-url - presigned URL 발급 → MemberService.createBusinessCardUploadUrl 위임")
    void createBusinessCardUploadUrlDelegates() throws Exception {
        WithMockLoginMember.loginAsGraduate();
        PresignedUploadResponse res = new PresignedUploadResponse(
                "https://bucket.s3.region.amazonaws.com/card-uuid.png?X-Amz-Signature=sig",
                "https://bucket.s3.region.amazonaws.com/card-uuid.png",
                "card-uuid.png", LocalDateTime.now().plusMinutes(5));
        given(memberService.createBusinessCardUploadUrl(any(), any())).willReturn(res);

        mockMvc.perform(post(BASE_URL + "/me/business-card/presigned-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PresignedUploadRequest("card.png", "image/png"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.uploadUrl").value(res.uploadUrl()))
                .andExpect(jsonPath("$.result.fileUrl").value(res.fileUrl()));

        verify(memberService).createBusinessCardUploadUrl(any(), any());
    }

    @Test
    @DisplayName("POST /api/v1/members/me/portfolio/presigned-url - presigned URL 발급 → MemberService.createPortfolioUploadUrl 위임")
    void createPortfolioUploadUrlDelegates() throws Exception {
        PresignedUploadResponse res = new PresignedUploadResponse(
                "https://bucket.s3.region.amazonaws.com/resume-uuid.pdf?X-Amz-Signature=sig",
                "https://bucket.s3.region.amazonaws.com/resume-uuid.pdf",
                "resume-uuid.pdf", LocalDateTime.now().plusMinutes(5));
        given(memberService.createPortfolioUploadUrl(any())).willReturn(res);

        mockMvc.perform(post(BASE_URL + "/me/portfolio/presigned-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PresignedUploadRequest("resume.pdf", "application/pdf"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.fileUrl").value(res.fileUrl()));

        verify(memberService).createPortfolioUploadUrl(any());
    }

    @Test
    @DisplayName("POST /api/v1/members/me/profile-image/presigned-url - fileName 공백 등 검증 실패는 400 (서비스 미호출)")
    void createUploadUrlRejectsInvalidRequest() throws Exception {
        mockMvc.perform(post(BASE_URL + "/me/profile-image/presigned-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PresignedUploadRequest("  ", "image/png"))))
                .andExpect(status().isBadRequest());

        verify(memberService, never()).createProfileImageUploadUrl(any());
    }
}
