package mju.capstone.ddingconnect.domain.interested_job.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mju.capstone.ddingconnect.domain.interested_job.domain.TargetJobCategory;
import mju.capstone.ddingconnect.domain.interested_job.dto.request.CreateTargetJobRequest;
import mju.capstone.ddingconnect.domain.interested_job.dto.request.UpdateTargetJobRequest;
import mju.capstone.ddingconnect.domain.interested_job.dto.response.TargetJobResponse;
import mju.capstone.ddingconnect.domain.interested_job.service.TargetJobService;
import mju.capstone.ddingconnect.global.auth.annotation.LoginMemberArgumentResolver;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TargetJobController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({WebMvcConfig.class, LoginMemberArgumentResolver.class})
@DisplayName("TargetJobController 슬라이스 테스트")
class TargetJobControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean TargetJobService targetJobService;

    @BeforeEach
    void setUp() { WithMockLoginMember.loginAsStudent(); }

    @AfterEach
    void tearDown() { WithMockLoginMember.clear(); }

    @Test
    @DisplayName("POST /api/v1/target-jobs - 관심 직군 추가")
    void 관심직군_추가() throws Exception {
        CreateTargetJobRequest req = new CreateTargetJobRequest(TargetJobCategory.BACKEND);
        given(targetJobService.create(any(), any()))
                .willReturn(new TargetJobResponse(1L, TargetJobCategory.BACKEND, "k"));

        mockMvc.perform(post("/api/v1/target-jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(1L));
    }

    @Test
    @DisplayName("GET /api/v1/target-jobs - 내 관심 직군 목록")
    void 관심직군_목록() throws Exception {
        given(targetJobService.getMyTargetJobs(any())).willReturn(List.of());

        mockMvc.perform(get("/api/v1/target-jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isArray());
    }

    @Test
    @DisplayName("PATCH /api/v1/target-jobs/{id} - 관심 직군 수정")
    void 관심직군_수정() throws Exception {
        UpdateTargetJobRequest req = new UpdateTargetJobRequest(TargetJobCategory.FRONTEND);
        given(targetJobService.update(any(), eq(1L), any()))
                .willReturn(new TargetJobResponse(1L, TargetJobCategory.FRONTEND, "k"));

        mockMvc.perform(patch("/api/v1/target-jobs/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.interestedJob").value("FRONTEND"));
    }

    @Test
    @DisplayName("DELETE /api/v1/target-jobs/{id} - 관심 직군 삭제")
    void 관심직군_삭제() throws Exception {
        mockMvc.perform(delete("/api/v1/target-jobs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("관심 직군이 삭제되었습니다."));
        verify(targetJobService).delete(any(), eq(1L));
    }
}
