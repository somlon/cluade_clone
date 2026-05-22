package mju.capstone.ddingconnect.domain.roadmap.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mju.capstone.ddingconnect.domain.interested_job.domain.TargetJobCategory;
import mju.capstone.ddingconnect.domain.roadmap.dto.request.CreateRoadmapRequest;
import mju.capstone.ddingconnect.domain.roadmap.dto.response.RoadmapResponse;
import mju.capstone.ddingconnect.domain.roadmap.service.RoadmapService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoadmapController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({WebMvcConfig.class, LoginMemberArgumentResolver.class})
@DisplayName("RoadmapController 슬라이스 테스트")
class RoadmapControllerTest {

    private static final String BASE_URL = "/api/v1/roadmaps";
    private static final Long MEMBER_ID = 1L;
    private static final String GENERATED_CONTENT = "{\"roadmap_title\":\"백엔드 로드맵\"}";
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 5, 23, 10, 30, 45);

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean RoadmapService roadmapService;

    @BeforeEach
    void setUp() { WithMockLoginMember.loginAsStudent(); }

    @AfterEach
    void tearDown() { WithMockLoginMember.clear(); }

    @Test
    @DisplayName("POST /api/v1/roadmaps?memberId= - 입력 폼으로 로드맵 등록")
    void createRoadmap() throws Exception {
        CreateRoadmapRequest req = new CreateRoadmapRequest(3, 4.0, "응용소프트웨어학과",
                TargetJobCategory.BACKEND, List.of(TechStackName.JAVA, TechStackName.SPRING), "카카오");
        given(roadmapService.create(eq(MEMBER_ID), any()))
                .willReturn(new RoadmapResponse(1L, MEMBER_ID, GENERATED_CONTENT, CREATED_AT));

        mockMvc.perform(post(BASE_URL)
                        .param("memberId", String.valueOf(MEMBER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(1L))
                .andExpect(jsonPath("$.result.content").value(GENERATED_CONTENT));

        verify(roadmapService).create(eq(MEMBER_ID), any(CreateRoadmapRequest.class));
    }

    @Test
    @DisplayName("GET /api/v1/roadmaps - 본인이 생성한 로드맵 목록 (createdAt 포함)")
    void getRoadmapList() throws Exception {
        given(roadmapService.getList(any()))
                .willReturn(List.of(new RoadmapResponse(2L, MEMBER_ID, GENERATED_CONTENT, CREATED_AT)));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isArray())
                .andExpect(jsonPath("$.result[0].id").value(2L))
                .andExpect(jsonPath("$.result[0].memberId").value(MEMBER_ID))
                .andExpect(jsonPath("$.result[0].createdAt").exists());

        verify(roadmapService).getList(any());
    }

    @Test
    @DisplayName("GET /api/v1/roadmaps/{id} - 로드맵 상세")
    void getRoadmapDetail() throws Exception {
        given(roadmapService.getOne(1L))
                .willReturn(new RoadmapResponse(1L, MEMBER_ID, GENERATED_CONTENT, CREATED_AT));

        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(1L));
    }

    @Test
    @DisplayName("DELETE /api/v1/roadmaps/{id} - 로드맵 삭제")
    void deleteRoadmap() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(SuccessMessage.ROADMAP_DELETED));
        verify(roadmapService).delete(any(), eq(1L));
    }
}
