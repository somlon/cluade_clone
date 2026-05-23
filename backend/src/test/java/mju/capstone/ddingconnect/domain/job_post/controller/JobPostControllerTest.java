package mju.capstone.ddingconnect.domain.job_post.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mju.capstone.ddingconnect.domain.job_post.domain.CareerType;
import mju.capstone.ddingconnect.domain.job_post.domain.JobType;
import mju.capstone.ddingconnect.domain.job_post.dto.request.CreateJobPostLinkRequest;
import mju.capstone.ddingconnect.domain.job_post.dto.request.CreateJobPostRequest;
import mju.capstone.ddingconnect.domain.job_post.dto.request.UpdateJobPostRequest;
import mju.capstone.ddingconnect.domain.job_post.dto.response.GraduatePostResponse;
import mju.capstone.ddingconnect.domain.job_post.dto.response.JobPostResponse;
import mju.capstone.ddingconnect.domain.job_post.service.JobPostService;
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

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(JobPostController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({WebMvcConfig.class, LoginMemberArgumentResolver.class})
@DisplayName("JobPostController 슬라이스 테스트")
class JobPostControllerTest {

    private static final String BASE_URL = "/api/v1/job-post";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean JobPostService jobPostService;

    @BeforeEach
    void setUp() { WithMockLoginMember.loginAsGraduate(); }

    @AfterEach
    void tearDown() { WithMockLoginMember.clear(); }

    @Test
    @DisplayName("POST /api/v1/job-post - 구직 공고 등록")
    void createJobPost() throws Exception {
        CreateJobPostRequest req = new CreateJobPostRequest("img", "성남", CareerType.NEW_GRADUATE,
                JobType.BACKEND, "한국", "성남시", "분당구",
                LocalDate.of(2026, 6, 30), "https://t.com", List.of("JavaScript", "React", "Node.js"), "네이버");
        JobPostResponse res = new JobPostResponse(1L, "네이버", "img", "성남",
                CareerType.NEW_GRADUATE, JobType.BACKEND, "분당구",
                LocalDate.of(2026, 6, 30), "https://t.com", List.of("JavaScript", "React", "Node.js"));
        given(jobPostService.create(any(), any())).willReturn(res);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.id").value(1L))
                .andExpect(jsonPath("$.result.companyName").value("네이버"))
                .andExpect(jsonPath("$.result.preferredLanguages").isArray())
                .andExpect(jsonPath("$.result.preferredLanguages.length()").value(3))
                .andExpect(jsonPath("$.result.preferredLanguages[0]").value("JavaScript"));
    }

    @Test
    @DisplayName("GET /api/v1/job-post - 구직 공고 목록 조회")
    void getJobPostList() throws Exception {
        given(jobPostService.getList()).willReturn(List.of());

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/job-post/{id} - 구직 공고 상세 조회")
    void getJobPostDetail() throws Exception {
        JobPostResponse res = new JobPostResponse(1L, "네이버", null, null, null, null, null, null, null, null);
        given(jobPostService.getOne(1L)).willReturn(res);

        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(1L));
    }

    @Test
    @DisplayName("PATCH /api/v1/job-post/{id} - 구직 공고 수정")
    void updateJobPost() throws Exception {
        UpdateJobPostRequest req = new UpdateJobPostRequest(null, null, null, null,
                null, null, null, null, null, null, "카카오");
        JobPostResponse res = new JobPostResponse(1L, "카카오", null, null, null, null, null, null, null, null);
        given(jobPostService.update(any(), eq(1L), any())).willReturn(res);

        mockMvc.perform(patch(BASE_URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.companyName").value("카카오"));
    }

    @Test
    @DisplayName("DELETE /api/v1/job-post/{id} - 구직 공고 삭제")
    void deleteJobPost() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(SuccessMessage.JOB_POST_DELETED));
        verify(jobPostService).delete(any(), eq(1L));
    }

    // ── TODO R: 링크 등록 + 분리 조회 ─────────────────────────────────

    @Test
    @DisplayName("POST /api/v1/job-post/link - 링크 전용 등록을 createFromLink 에 위임")
    void createJobPostLink() throws Exception {
        CreateJobPostLinkRequest req = new CreateJobPostLinkRequest("https://example.com/jobs/1");
        JobPostResponse res = new JobPostResponse(50L, null, null, null, null, null, null, null,
                "https://example.com/jobs/1", java.util.List.of());
        given(jobPostService.createFromLink(any(), any())).willReturn(res);

        mockMvc.perform(post(BASE_URL + "/link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(50L))
                .andExpect(jsonPath("$.result.detailUrl").value("https://example.com/jobs/1"));

        verify(jobPostService).createFromLink(any(), any());
    }

    @Test
    @DisplayName("POST /api/v1/job-post/link - 잘못된 URL 형식은 400 으로 거부")
    void createJobPostLinkRejectsBadUrl() throws Exception {
        CreateJobPostLinkRequest req = new CreateJobPostLinkRequest("not-a-url");

        mockMvc.perform(post(BASE_URL + "/link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false));
        verify(jobPostService, org.mockito.Mockito.never()).createFromLink(any(), any());
    }

    @Test
    @DisplayName("GET /api/v1/job-post/graduates - 선배 공고 카드 리스트 위임")
    void getGraduatePosts() throws Exception {
        given(jobPostService.getGraduatePosts()).willReturn(java.util.List.of(
                new GraduatePostResponse(100L, "네이버", null, null, null, JobType.BACKEND,
                        null, null, null, java.util.List.of(),
                        1L, "선배닉", "응용소프트웨어학과", JobType.BACKEND, 5)));

        mockMvc.perform(get(BASE_URL + "/graduates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].id").value(100L))
                .andExpect(jsonPath("$.result[0].graduateMemberId").value(1L))
                .andExpect(jsonPath("$.result[0].graduateNickname").value("선배닉"))
                .andExpect(jsonPath("$.result[0].graduateCareerYear").value(5));

        verify(jobPostService).getGraduatePosts();
    }

    @Test
    @DisplayName("GET /api/v1/job-post/crawled - 일반 공고 리스트 위임")
    void getCrawledJobPosts() throws Exception {
        given(jobPostService.getCrawledPosts()).willReturn(java.util.List.of(
                new JobPostResponse(200L, "크롤링", null, null, null, null, null, null, null, java.util.List.of())));

        mockMvc.perform(get(BASE_URL + "/crawled"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].id").value(200L))
                .andExpect(jsonPath("$.result[0].companyName").value("크롤링"));

        verify(jobPostService).getCrawledPosts();
    }
}
