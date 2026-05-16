package mju.capstone.ddingconnect.domain.job_post.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mju.capstone.ddingconnect.domain.job_post.domain.CareerType;
import mju.capstone.ddingconnect.domain.job_post.domain.JobType;
import mju.capstone.ddingconnect.domain.job_post.dto.request.CreateJobPostRequest;
import mju.capstone.ddingconnect.domain.job_post.dto.request.UpdateJobPostRequest;
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
    void 구직공고_등록() throws Exception {
        CreateJobPostRequest req = new CreateJobPostRequest("img", "성남", CareerType.NEW_GRADUATE,
                JobType.BACKEND, "한국", "성남시", "분당구",
                LocalDate.of(2026, 6, 30), "https://t.com", "Java", "네이버");
        JobPostResponse res = new JobPostResponse(1L, "네이버", "img", "성남",
                CareerType.NEW_GRADUATE, JobType.BACKEND, "분당구",
                LocalDate.of(2026, 6, 30), "https://t.com", "Java");
        given(jobPostService.create(any(), any())).willReturn(res);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.id").value(1L))
                .andExpect(jsonPath("$.result.companyName").value("네이버"));
    }

    @Test
    @DisplayName("GET /api/v1/job-post - 구직 공고 목록 조회")
    void 구직공고_목록조회() throws Exception {
        given(jobPostService.getList()).willReturn(List.of());

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/job-post/{id} - 구직 공고 상세 조회")
    void 구직공고_상세조회() throws Exception {
        JobPostResponse res = new JobPostResponse(1L, "네이버", null, null, null, null, null, null, null, null);
        given(jobPostService.getOne(1L)).willReturn(res);

        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(1L));
    }

    @Test
    @DisplayName("PATCH /api/v1/job-post/{id} - 구직 공고 수정")
    void 구직공고_수정() throws Exception {
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
    void 구직공고_삭제() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(SuccessMessage.JOB_POST_DELETED));
        verify(jobPostService).delete(any(), eq(1L));
    }
}
