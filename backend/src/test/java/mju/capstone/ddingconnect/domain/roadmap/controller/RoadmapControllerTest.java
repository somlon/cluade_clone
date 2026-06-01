package mju.capstone.ddingconnect.domain.roadmap.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import mju.capstone.ddingconnect.domain.interested_job.domain.TargetJobCategory;
import mju.capstone.ddingconnect.domain.roadmap.dto.request.CreateRoadmapRequest;
import mju.capstone.ddingconnect.domain.roadmap.dto.response.RoadmapDownloadResponse;
import mju.capstone.ddingconnect.domain.roadmap.dto.response.RoadmapListResponse;
import mju.capstone.ddingconnect.domain.roadmap.dto.response.RoadmapResponse;
import mju.capstone.ddingconnect.domain.roadmap.service.RoadmapService;
import mju.capstone.ddingconnect.domain.techstack.domain.TechStackName;
import mju.capstone.ddingconnect.global.auth.annotation.LoginMemberArgumentResolver;
import mju.capstone.ddingconnect.global.common.SuccessMessage;
import mju.capstone.ddingconnect.global.config.WebMvcConfig;
import mju.capstone.ddingconnect.global.response.code.status.ErrorStatus;
import mju.capstone.ddingconnect.global.response.exception.ExceptionAdvice;
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
@Import({WebMvcConfig.class, LoginMemberArgumentResolver.class, ExceptionAdvice.class})
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
    @DisplayName("POST /api/v1/roadmaps - 입력 폼으로 로드맵 생성 후 카드 목록(최신순, 방금 만든 항목 최상단)을 반환")
    void createRoadmap() throws Exception {
        CreateRoadmapRequest req = new CreateRoadmapRequest(3, 4.0, "응용소프트웨어학과",
                TargetJobCategory.BACKEND, List.of(TechStackName.JAVA, TechStackName.SPRING), "카카오");
        given(roadmapService.create(eq(WithMockLoginMember.STUDENT_ID), any()))
                .willReturn(List.of(
                        new RoadmapListResponse(3L, "백엔드 개발자 로드맵", CREATED_AT.plusSeconds(1)),
                        new RoadmapListResponse(2L, "이전 로드맵", CREATED_AT)));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isArray())
                .andExpect(jsonPath("$.result[0].id").value(3L))
                .andExpect(jsonPath("$.result[0].title").value("백엔드 개발자 로드맵"))
                .andExpect(jsonPath("$.result[0].createdAt").exists())
                .andExpect(jsonPath("$.result[0].content").doesNotExist())
                .andExpect(jsonPath("$.result[1].id").value(2L));

        verify(roadmapService).create(eq(WithMockLoginMember.STUDENT_ID), any(CreateRoadmapRequest.class));
    }

    @Test
    @DisplayName("GET /api/v1/roadmaps - 본인 로드맵 카드 목록 (id/title/createdAt 만, content 미포함)")
    void getRoadmapList() throws Exception {
        given(roadmapService.getList(any()))
                .willReturn(List.of(new RoadmapListResponse(2L, "백엔드 개발자 로드맵", CREATED_AT)));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isArray())
                .andExpect(jsonPath("$.result[0].id").value(2L))
                .andExpect(jsonPath("$.result[0].title").value("백엔드 개발자 로드맵"))
                .andExpect(jsonPath("$.result[0].createdAt").exists())
                .andExpect(jsonPath("$.result[0].content").doesNotExist())
                .andExpect(jsonPath("$.result[0].memberId").doesNotExist());

        verify(roadmapService).getList(any());
    }

    @Test
    @DisplayName("GET /api/v1/roadmaps/{id} - 로드맵 상세 (본인 소유)")
    void getRoadmapDetail() throws Exception {
        given(roadmapService.getOne(any(), eq(1L)))
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

    @Test
    @DisplayName("GET /api/v1/roadmaps - GRADUATE 회원은 MEMBER_FIELD_ROLE_MISMATCH(400)로 거부되고 서비스는 호출되지 않는다")
    void getRoadmapListRejectsGraduate() throws Exception {
        WithMockLoginMember.loginAsGraduate();

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorStatus.MEMBER_FIELD_ROLE_MISMATCH.getCode()));

        verify(roadmapService, org.mockito.Mockito.never()).getList(any());
    }

    @Test
    @DisplayName("GET /api/v1/roadmaps/{id} - GRADUATE 도 본인 소유 단건 조회는 허용된다 (역할 가드는 목록만 적용)")
    void getRoadmapDetailAllowedForGraduate() throws Exception {
        WithMockLoginMember.loginAsGraduate();
        given(roadmapService.getOne(any(), eq(1L)))
                .willReturn(new RoadmapResponse(1L, MEMBER_ID, GENERATED_CONTENT, CREATED_AT));

        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(1L));
    }

    @Test
    @DisplayName("GET /api/v1/roadmaps/{id}/download - 다운로드 URL 발급 (fileUrl/fileName/expiresAt)")
    void downloadRoadmap() throws Exception {
        LocalDateTime expiresAt = CREATED_AT.plusMinutes(5);
        given(roadmapService.getDownloadUrl(any(), eq(1L)))
                .willReturn(new RoadmapDownloadResponse(
                        "https://signed.example/roadmaps/1.pdf?X-Amz-Expires=300",
                        "백엔드 로드맵.pdf",
                        expiresAt));

        mockMvc.perform(get(BASE_URL + "/1/download"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.fileUrl").value("https://signed.example/roadmaps/1.pdf?X-Amz-Expires=300"))
                .andExpect(jsonPath("$.result.fileName").value("백엔드 로드맵.pdf"))
                .andExpect(jsonPath("$.result.expiresAt").exists());

        verify(roadmapService).getDownloadUrl(any(), eq(1L));
    }
}
